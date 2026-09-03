package com.lu4mic.workflow_engine.exception;

import java.util.UUID;

public abstract class TaskExecutionException extends RuntimeException {
    private final UUID taskRunId;

    protected TaskExecutionException(UUID taskRunId, String message, Throwable cause) {
        super(message, cause);
        this.taskRunId = taskRunId;
    }

    public UUID getTaskRunId() {
        return taskRunId;
    }
}
