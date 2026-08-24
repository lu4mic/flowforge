package com.lu4mic.workflow_engine.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lu4mic.workflow_engine.service.WorkflowService;
import com.lu4mic.workflow_engine.dto.CreateWorkflowRequest;
import com.lu4mic.workflow_engine.dto.WorkflowResponse;
import com.lu4mic.workflow_engine.model.Workflow;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        Workflow workflow = service.createWorkflow(request.name(), request.description());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new WorkflowResponse(workflow.getId(), workflow.getName(), workflow.getDescription(),
                        workflow.getCreatedAt(), workflow.getUpdatedAt(), workflow.getVersion()));

    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(@PathVariable UUID id) {

        return service.findWorkflow(id)
                .map(workflow -> new WorkflowResponse(
                        workflow.getId(),
                        workflow.getName(),
                        workflow.getDescription(),
                        workflow.getCreatedAt(),
                        workflow.getUpdatedAt(),
                        workflow.getVersion()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getWorkflowList() {
        List<WorkflowResponse> responses = service.getAllWorkflow()
                .stream()
                .map(workflow -> new WorkflowResponse(
                        workflow.getId(),
                        workflow.getName(),
                        workflow.getDescription(),
                        workflow.getCreatedAt(),
                        workflow.getUpdatedAt(),
                        workflow.getVersion()))
                .toList();

        return ResponseEntity.ok(responses);
    }

}
