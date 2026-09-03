package com.lu4mic.workflow_engine.exception;

import java.util.UUID;

public class HttpTaskExecutionException extends TaskExecutionException {

    public HttpTaskExecutionException(
            UUID taskRunId,
            String reason,
            Throwable cause) {
        super(taskRunId, "HTTP task execution failed for TaskRun " + taskRunId + ": " + reason, cause);
    }
}
