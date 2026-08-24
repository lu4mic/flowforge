package com.lu4mic.workflow_engine.dto;

import java.util.UUID;

import com.lu4mic.workflow_engine.model.TaskHttpMethod;
import com.lu4mic.workflow_engine.model.TaskType;

public record WorkflowTaskResponse(
                UUID id,
                String name,
                String key,
                TaskType type,
                Long delayDurationMs,
                UUID workflowId,
                TaskHttpMethod httpMethod,
                String httpUrl

) {

}
