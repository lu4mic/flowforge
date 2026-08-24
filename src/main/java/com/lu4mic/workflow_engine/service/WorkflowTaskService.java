package com.lu4mic.workflow_engine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.exception.WorkflowNotFoundException;
import com.lu4mic.workflow_engine.model.TaskHttpMethod;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.model.TaskType;
import com.lu4mic.workflow_engine.repository.WorkflowRepository;
import com.lu4mic.workflow_engine.repository.WorkflowTaskRepository;

@Service
public class WorkflowTaskService {
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowTaskService(WorkflowTaskRepository taskRepository, WorkflowRepository workflowRepository) {
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
    }

    public WorkflowTask createTask(
            UUID id,
            String key,
            String name,
            TaskType type,
            Long delayDurationMs,
            TaskHttpMethod httpMethod,
            String httpUrl) {
        return workflowRepository.findById(id)
                .map(workflow -> new WorkflowTask(key, name, type, delayDurationMs, httpMethod, httpUrl, workflow))
                .map(taskRepository::save)
                .orElseThrow(() -> new WorkflowNotFoundException(id));
    }

    public List<WorkflowTask> getAllWorkflowTasks(UUID workflowId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowNotFoundException(workflowId);
        }

        return taskRepository.findAllByWorkflow_Id(workflowId);
    }
}
