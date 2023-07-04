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

package com.iexec.core.detector.task;

import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.iexec.common.chain.ChainTask;
import com.iexec.core.chain.IexecHubService;
import com.iexec.core.detector.Detector;
import com.iexec.core.task.Task;
import com.iexec.core.task.TaskService;
import com.iexec.core.task.TaskStatus;
import com.iexec.core.task.TaskUpdateManager;
import com.iexec.core.task.event.TaskExtendedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExtendedTaskDetector implements Detector {

	private final TaskService taskService;
	private final TaskUpdateManager taskUpdateManager;
	private final IexecHubService iexecHubService;
	private BigInteger lastSeenBlock = BigInteger.ZERO;

	public ExtendedTaskDetector(TaskService taskService, TaskUpdateManager taskUpdateManager,
			IexecHubService iexecHubService) {
		this.taskService = taskService;
		this.taskUpdateManager = taskUpdateManager;
		this.iexecHubService = iexecHubService;
	}

	/**
     * Detector to detect tasks that are reopening but are not reopened yet.
     */
    @Scheduled(fixedRateString = "#{@cronConfiguration.getExtend()}")
    @Override
    public void detect() {
        log.debug("Trying to detect extended tasks");
        for (Task task : taskService.findByCurrentStatus(TaskStatus.RUNNING)) {
            Optional<ChainTask> oChainTask = iexecHubService.getChainTask(task.getChainTaskId());
            if (!oChainTask.isPresent()) {
                continue;
            }
            
            iexecHubService.getTaskExtendedEventObservable(lastSeenBlock, null, task.getChainTaskId()).subscribe(extendTaskEvent -> extendTaskEvent.ifPresent(this::onExtendTaskEvent));
        }
    }

	private void onExtendTaskEvent(TaskExtendedEvent extendTaskEvent) {
		String taskId = extendTaskEvent.getChainTaskId();
		BigInteger taskBlock = extendTaskEvent.getBlockNumber();
		log.info("Received task [taskId:{}, block:{}]", taskId, taskBlock);
		if (taskBlock == null || taskBlock.equals(BigInteger.ZERO)) {
			log.warn(
					"Task block number is empty, fetching later blockchain "
							+ "events will be more expensive [chainTaskId:{}, taskBlock:{}]",
					taskId, taskBlock);
			extendTaskEvent.setBlockNumber(BigInteger.ZERO);
		}
		this.handleExtendedTaskEvent(extendTaskEvent);
		if (lastSeenBlock.compareTo(taskBlock) < 0) {
			lastSeenBlock = taskBlock;
		}
	}

	private void handleExtendedTaskEvent(TaskExtendedEvent extendTaskEvent) {
         Optional<Task> optional = taskService.getTaskByChainTaskId(extendTaskEvent.getChainTaskId());
         if (optional.isEmpty()) {
             return;
         }
         Task task = optional.get();
         
         long duration = extendTaskEvent.getDuration().longValue();
         long oldDuration = task.getMaxExecutionTime();
         long contributionDeadline = task.getContributionDeadline().getTime()/1000;
         
         Date newDeadline = new Date(contributionDeadline - oldDuration * 10 + duration * 10);
         task.setFinalDeadline(newDeadline);
         task.setContributionDeadline(newDeadline);
         task.setMaxExecutionTime(duration);
	}
}
