package com.iexec.core.chain;

import com.iexec.common.chain.ChainDeal;
import com.iexec.common.utils.BytesUtils;
import com.iexec.core.configuration.ConfigurationService;
import com.iexec.core.task.Task;
import com.iexec.core.task.TaskService;
import com.iexec.core.task.event.TaskCreatedEvent;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Optional;

@Slf4j
@Service
public class DealWatcherService {

    private final IexecHubService iexecHubService;
    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskService taskService;
    // internal variables
    private Disposable dealEventSubscriptionReplay;

    @Autowired
    public DealWatcherService(IexecHubService iexecHubService,
                              ConfigurationService configurationService,
                              ApplicationEventPublisher applicationEventPublisher,
                              TaskService taskService) {
        this.iexecHubService = iexecHubService;
        this.configurationService = configurationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskService = taskService;
    }

    public void run() {
        subscribeToDealEventFromOneBlockToLatest(configurationService.getLastSeenBlockWithDeal());
    }

    Disposable subscribeToDealEventFromOneBlockToLatest(BigInteger from) {
        log.info("Watcher DealEvent started [from:{}, to:{}]", from, "latest");
        return iexecHubService.getDealEventObservableToLatest(from)
                .subscribe(dealEvent -> dealEvent.ifPresent(this::onDealEvent));
    }

    private void onDealEvent(DealEvent dealEvent) {
        log.info("Received deal [dealId:{}, block:{}]", dealEvent.getChainDealId(), dealEvent.getBlockNumber());
        this.handleDeal(dealEvent.getChainDealId());
        if (configurationService.getLastSeenBlockWithDeal().intValue() < dealEvent.getBlockNumber().intValue()) {
            configurationService.setLastSeenBlockWithDeal(dealEvent.getBlockNumber());
        }
    }

    private void handleDeal(String chainDealId) {
        Optional<ChainDeal> optionalChainDeal = iexecHubService.getChainDeal(chainDealId);
        if (!optionalChainDeal.isPresent()) {
            return;
        }
        ChainDeal chainDeal = optionalChainDeal.get();


        try {
            int startBag = chainDeal.getBotFirst().intValue();
            int endBag = chainDeal.getBotFirst().intValue() + chainDeal.getBotSize().intValue();

            for (int taskIndex = startBag; taskIndex < endBag; taskIndex++) {
                Optional<Task> optional = taskService.addTask(chainDealId, taskIndex,
                        BytesUtils.hexStringToAscii(chainDeal.getChainApp().getUri()),
                        chainDeal.getParams().getIexecArgs(),
                        chainDeal.getTrust().intValue(),
                        chainDeal.getChainCategory().getMaxExecutionTime(),
                        chainDeal.getTag());
                optional.ifPresent(task -> applicationEventPublisher.publishEvent(new TaskCreatedEvent(task.getChainTaskId())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Some deal events are sometimes missed by #schedulerNoticeEventObservable method
     * so we decide to replay events from times to times (already saved events will be ignored)
     * */
    @Scheduled(fixedRateString = "${cron.detector.dealwatcherreplay.period}")
    void replayDealEvent() {
        if (configurationService.getFromReplay().intValue() < configurationService.getLastSeenBlockWithDeal().intValue()) {
            if (dealEventSubscriptionReplay != null && !dealEventSubscriptionReplay.isDisposed()) {
                dealEventSubscriptionReplay.dispose();
            }
            dealEventSubscriptionReplay = subscribeToDealEventInRange(configurationService.getFromReplay(), configurationService.getLastSeenBlockWithDeal());
            configurationService.setFromReplay(configurationService.getLastSeenBlockWithDeal());
        }
    }

    private Disposable subscribeToDealEventInRange(BigInteger from, BigInteger to) {
        log.info("Replay Watcher DealEvent started [from:{}, to:{}]", from, (to == null) ? "latest" : to);
        return iexecHubService.getDealEventObservable(from, to)
                .subscribe(dealEvent -> dealEvent.ifPresent(this::onDealEvent));
    }

}
