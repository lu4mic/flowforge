package com.lu4mic.workflow_engine.dto;

import java.util.UUID;

public record WorkflowDependencyResponse(
                UUID id,
                UUID workflowId,
                UUID prerequisiteTaskId,
                UUID dependentTaskId) {

}
