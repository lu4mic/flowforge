package com.lu4mic.workflow_engine.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lu4mic.workflow_engine.dto.WorkflowRunResponse;
import com.lu4mic.workflow_engine.model.WorkflowRun;
import com.lu4mic.workflow_engine.service.WorkflowRunService;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/workflows/{workflowId}/runs")
public class WorkflowRunController {
    private final WorkflowRunService workflowRunService;

    public WorkflowRunController(WorkflowRunService workflowRunService) {
        this.workflowRunService = workflowRunService;
    }

    @PostMapping
    public ResponseEntity<WorkflowRunResponse> createRun(@PathVariable UUID workflowId) {
        WorkflowRun workflowRun = workflowRunService.createRun(workflowId);
        WorkflowRunResponse response = new WorkflowRunResponse(
                workflowRun.getId(),
                workflowRun.getWorkflow().getId(),
                workflowRun.getWorkflowVersion(),
                workflowRun.getStatus(),
                workflowRun.getCreatedAt(),
                workflowRun.getStartedAt(),
                workflowRun.getCompletedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
