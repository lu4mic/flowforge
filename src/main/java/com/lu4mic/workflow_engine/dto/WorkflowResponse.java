package com.lu4mic.workflow_engine.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowResponse(

        UUID id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version

) {
}
