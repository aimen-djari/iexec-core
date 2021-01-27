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

package com.iexec.core.task.executor;

import java.util.concurrent.*;

import com.iexec.core.task.TaskUpdateRequestConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This class is used to perform updates on a task one by one.
 * It also ensures that no extra update is performed for no reason
 * (in the case of multiple replicate updates in a short time,
 * the task update will only be called once)
 */
@Slf4j
@Component
public class TaskUpdateRequestManager {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private Future<Void> consumerSubscription;

    /**
     * Publish TaskUpdateRequest async
     * @param chainTaskId
     */
    public void publishRequest(String chainTaskId) {
        if (chainTaskId.isEmpty()){
            return;
        }
        if (queue.contains(chainTaskId)){
            log.warn("Request already published [chainTaskId:{}]", chainTaskId);
            return;
        }
        Runnable publishRequest = () -> queue.offer(chainTaskId);
        executorService.submit(publishRequest);
        log.info("Published request [chainTaskId:{}, queueSize:{}]", chainTaskId, queue.size());
    }

    /**
     * Authorize one TaskUpdateRequest consumer subscription at a time.
     * @param consumer
     */
    public void setRequestConsumer(final TaskUpdateRequestConsumer consumer) {
        if (consumerSubscription != null){
            consumerSubscription.cancel(true);
            log.info("Canceled previous consumer subscription");
        }
        Callable<Void> consumeTask = () -> {
            while (true){
                log.info("Waiting requests from publisher");
                try {
                    String chainTaskId = queue.take();
                    consumer.onTaskUpdateRequest(chainTaskId);
                } catch (InterruptedException e) {
                    log.error("The unexpected happened", e);
                }
            }
        };
        consumerSubscription = executorService.submit(consumeTask);
        log.info("Setup request consumer subscription");
    }

}
