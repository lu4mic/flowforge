package com.lu4mic.workflow_engine.service;

import com.lu4mic.workflow_engine.repository.TaskRunRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.exception.WorkflowNotFoundException;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.WorkflowRun;
import com.lu4mic.workflow_engine.repository.WorkflowDependencyRepository;
import com.lu4mic.workflow_engine.repository.WorkflowRepository;
import com.lu4mic.workflow_engine.repository.WorkflowRunRepository;
import com.lu4mic.workflow_engine.repository.WorkflowTaskRepository;

import jakarta.transaction.Transactional;

@Service
public class WorkflowRunService {
    private final TaskRunRepository taskRunRepository;
    private final WorkflowRunRepository runRepository;
    private final WorkflowDependencyRepository dependencyRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowRunService(WorkflowRunRepository runRepository, WorkflowDependencyRepository dependencyRepository,
            WorkflowTaskRepository taskRepository, WorkflowRepository workflowRepository,
            TaskRunRepository taskRunRepository) {
        this.runRepository = runRepository;
        this.dependencyRepository = dependencyRepository;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
        this.taskRunRepository = taskRunRepository;
    }

    @Transactional
    public WorkflowRun createRun(UUID id) {
        WorkflowRun workflowRun = workflowRepository.findById(id)
                .map(WorkflowRun::new)
                .map(runRepository::save)
                .orElseThrow(() -> new WorkflowNotFoundException(id));

        List<TaskRun> taskRuns = taskRepository.findAllByWorkflow_Id(id)
                .stream()
                .map(workflowTask -> new TaskRun(workflowRun, workflowTask))
                .toList();

        for (TaskRun taskRun : taskRuns) {
            if (!dependencyRepository.existsByDependentTask_Id(taskRun.getWorkflowTask().getId())) {
                taskRun.markReady();
            }
        }

        taskRunRepository.saveAll(taskRuns);

        return workflowRun;
    }

}
