package com.lu4mic.workflow_engine.exception;

import java.util.UUID;

public class DelayTaskExecutionException extends TaskExecutionException {

    public DelayTaskExecutionException(UUID taskRunId, Throwable cause) {
        super(
                taskRunId,
                "DELAY task execution was interrupted for TaskRun " + taskRunId,
                cause);
    }
}
