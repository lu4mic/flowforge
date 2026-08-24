package com.lu4mic.workflow_engine.dto;

import com.lu4mic.workflow_engine.model.TaskHttpMethod;

public record HttpTaskExecutionResponse(
        TaskHttpMethod method,
        String url) {

}
