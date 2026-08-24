package com.lu4mic.workflow_engine.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lu4mic.workflow_engine.dto.CreateWorkflowDependencyRequest;
import com.lu4mic.workflow_engine.dto.WorkflowDependencyResponse;
import com.lu4mic.workflow_engine.model.WorkflowDependency;
import com.lu4mic.workflow_engine.service.WorkflowDependencyService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/workflows/{workflowId}/dependencies")
public class WorkflowDependencyController {
    private final WorkflowDependencyService dependencyService;

    public WorkflowDependencyController(WorkflowDependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @PostMapping
    public ResponseEntity<WorkflowDependencyResponse> createDependency(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CreateWorkflowDependencyRequest request) {
        WorkflowDependency dependency = dependencyService.createDependency(
                workflowId,
                request.prerequisiteTaskId(),
                request.dependentTaskId());

        WorkflowDependencyResponse response = new WorkflowDependencyResponse(
                dependency.getId(),
                workflowId,
                dependency.getPrerequisiteTask().getId(),
                dependency.getDependentTask().getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDependencyResponse>> listAllDependencies(
            @PathVariable UUID workflowId) {
        List<WorkflowDependencyResponse> responses = dependencyService
                .getAllWorkflowDependencies(workflowId)
                .stream()
                .map(dependency -> new WorkflowDependencyResponse(
                        dependency.getId(),
                        workflowId,
                        dependency.getPrerequisiteTask().getId(),
                        dependency.getDependentTask().getId()))
                .toList();

        return ResponseEntity.ok(responses);
    }

}
