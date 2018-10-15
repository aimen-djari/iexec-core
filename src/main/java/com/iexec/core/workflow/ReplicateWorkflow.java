package com.iexec.core.workflow;

import com.iexec.common.replicate.ReplicateStatus;

import static com.iexec.common.replicate.ReplicateStatus.*;

public class ReplicateWorkflow extends Workflow<ReplicateStatus> {

    private static ReplicateWorkflow instance;

    public static synchronized ReplicateWorkflow getInstance() {
        if (instance == null) {
            instance = new ReplicateWorkflow();
        }
        return instance;
    }

    private ReplicateWorkflow() {
        super();

        // This is where the whole workflow is defined
        addTransition(CREATED, RUNNING);
        addTransition(RUNNING, COMPUTED);
        addTransition(COMPUTED, UPLOADING_RESULT);
        addTransition(UPLOADING_RESULT, RESULT_UPLOADED);
        addTransition(UPLOADING_RESULT, UPLOAD_RESULT_REQUEST_FAILED);
        addTransition(UPLOADING_RESULT, ERROR);
    }
}