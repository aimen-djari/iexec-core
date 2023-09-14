/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
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

import com.iexec.common.utils.BytesUtils;
import com.iexec.core.configuration.ConfigurationService;
import com.iexec.core.task.Task;
import com.iexec.core.task.TaskService;
import com.iexec.core.task.event.TaskExtendedEvent;

import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class TaskExtendedWatcherService {

    private final IexecHubService iexecHubService;
    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskService taskService;
    private final Web3jService web3jService;
    // internal variables
    private Disposable taskExtendedEventSubscriptionReplay;

    @Autowired
    public TaskExtendedWatcherService(IexecHubService iexecHubService,
                              ConfigurationService configurationService,
                              ApplicationEventPublisher applicationEventPublisher,
                              TaskService taskService,
                              Web3jService web3jService) {
        this.iexecHubService = iexecHubService;
        this.configurationService = configurationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.taskService = taskService;
        this.web3jService = web3jService;
    }

    @Async
    public void run() {
    	log.info("Extension RUN [replayFromBlock:{}, lastSeenBlockWithTaskExtendedEvent:{}]", configurationService.getLastSeenBlockWithExtendedEvent());
    	
    	subscribeToTaskExtendedEventFromOneBlockToLatest(configurationService.getLastSeenBlockWithExtendedEvent());
    }

    /**
     * Subscribe to onchain task extended events from
     * a given block to the latest block.
     * 
     * @param from start block
     * @return disposable subscription
     */
    Disposable subscribeToTaskExtendedEventFromOneBlockToLatest(BigInteger from) {
        log.info("Watcher TaskExtended Event started [from:{}, to:{}]", from, "latest");
        return iexecHubService.getTaskExtendedEventObservableToLatest(from)
                .subscribe(taskEvent -> taskEvent.ifPresent(this::onTaskExtendedEvent));
    }

    /**
     * Update last seen block in the database
     * and run {@link TaskExtendedEvent} handler.
     * 
     * @param taskExtendedEvent
     */
    private void onTaskExtendedEvent(TaskExtendedEvent taskExtendedEvent) {
        String taskId = taskExtendedEvent.getChainTaskId();
        BigInteger taskEventBlock = taskExtendedEvent.getBlockNumber();
        log.info("Received task extended event [taskId:{}, block:{}]", taskId,
        		taskEventBlock);
        if (taskEventBlock == null || taskEventBlock.equals(BigInteger.ZERO)){
            log.warn("Task Extended Event block number is empty, fetching later blockchain " +
                    "events will be more expensive [chainTaskId:{}, taskEventBlock:{}, " +
                    "lastBlock:{}]", taskId, taskEventBlock, web3jService.getLatestBlockNumber());
            taskExtendedEvent.setBlockNumber(BigInteger.ZERO);
        }
        this.handleTaskExtendedEvent(taskExtendedEvent);
        if (configurationService.getLastSeenBlockWithExtendedEvent().compareTo(taskEventBlock) < 0) {
            configurationService.setLastSeenBlockWithExtendedEvent(taskEventBlock);
        }
    }
    
    private void handleTaskExtendedEvent(TaskExtendedEvent taskExtendedEvent) {
    	String taskId = taskExtendedEvent.getChainTaskId();
    	
    	log.info("TaskExtended Event handle() [taskId:{}]", taskId);
    	Optional<Task> optional = taskService.getTaskByChainTaskId(taskId);
        if (optional.isEmpty()) {
            return;
        }
        Task task = optional.get();
        
        long newDuration = taskExtendedEvent.getDuration().longValue();
        long oldDuration = task.getMaxExecutionTime();
        long finalDeadline = task.getFinalDeadline().getTime();
        
        log.info("Task Duration extension requestt [taskId:{}, new duration:{}, old duration:{}, old deadline:{}]", taskId, newDuration, task.getMaxExecutionTime(), task.getFinalDeadline().getTime());

        Date newDeadline = new Date(finalDeadline - oldDuration * 1000 + newDuration * 1000);
        
        taskService.updateTask(taskId, newDuration, newDeadline, newDeadline, false);
        
        log.info("Task Duration extended [taskId:{}, new duration:{}, old duration:{}, new deadline:{}, old deadline:{}]", taskId, task.getMaxExecutionTime(), oldDuration, task.getFinalDeadline().getTime(), finalDeadline);
    	
    }

    /*
     * Some task extended events are sometimes missed by #schedulerNoticeEventObservable method
     * so we decide to replay events from times to times (already saved events will be ignored)
     */
    @Scheduled(fixedRateString = "#{@cronConfiguration.getExtendedEventReplay()}")
    void replayTaskExtendedEvent() {
        BigInteger lastSeenBlockWithTaskExtendedEvent = configurationService.getLastSeenBlockWithExtendedEvent();
        BigInteger replayFromBlock = configurationService.getFromReplay();
    	
        if (replayFromBlock.compareTo(lastSeenBlockWithTaskExtendedEvent) >= 0) {
            return;
        }
        if (this.taskExtendedEventSubscriptionReplay != null && !this.taskExtendedEventSubscriptionReplay.isDisposed()) {
            this.taskExtendedEventSubscriptionReplay.dispose();
        }
        this.taskExtendedEventSubscriptionReplay = subscribeToTaskExtendedEventInRange(replayFromBlock, lastSeenBlockWithTaskExtendedEvent);
        configurationService.setFromReplay(lastSeenBlockWithTaskExtendedEvent);
    }

    /**
     * Subscribe to onchain task extended events for
     * a fixed range of blocks.
     * 
     * @param from start block
     * @param to end block
     * @return disposable subscription
     */
    private Disposable subscribeToTaskExtendedEventInRange(BigInteger from, BigInteger to) {
        log.info("Replay Watcher TaskExtendedEvent started [from:{}, to:{}]",
                from, (to == null) ? "latest" : to);
        return iexecHubService.getTaskExtendedEventObservable(from, to)
                .subscribe(taskEvent -> taskEvent.ifPresent(this::onTaskExtendedEvent));
    }
}
