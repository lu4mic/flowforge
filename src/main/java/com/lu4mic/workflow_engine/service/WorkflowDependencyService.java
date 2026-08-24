package com.lu4mic.workflow_engine.service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lu4mic.workflow_engine.exception.InvalidWorkflowDependencyException;
import com.lu4mic.workflow_engine.exception.TaskNotFoundException;
import com.lu4mic.workflow_engine.exception.WorkflowDependencyAlreadyExistsException;
import com.lu4mic.workflow_engine.exception.WorkflowNotFoundException;
import com.lu4mic.workflow_engine.model.WorkflowDependency;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.repository.WorkflowDependencyRepository;
import com.lu4mic.workflow_engine.repository.WorkflowRepository;
import com.lu4mic.workflow_engine.repository.WorkflowTaskRepository;

@Service
public class WorkflowDependencyService {
    private final WorkflowDependencyRepository dependencyRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowDependencyService(
            WorkflowDependencyRepository dependencyRepository,
            WorkflowTaskRepository taskRepository,
            WorkflowRepository workflowRepository) {
        this.dependencyRepository = dependencyRepository;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public WorkflowDependency createDependency(
            UUID workflowId,
            UUID prerequisiteTaskId,
            UUID dependentTaskId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowNotFoundException(workflowId);
        }

        WorkflowTask prerequisiteTask = findTask(prerequisiteTaskId);
        WorkflowTask dependentTask = findTask(dependentTaskId);
        validateTasks(workflowId, prerequisiteTask, dependentTask);

        WorkflowDependency dependency = new WorkflowDependency(prerequisiteTask, dependentTask);
        return dependencyRepository.save(dependency);
    }

    private WorkflowTask findTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private void validateTasks(
            UUID workflowId,
            WorkflowTask prerequisiteTask,
            WorkflowTask dependentTask) {
        if (prerequisiteTask.getId().equals(dependentTask.getId())) {
            throw new InvalidWorkflowDependencyException(
                    "Prerequisite and dependent tasks must be different");
        }

        validateTaskBelongsToWorkflow(workflowId, prerequisiteTask);
        validateTaskBelongsToWorkflow(workflowId, dependentTask);

        if (dependencyRepository.existsByPrerequisiteTask_IdAndDependentTask_Id(
                prerequisiteTask.getId(),
                dependentTask.getId())) {
            throw new WorkflowDependencyAlreadyExistsException(
                    prerequisiteTask.getId(),
                    dependentTask.getId());
        }

        if (isThisACycle(prerequisiteTask, dependentTask)) {
            throw new InvalidWorkflowDependencyException(
                    "Dependency would create a cycle");
        }
    }

    private void validateTaskBelongsToWorkflow(UUID workflowId, WorkflowTask task) {
        if (!task.getWorkflow().getId().equals(workflowId)) {
            throw new InvalidWorkflowDependencyException(
                    "Task " + task.getId() + " does not belong to workflow " + workflowId);
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowDependency> getAllWorkflowDependencies(UUID workflowId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowNotFoundException(workflowId);
        }

        return dependencyRepository.findAllByPrerequisiteTask_Workflow_Id(workflowId);
    }

    public boolean isThisACycle(WorkflowTask prerequisiteTask, WorkflowTask dependentTask) {
        Queue<WorkflowTask> taskQueue = new LinkedList<>();
        Set<UUID> visitedTaskIds = new HashSet<>();
        taskQueue.add(dependentTask);

        while (!taskQueue.isEmpty()) {
            WorkflowTask currentTask = taskQueue.poll();

            if (currentTask.getId().equals(prerequisiteTask.getId())) {
                return true;
            }

            if (!visitedTaskIds.add(currentTask.getId())) {
                continue;
            }

            List<WorkflowDependency> tasks = dependencyRepository
                    .findAllByPrerequisiteTask_Id(currentTask.getId());
            for (WorkflowDependency dependencyTask : tasks) {
                taskQueue.add(dependencyTask.getDependentTask());
            }
        }

        return false;
    }
}
