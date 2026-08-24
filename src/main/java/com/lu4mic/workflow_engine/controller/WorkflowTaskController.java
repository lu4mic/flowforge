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

import com.lu4mic.workflow_engine.dto.CreateWorkflowTaskRequest;
import com.lu4mic.workflow_engine.dto.WorkflowTaskResponse;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.service.WorkflowTaskService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/workflows/{workflowId}/tasks")
public class WorkflowTaskController {
    private final WorkflowTaskService taskService;

    public WorkflowTaskController(WorkflowTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<WorkflowTaskResponse> createTask(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CreateWorkflowTaskRequest request) {
        WorkflowTask workflowTask = taskService.createTask(
                workflowId,
                request.key(),
                request.name(),
                request.type(),
                request.delayDurationMs(),
                request.httpMethod(),
                request.httpUrl());

        WorkflowTaskResponse response = new WorkflowTaskResponse(
                workflowTask.getId(),
                workflowTask.getName(),
                workflowTask.getKey(),
                workflowTask.getTaskType(),
                workflowTask.getDelayDurationMs(),
                workflowTask.getWorkflow().getId(),
                workflowTask.getHttpMethod(),
                workflowTask.getHttpUrl());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowTaskResponse>> listWorkflowTasks(@PathVariable UUID workflowId) {
        List<WorkflowTaskResponse> responses = taskService.getAllWorkflowTasks(workflowId)
                .stream()
                .map(task -> new WorkflowTaskResponse(
                        task.getId(),
                        task.getName(),
                        task.getKey(),
                        task.getTaskType(),
                        task.getDelayDurationMs(),
                        task.getWorkflow().getId(),
                        task.getHttpMethod(),
                        task.getHttpUrl()))
                .toList();

        return ResponseEntity.ok(responses);
    }

}
