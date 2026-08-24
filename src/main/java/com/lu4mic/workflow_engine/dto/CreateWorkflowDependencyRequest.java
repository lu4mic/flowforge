package com.lu4mic.workflow_engine.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateWorkflowDependencyRequest(
        @NotNull UUID prerequisiteTaskId,
        @NotNull UUID dependentTaskId) {

}
