package com.lu4mic.workflow_engine.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.dto.HttpTaskExecutionResponse;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.TaskType;
import com.lu4mic.workflow_engine.model.WorkflowDependency;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.repository.TaskRunRepository;
import com.lu4mic.workflow_engine.repository.WorkflowDependencyRepository;

import jakarta.transaction.Transactional;

@Service
public class TaskRunService {
    private final WorkflowDependencyRepository dependencyRepository;
    private final TaskRunRepository taskRunRepository;

    public TaskRunService(
            WorkflowDependencyRepository dependencyRepository,
            TaskRunRepository taskRunRepository) {
        this.dependencyRepository = dependencyRepository;
        this.taskRunRepository = taskRunRepository;
    }

    @Transactional
    public long startDelayTaskRun(UUID taskRunId) {
        TaskRun taskRun = findTaskRun(taskRunId);
        WorkflowTask workflowTask = taskRun.getWorkflowTask();

        if (workflowTask.getTaskType() != TaskType.DELAY) {
            throw new IllegalStateException("Only DELAY tasks are supported for execution");
        }

        Long delayDurationMs = workflowTask.getDelayDurationMs();
        if (delayDurationMs == null || delayDurationMs <= 0) {
            throw new IllegalStateException("DELAY task must have a positive delayDurationMs");
        }

        taskRun.start();
        return delayDurationMs;
    }

    @Transactional
    public void completeTaskRun(UUID taskRunId) {
        TaskRun task = findTaskRun(taskRunId);

        task.succeed();

        markUnblockedDependentTasksReady(task);
    }

    @Transactional
    public void failTaskRun(UUID taskRunId) {
        findTaskRun(taskRunId).fail();
    }

    private void markUnblockedDependentTasksReady(TaskRun completedTaskRun) {
        if (completedTaskRun.getStatus() != TaskRunStatus.SUCCEEDED) {
            throw new IllegalStateException("TaskRun must be SUCCEEDED before checking dependent tasks");
        }

        UUID workflowRunId = completedTaskRun.getWorkflowRun().getId();
        UUID completedWorkflowTaskId = completedTaskRun.getWorkflowTask().getId();

        List<WorkflowDependency> outgoingDependencies = dependencyRepository
                .findAllByPrerequisiteTask_Id(completedWorkflowTaskId);

        for (WorkflowDependency outgoingDependency : outgoingDependencies) {
            WorkflowTask dependentTask = outgoingDependency.getDependentTask();

            List<WorkflowDependency> prerequisiteDependencies = dependencyRepository
                    .findAllByDependentTask_Id(dependentTask.getId());

            boolean allPrerequisitesSucceeded = prerequisiteDependencies.stream()
                    .allMatch(prerequisiteDependency -> {
                        TaskRun prerequisiteTaskRun = findTaskRun(
                                workflowRunId,
                                prerequisiteDependency.getPrerequisiteTask().getId());

                        return prerequisiteTaskRun.getStatus() == TaskRunStatus.SUCCEEDED;
                    });

            if (allPrerequisitesSucceeded) {
                TaskRun dependentTaskRun = findTaskRun(workflowRunId, dependentTask.getId());

                if (dependentTaskRun.getStatus() == TaskRunStatus.PENDING) {
                    dependentTaskRun.markReady();
                }
            }
        }
    }

    private TaskRun findTaskRun(UUID workflowRunId, UUID workflowTaskId) {
        return taskRunRepository
                .findByWorkflowRun_IdAndWorkflowTask_Id(workflowRunId, workflowTaskId)
                .orElseThrow(() -> new IllegalStateException(
                        "TaskRun not found for workflowRun " + workflowRunId
                                + " and workflowTask " + workflowTaskId));
    }

    private TaskRun findTaskRun(UUID taskRunId) {
        return taskRunRepository.findById(taskRunId)
                .orElseThrow(() -> new IllegalStateException(
                        "TaskRun not found: " + taskRunId));
    }

    @Transactional
    public HttpTaskExecutionResponse startHttpTaskRun(UUID taskRunId) {
        TaskRun taskRun = taskRunRepository.findById(taskRunId)
                .orElseThrow(() -> new IllegalStateException("TaskRun not found"));

        WorkflowTask taskRunWorkflowTask = taskRun.getWorkflowTask();

        if (taskRunWorkflowTask.getTaskType() != TaskType.HTTP) {
            throw new IllegalStateException("TaskRun must reference an HTTP task");
        }

        if (taskRunWorkflowTask.getHttpMethod() == null) {
            throw new IllegalStateException("HTTP task must have an httpMethod");
        }

        if (taskRunWorkflowTask.getHttpUrl() == null || taskRunWorkflowTask.getHttpUrl().isBlank()) {
            throw new IllegalStateException("HTTP task must have a non-blank httpUrl");
        }

        taskRun.start();

        return new HttpTaskExecutionResponse(
                taskRunWorkflowTask.getHttpMethod(),
                taskRunWorkflowTask.getHttpUrl());
    }
}
