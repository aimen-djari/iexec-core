/*
 * Copyright 2020-2023 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.core.chain;

import com.iexec.commons.poco.chain.ChainDeal;
import com.iexec.commons.poco.contract.generated.IexecHubContract;
import com.iexec.commons.poco.utils.BytesUtils;
import com.iexec.core.configuration.ConfigurationService;
import com.iexec.core.task.Task;
import com.iexec.core.task.TaskService;
import com.iexec.core.task.event.TaskCreatedEvent;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Optional;

import static com.iexec.commons.poco.contract.generated.IexecHubContract.SCHEDULERNOTICE_EVENT;

@Slf4j
@Service
public class DealWatcherService {

    private final ChainConfig chainConfig;
    private final IexecHubService iexecHubService;
    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskService taskService;
    private final Web3jService web3jService;
    // internal variables
    private Disposable dealEventSubscriptionReplay;
    @Getter
    private BigInteger latestBlockNumberWithDeal = BigInteger.ZERO;
    @Getter
    private long dealEventsCount = 0;
    @Getter
    private long dealsCount = 0;
    @Getter
    private long replayDealsCount = 0;

    public DealWatcherService(ChainConfig chainConfig,
                              IexecHubService iexecHubService,
                              ConfigurationService configurationService,
                              ApplicationEventPublisher applicationEventPublisher,
                              TaskService taskService,
                              Web3jService web3jService) {
        this.chainConfig = chainConfig;
        this.iexecHubService = iexecHubService;
        this.configurationService = configurationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskService = taskService;
        this.web3jService = web3jService;
    }

    /**
     * This should be non-blocking to liberate
     * the main thread, since deals can have
     * a large number of tasks (BoT).
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        subscribeToDealEventFromOneBlockToLatest(configurationService.getLastSeenBlockWithDeal());
    }

    /**
     * Subscribe to onchain deal events from
     * a given block to the latest block.
     * 
     * @param from start block
     * @return disposable subscription
     */
    Disposable subscribeToDealEventFromOneBlockToLatest(BigInteger from) {
        log.info("Watcher DealEvent started [from:{}, to:{}]", from, "latest");
        EthFilter filter = createDealEventFilter(from, null);
        return iexecHubService.getDealEventObservable(filter)
                .map(this::schedulerNoticeToDealEvent)
                .subscribe(dealEvent -> dealEvent.ifPresent(event -> onDealEvent(event, "start")));
    }

    /**
     * Update last seen block in the database
     * and run {@link DealEvent} handler.
     * 
     * @param dealEvent
     */
    private void onDealEvent(DealEvent dealEvent, String watcher) {
        if ("replay".equals(watcher)) {
            replayDealsCount++;
        } else {
            dealsCount++;
        }
        String dealId = dealEvent.getChainDealId();
        BigInteger dealBlock = dealEvent.getBlockNumber();
        log.info("Received deal [dealId:{}, block:{}, watcher: {}]",
                dealId, dealBlock, watcher);
        if (dealBlock.equals(BigInteger.ZERO)) {
            log.warn("Deal block number is empty, fetching later blockchain " +
                    "events will be more expensive [chainDealId:{}, dealBlock:{}, " +
                    "lastBlock:{}]", dealId, dealBlock, web3jService.getLatestBlockNumber());
        }
        this.handleDeal(dealEvent);
        if (configurationService.getLastSeenBlockWithDeal().compareTo(dealBlock) < 0) {
            configurationService.setLastSeenBlockWithDeal(dealBlock);
        }
    }

    /**
     * Handle new onchain deals and add its tasks
     * to db.
     *
     * @param dealEvent
     */
    private void handleDeal(DealEvent dealEvent) {
        String chainDealId = dealEvent.getChainDealId();
        Optional<ChainDeal> oChainDeal = iexecHubService.getChainDeal(chainDealId);
        if (oChainDeal.isEmpty()) {
            log.error("Could not get chain deal [chainDealId:{}]", chainDealId);
            return;
        }
        ChainDeal chainDeal = oChainDeal.get();
        // do not process deals after deadline
        if (!iexecHubService.isBeforeContributionDeadline(chainDeal)) {
            log.error("Deal has expired [chainDealId:{}, deadline:{}]",
                    chainDealId, iexecHubService.getChainDealContributionDeadline(chainDeal));
            return;
        }
        int startBag = chainDeal.getBotFirst().intValue();
        int endBag = chainDeal.getBotFirst().intValue() + chainDeal.getBotSize().intValue();
        for (int taskIndex = startBag; taskIndex < endBag; taskIndex++) {
            Optional<Task> optional = taskService.addTask(
                    chainDealId,
                    taskIndex,
                    dealEvent.getBlockNumber().longValue(),
                    BytesUtils.hexStringToAscii(chainDeal.getChainApp().getUri()),
                    chainDeal.getParams().getIexecArgs(),
                    chainDeal.getTrust().intValue(),
                    chainDeal.getChainCategory().getMaxExecutionTime(),
                    chainDeal.getTag(),
                    iexecHubService.getChainDealContributionDeadline(chainDeal),
                    iexecHubService.getChainDealFinalDeadline(chainDeal));
            optional.ifPresent(task -> applicationEventPublisher
                    .publishEvent(new TaskCreatedEvent(task.getChainTaskId())));
        }
    }

    /*
     * Some deal events are sometimes missed by #schedulerNoticeEventObservable method
     * so we decide to replay events from times to times (already saved events will be ignored)
     */
    @Scheduled(fixedRateString = "#{@cronConfiguration.getDealReplay()}")
    void replayDealEvent() {
        BigInteger lastSeenBlockWithDeal = configurationService.getLastSeenBlockWithDeal();
        BigInteger replayFromBlock = configurationService.getFromReplay();
        if (replayFromBlock.compareTo(lastSeenBlockWithDeal) >= 0) {
            return;
        }
        if (this.dealEventSubscriptionReplay != null && !this.dealEventSubscriptionReplay.isDisposed()) {
            this.dealEventSubscriptionReplay.dispose();
        }
        this.dealEventSubscriptionReplay = subscribeToDealEventInRange(replayFromBlock, lastSeenBlockWithDeal);
        configurationService.setFromReplay(lastSeenBlockWithDeal);
    }

    /**
     * Subscribe to onchain deal events for
     * a fixed range of blocks.
     * 
     * @param from start block
     * @param to end block
     * @return disposable subscription
     */
    private Disposable subscribeToDealEventInRange(BigInteger from, BigInteger to) {
        log.info("Replay Watcher DealEvent started [from:{}, to:{}]",
                from, (to == null) ? "latest" : to);
        EthFilter filter = createDealEventFilter(from, to);
        return iexecHubService.getDealEventObservable(filter)
                .map(this::schedulerNoticeToDealEvent)
                .subscribe(dealEvent -> dealEvent.ifPresent(event -> onDealEvent(event, "replay")));
    }

    EthFilter createDealEventFilter(BigInteger from, BigInteger to) {
        final DefaultBlockParameter fromBlock = DefaultBlockParameter.valueOf(from);
        final DefaultBlockParameter toBlock = to == null
                ? DefaultBlockParameterName.LATEST
                : DefaultBlockParameter.valueOf(to);
        final EthFilter filter = new EthFilter(fromBlock, toBlock, chainConfig.getHubAddress());
        final BigInteger poolAddressBigInt = Numeric.toBigInt(chainConfig.getPoolAddress());
        filter.addSingleTopic(EventEncoder.encode(SCHEDULERNOTICE_EVENT));
        filter.addSingleTopic(Numeric.toHexStringWithPrefixZeroPadded(poolAddressBigInt, 64));
        return filter;
    }

    Optional<DealEvent> schedulerNoticeToDealEvent(IexecHubContract.SchedulerNoticeEventResponse schedulerNotice) {
        dealEventsCount++;
        BigInteger noticeBlockNumber = schedulerNotice.log.getBlockNumber();
        if (latestBlockNumberWithDeal.compareTo(noticeBlockNumber) < 0) {
            latestBlockNumberWithDeal = noticeBlockNumber;
        }
        log.info("Received new deal [blockNumber:{}, chainDealId:{}, dealEventsCount:{}]",
                schedulerNotice.log.getBlockNumber(), BytesUtils.bytesToString(schedulerNotice.dealid), dealEventsCount);
        if (schedulerNotice.workerpool.equalsIgnoreCase(chainConfig.getPoolAddress())) {
            return Optional.of(new DealEvent(schedulerNotice));
        }
        log.warn("This deal event should not have been received [blockNumber:{}, chainDealId:{}, dealEventsCount:{}]",
                schedulerNotice.log.getBlockNumber(), BytesUtils.bytesToString(schedulerNotice.dealid), dealEventsCount);
        return Optional.empty();
    }
}
