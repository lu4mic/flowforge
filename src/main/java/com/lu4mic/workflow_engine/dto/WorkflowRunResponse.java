package com.lu4mic.workflow_engine.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.lu4mic.workflow_engine.model.WorkflowRunStatus;

public record WorkflowRunResponse(
        UUID id,
        UUID workflowId,
        Integer workflowVersion,
        WorkflowRunStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {

}
