package com.lu4mic.workflow_engine.exception;

import java.util.UUID;

public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(UUID workflowId) {
        super("Workflow not found: " + workflowId);
    }
}
