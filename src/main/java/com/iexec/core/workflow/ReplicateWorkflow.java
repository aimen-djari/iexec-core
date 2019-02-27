package com.iexec.core.workflow;

import com.iexec.common.replicate.ReplicateStatus;

import static com.iexec.common.replicate.ReplicateStatus.*;


public class ReplicateWorkflow extends Workflow<ReplicateStatus> {

    private static ReplicateWorkflow instance;

    private ReplicateWorkflow() {
        super();

        // This is where the whole workflow is defined
        addTransition(CREATED, RUNNING);
        addTransition(RUNNING, APP_DOWNLOADING);

        // app
        addTransition(APP_DOWNLOADING, toList(APP_DOWNLOADED, APP_DOWNLOAD_FAILED));

        addTransition(APP_DOWNLOAD_FAILED, toList(
                // DATA_DOWNLOADING,
                CANT_CONTRIBUTE_SINCE_STAKE_TOO_LOW,
                CANT_CONTRIBUTE_SINCE_TASK_NOT_ACTIVE,
                CANT_CONTRIBUTE_SINCE_AFTER_DEADLINE,
                CANT_CONTRIBUTE_SINCE_CONTRIBUTION_ALREADY_SET,
                CAN_CONTRIBUTE));

        addTransition(APP_DOWNLOADED, DATA_DOWNLOADING);

        // data
        addTransition(DATA_DOWNLOADING, toList(DATA_DOWNLOADED, DATA_DOWNLOAD_FAILED));

        addTransition(DATA_DOWNLOAD_FAILED, toList(
                // COMPUTING,
                CANT_CONTRIBUTE_SINCE_STAKE_TOO_LOW,
                CANT_CONTRIBUTE_SINCE_TASK_NOT_ACTIVE,
                CANT_CONTRIBUTE_SINCE_AFTER_DEADLINE,
                CANT_CONTRIBUTE_SINCE_CONTRIBUTION_ALREADY_SET,
                CAN_CONTRIBUTE));

        addTransition(DATA_DOWNLOADED, COMPUTING);

        // computation
        addTransition(COMPUTING, toList(COMPUTED, COMPUTE_FAILED));

        addTransition(COMPUTED, toList(
                CANT_CONTRIBUTE_SINCE_STAKE_TOO_LOW,
                CANT_CONTRIBUTE_SINCE_TASK_NOT_ACTIVE,
                CANT_CONTRIBUTE_SINCE_AFTER_DEADLINE,
                CANT_CONTRIBUTE_SINCE_CONTRIBUTION_ALREADY_SET,
                CAN_CONTRIBUTE));

        // contribution
        addTransition(CAN_CONTRIBUTE, toList(CONTRIBUTING, OUT_OF_GAS));
        addTransition(CONTRIBUTING, toList(CONTRIBUTED, CONTRIBUTE_FAILED));
        addTransitionFromStatusBeforeContributedToGivenStatus(ABORTED_ON_CONTRIBUTION_TIMEOUT);
        addTransitionFromStatusBeforeContributedToGivenStatus(ABORTED_ON_CONSENSUS_REACHED);

        // reveal - completed
        addTransition(CONTRIBUTED, toList(CANT_REVEAL, OUT_OF_GAS, REVEALING, REVEAL_TIMEOUT));
        addTransition(REVEALING, toList(REVEAL_TIMEOUT, REVEALED, REVEAL_FAILED));
        addTransition(REVEALED, toList(RESULT_UPLOADING, COMPLETED));
        addTransition(RESULT_UPLOADING, toList(RESULT_UPLOADED, RESULT_UPLOAD_REQUEST_FAILED));
        addTransition(WORKER_LOST, RESULT_UPLOAD_REQUEST_FAILED);
        addTransition(RESULT_UPLOADED, COMPLETED);

        // worker_lost
        addTransition(WORKER_LOST, toList(
                ABORTED_ON_CONSENSUS_REACHED,
                ABORTED_ON_CONTRIBUTION_TIMEOUT,
                RESULT_UPLOAD_REQUEST_FAILED,
                RESULT_UPLOAD_FAILED,
                COMPLETED,
                REVEAL_TIMEOUT));

        // from any status to WORKER_LOST or ERROR
        addTransitionToAllStatuses(WORKER_LOST);
        addTransitionToAllStatuses(ERROR);
    }

    public static synchronized ReplicateWorkflow getInstance() {
        if (instance == null) {
            instance = new ReplicateWorkflow();
        }
        return instance;
    }

    private void addTransitionFromStatusBeforeContributedToGivenStatus(ReplicateStatus to) {
        for (ReplicateStatus from : getStatusesBeforeContributed()) {
            addTransition(from, to);
        }

        addTransition(CONTRIBUTED, to);
        addTransition(OUT_OF_GAS, to);
        addTransition(WORKER_LOST, to);
    }
}
