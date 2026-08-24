package com.lu4mic.workflow_engine.dto;

import com.lu4mic.workflow_engine.model.TaskHttpMethod;
import com.lu4mic.workflow_engine.model.TaskType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateWorkflowTaskRequest(
                @NotBlank String key,
                @NotBlank String name,
                @NotNull TaskType type,
                @Positive Long delayDurationMs,
                TaskHttpMethod httpMethod,
                String httpUrl) {

}
