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
import com.iexec.core.task.TaskStatus;
import com.iexec.core.task.event.TaskCreatedEvent;
import com.iexec.core.task.event.TaskInterruptEvent;

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
public class TaskInterruptWatcherService {

    private final IexecHubService iexecHubService;
    private final ConfigurationService configurationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TaskService taskService;
    private final Web3jService web3jService;
    // internal variables
    private Disposable taskInterruptEventSubscriptionReplay;

    @Autowired
    public TaskInterruptWatcherService(IexecHubService iexecHubService,
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
    	subscribeToTaskInterruptEventFromOneBlockToLatest(configurationService.getLastSeenBlockWithInterruptEvent());
    }

    /**
     * Subscribe to onchain task interrupt events from
     * a given block to the latest block.
     * 
     * @param from start block
     * @return disposable subscription
     */
    Disposable subscribeToTaskInterruptEventFromOneBlockToLatest(BigInteger from) {
        log.info("Watcher TaskInterrupt Event started [from:{}, to:{}]", from, "latest");
        return iexecHubService.getTaskInterruptEventObservableToLatest(from)
                .subscribe(taskEvent -> taskEvent.ifPresent(this::onTaskInterruptEvent));
    }

    /**
     * Update last seen block in the database
     * and run {@link TaskInterruptEvent} handler.
     * 
     * @param taskInterruptEvent
     */
    private void onTaskInterruptEvent(TaskInterruptEvent taskInterruptEvent) {
        String taskId = taskInterruptEvent.getChainTaskId();
        BigInteger taskEventBlock = taskInterruptEvent.getBlockNumber();
        log.info("Received task interrupt event [taskId:{}, block:{}]", taskId,
        		taskEventBlock);
        if (taskEventBlock == null || taskEventBlock.equals(BigInteger.ZERO)){
            log.warn("Task Interrupt Event block number is empty, fetching later blockchain " +
                    "events will be more expensive [chainTaskId:{}, taskEventBlock:{}, " +
                    "lastBlock:{}]", taskId, taskEventBlock, web3jService.getLatestBlockNumber());
            taskInterruptEvent.setBlockNumber(BigInteger.ZERO);
        }
        this.handleTaskInterruptEvent(taskInterruptEvent);
        if (configurationService.getLastSeenBlockWithInterruptEvent().compareTo(taskEventBlock) < 0) {
            configurationService.setLastSeenBlockWithInterruptEvent(taskEventBlock);
        }
    }
    
    private void handleTaskInterruptEvent(TaskInterruptEvent taskInterruptEvent) {
    	String taskId = taskInterruptEvent.getChainTaskId();
    	log.info("Handling an interrupted task:{}", taskId);
        Optional<Task> optional = taskService.getTaskByChainTaskId(taskId);
        if (optional.isEmpty()) {
            return;
        }
        Task task = optional.get();
        log.info("Handling an interrupted task:{} with status:{}", taskId, task.getCurrentStatus());
        
        if (!TaskStatus.getStatusesWhereInterruptionIsImpossible().contains(task.getCurrentStatus())) {
        	long newDuration = taskInterruptEvent.getDuration().longValue();
        	long oldDuration = task.getMaxExecutionTime();
        	long finalDeadline = task.getFinalDeadline().getTime();
            
            Date newDeadline = new Date(finalDeadline - oldDuration * 1000 + newDuration * 1000);
            
            taskService.updateTask(taskId, newDuration, newDeadline, newDeadline, true);
            
            
            log.info("Detected TaskInteruptEvent of task:{}, isInterrupted:{}]", taskId, task.isInterrupted());
            applicationEventPublisher.publishEvent(taskInterruptEvent);
        }
	}

    /*
     * Some task interrupt events are sometimes missed by #schedulerNoticeEventObservable method
     * so we decide to replay events from times to times (already saved events will be ignored)
     */
    @Scheduled(fixedRateString = "#{@cronConfiguration.getInterruptEventReplay()}")
    void replayTaskInterruptEvent() {
        BigInteger lastSeenBlockWithTaskInterruptEvent = configurationService.getLastSeenBlockWithInterruptEvent();
        BigInteger replayFromBlock = configurationService.getFromReplay();
    
        if (replayFromBlock.compareTo(lastSeenBlockWithTaskInterruptEvent) >= 0) {
            return;
        }
        if (this.taskInterruptEventSubscriptionReplay != null && !this.taskInterruptEventSubscriptionReplay.isDisposed()) {
            this.taskInterruptEventSubscriptionReplay.dispose();
        }
        this.taskInterruptEventSubscriptionReplay = subscribeToTaskInterruptEventInRange(replayFromBlock, lastSeenBlockWithTaskInterruptEvent);
        configurationService.setFromReplay(lastSeenBlockWithTaskInterruptEvent);
    }

    /**
     * Subscribe to onchain task interrupt events for
     * a fixed range of blocks.
     * 
     * @param from start block
     * @param to end block
     * @return disposable subscription
     */
    private Disposable subscribeToTaskInterruptEventInRange(BigInteger from, BigInteger to) {
        log.info("Replay Watcher TaskInterruptEvent started [from:{}, to:{}]",
                from, (to == null) ? "latest" : to);
        return iexecHubService.getTaskInterruptEventObservable(from, to)
                .subscribe(taskEvent -> taskEvent.ifPresent(this::onTaskInterruptEvent));
    }
}
