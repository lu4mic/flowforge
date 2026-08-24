package com.lu4mic.workflow_engine.exception;

import java.util.UUID;

public class WorkflowDependencyAlreadyExistsException extends RuntimeException {

    public WorkflowDependencyAlreadyExistsException(
            UUID prerequisiteTaskId,
            UUID dependentTaskId) {
        super("Dependency already exists: " + prerequisiteTaskId + " -> " + dependentTaskId);
    }
}
