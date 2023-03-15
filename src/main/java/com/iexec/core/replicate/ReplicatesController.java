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

package com.iexec.core.replicate;

import com.iexec.common.notification.TaskNotification;
import com.iexec.common.notification.TaskNotificationType;
import com.iexec.common.replicate.*;
import com.iexec.core.security.JwtTokenProvider;
import com.iexec.core.worker.WorkerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static org.springframework.http.ResponseEntity.status;

@RestController
public class ReplicatesController {

    private final ReplicatesService replicatesService;
    private final ReplicateSupplyService replicateSupplyService;
    private final JwtTokenProvider jwtTokenProvider;
    private final WorkerService workerService;

    public ReplicatesController(ReplicatesService replicatesService,
                                ReplicateSupplyService replicateSupplyService,
                                JwtTokenProvider jwtTokenProvider,
                                WorkerService workerService) {
        this.replicatesService = replicatesService;
        this.replicateSupplyService = replicateSupplyService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.workerService = workerService;
    }

    @GetMapping("/replicates/available")
    public ResponseEntity<ReplicateTaskSummary> getAvailableReplicateTaskSummary(
        @RequestParam(name = "blockNumber") long blockNumber,
        @RequestHeader("Authorization") String bearerToken) {
        String workerWalletAddress = jwtTokenProvider.getWalletAddressFromBearerToken(bearerToken);
        if (workerWalletAddress.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!workerService.isWorkerAllowedToAskReplicate(workerWalletAddress)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        workerService.updateLastReplicateDemandDate(workerWalletAddress);

        return replicateSupplyService
                .getAvailableReplicateTaskSummary(blockNumber, workerWalletAddress)
                .map(ResponseEntity::ok)
                .orElseGet(() -> status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping("/replicates/interrupted")
    public ResponseEntity<List<TaskNotification>> getMissedTaskNotifications(
            @RequestParam(name = "blockNumber") long blockNumber,
            @RequestHeader("Authorization") String bearerToken) {

        String workerWalletAddress = jwtTokenProvider.getWalletAddressFromBearerToken(bearerToken);
        if (workerWalletAddress.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<TaskNotification> missedTaskNotifications =
                replicateSupplyService.getMissedTaskNotifications(blockNumber, workerWalletAddress);

        return ResponseEntity.ok(missedTaskNotifications);
    }

    /**
     * Handles workers requests to update a replicate status.
     * <p>
     * The scheduler response can only be null on authentication failures.
     * In all other situations, a notification must be sent and the body cannot be null.
     *
     * @param bearerToken Authentication token of a worker.
     * @param chainTaskId ID of the task on which the worker has an update.
     * @param statusUpdate Status update sent by the worker.
     * @return A notification to the worker. A notification is implemented in {@code TaskNotificationType}.
     */
    @PostMapping("/replicates/{chainTaskId}/updateStatus")
    public ResponseEntity<TaskNotificationType> updateReplicateStatus(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable(name = "chainTaskId") String chainTaskId,
            @RequestBody ReplicateStatusUpdate statusUpdate) {

        String walletAddress = jwtTokenProvider.getWalletAddressFromBearerToken(bearerToken);

        if (walletAddress.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        statusUpdate.setModifier(ReplicateStatusModifier.WORKER);
        statusUpdate.setDate(new Date());

        // Assuming wallet address sent by the worker is correct
        // would be a security issue. Let's replace it.
        final ReplicateStatusDetails details = statusUpdate.getDetails();
        if (details != null) {
            final ComputeLogs computeLogs = details.getComputeLogs();
            if (computeLogs != null) {
                computeLogs.setWalletAddress(walletAddress);
            }
        }

        final UpdateReplicateStatusArgs updateReplicateStatusArgs = replicatesService.computeUpdateReplicateStatusArgs(
                chainTaskId,
                walletAddress,
                statusUpdate);
        final ReplicateStatusUpdateError replicateStatusUpdateError = replicatesService.canUpdateReplicateStatus(
                chainTaskId,
                walletAddress,
                statusUpdate,
                updateReplicateStatusArgs);

        switch (replicateStatusUpdateError) {
            case NO_ERROR:
                return replicatesService
                        .updateReplicateStatus(chainTaskId, walletAddress, statusUpdate, updateReplicateStatusArgs)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.ok(TaskNotificationType.PLEASE_ABORT));
            case ALREADY_REPORTED:
                return ResponseEntity.status(HttpStatus.ALREADY_REPORTED)
                        .body(TaskNotificationType.PLEASE_WAIT);
            case UNKNOWN_REPLICATE:
            case BAD_WORKFLOW_TRANSITION:
            case GENERIC_CANT_UPDATE:
            default:
                return ResponseEntity.ok(TaskNotificationType.PLEASE_ABORT);
        }
    }
}
