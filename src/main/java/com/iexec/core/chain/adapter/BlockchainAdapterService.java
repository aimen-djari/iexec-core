/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.core.chain.adapter;

import com.iexec.blockchain.api.BlockchainAdapterApiClient;
import com.iexec.common.chain.adapter.CommandStatus;
import com.iexec.common.chain.adapter.args.TaskFinalizeArgs;
import com.iexec.common.config.PublicChainConfig;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
public class BlockchainAdapterService {

    public static final int WATCH_PERIOD_SECONDS = 1;//To tune
    public static final int MAX_ATTEMPTS = 50;

    private final BlockchainAdapterApiClient blockchainAdapterClient;
    private PublicChainConfig publicChainConfig;

    public BlockchainAdapterService(BlockchainAdapterApiClient blockchainAdapterClient) {
        this.blockchainAdapterClient = blockchainAdapterClient;
    }

    /**
     * Request on-chain initialization of the task.
     *
     * @param chainDealId ID of the deal
     * @param taskIndex   index of the task in the deal
     * @return chain task ID is initialization is properly requested
     */
    public Optional<String> requestInitialize(String chainDealId, int taskIndex) {
        try {
            String chainTaskId = blockchainAdapterClient.requestInitializeTask(chainDealId, taskIndex);
            if (!StringUtils.isEmpty(chainTaskId)) {
                log.info("Requested initialize [chainTaskId:{}, chainDealId:{}, " +
                        "taskIndex:{}]", chainTaskId, chainDealId, taskIndex);
                return Optional.of(chainTaskId);
            }
        } catch (Exception e) {
            log.error("Failed to requestInitialize [chainDealId:{}, " +
                    "taskIndex:{}]", chainDealId, taskIndex, e);
        }
        return Optional.empty();
    }

    /**
     * Verify if the initialize task command is completed on-chain.
     *
     * @param chainTaskId ID of the task
     * @return true if the tx is mined, false if reverted or empty for other
     * cases (too long since still RECEIVED or PROCESSING, adapter error)
     */
    public Optional<Boolean> isInitialized(String chainTaskId) {
        return isCommandCompleted(blockchainAdapterClient::getStatusForInitializeTaskRequest,
                chainTaskId, SECONDS.toMillis(WATCH_PERIOD_SECONDS), MAX_ATTEMPTS);
    }

    /**
     * Request on-chain finalization of the task.
     *
     * @param chainTaskId  ID of the deal
     * @param resultLink   link of the result to be published on-chain
     * @param callbackData optional data for on-chain callback
     * @return chain task ID is initialization is properly requested
     */
    public Optional<String> requestFinalize(String chainTaskId,
                                            String resultLink,
                                            String callbackData) {
        try {
            String finalizeResponse = blockchainAdapterClient.requestFinalizeTask(chainTaskId,
                    new TaskFinalizeArgs(resultLink, callbackData));
            if (!StringUtils.isEmpty(finalizeResponse)) {
                log.info("Requested finalize [chainTaskId:{}, resultLink:{}, callbackData:{}]",
                        chainTaskId, resultLink, callbackData);
                return Optional.of(chainTaskId);
            }
        } catch (Exception e) {
            log.error("Failed to requestFinalize [chainTaskId:{}, resultLink:{}, callbackData:{}]",
                    chainTaskId, resultLink, callbackData, e);
        }
        return Optional.empty();
    }

    /**
     * Verify if the finalize task command is completed on-chain.
     *
     * @param chainTaskId ID of the task
     * @return true if the tx is mined, false if reverted or empty for other
     * cases (too long since still RECEIVED or PROCESSING, adapter error)
     */
    public Optional<Boolean> isFinalized(String chainTaskId) {
        return isCommandCompleted(blockchainAdapterClient::getStatusForFinalizeTaskRequest,
                chainTaskId, SECONDS.toMillis(WATCH_PERIOD_SECONDS), MAX_ATTEMPTS);
    }

    /**
     * Verify if a command sent to the adapter is completed on-chain.
     *
     * @param getCommandStatusFunction method for checking the command is completed
     * @param chainTaskId              ID of the task
     * @param period                   period in ms between checks
     * @param maxAttempts              maximum number of attempts for checking
     * @return true if the tx is mined, false if reverted or empty for other
     * cases (too long since still RECEIVED or PROCESSING, adapter error)
     */
    Optional<Boolean> isCommandCompleted(
            Function<String, CommandStatus> getCommandStatusFunction,
            String chainTaskId,
            long period, int maxAttempts) {
        for(int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                CommandStatus status = getCommandStatusFunction.apply(chainTaskId);
                if (CommandStatus.SUCCESS.equals(status) || CommandStatus.FAILURE.equals(status)) {
                    return Optional.of(status.equals(CommandStatus.SUCCESS));
                }
                // RECEIVED, PROCESSING
                log.warn("Waiting command completion [chainTaskId:{}, status:{}, period:{}ms, attempt:{}, maxAttempts:{}]",
                        chainTaskId, status, period, attempt, maxAttempts);
                Thread.sleep(period);
            } catch (Exception e) {
                log.error("Unexpected error while waiting command completion [chainTaskId:{}, period:{}ms, attempt:{}, maxAttempts:{}]",
                        chainTaskId, period, attempt, maxAttempts, e);
                return Optional.empty();
            }
        }
        log.error("Reached max retry while waiting command completion [chainTaskId:{}, maxAttempts:{}]",
                chainTaskId, maxAttempts);
        return Optional.empty();
    }

    /**
     * Retrieve and store the public chain config.
     */
    public PublicChainConfig getPublicChainConfig() {
        if (publicChainConfig != null) {
            return publicChainConfig;
        }
        try {
            publicChainConfig = blockchainAdapterClient.getPublicChainConfig();
            log.info("Received public chain config [publicChainConfig:{}]", publicChainConfig);
            return publicChainConfig;
        } catch (FeignException e) {
            log.error("Failed to get public chain config:", e);
        }
        return null;
    }

    @Bean
    public int getChainId() {
        return getPublicChainConfig().getChainId();
    }

}