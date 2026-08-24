package com.lu4mic.workflow_engine.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowRequest(

        // O nome precisa existir e não pode ser vazio.
        @NotBlank String name,
        String description

) {
}
