package com.lu4mic.workflow_engine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowTask;

public interface TaskRunRepository extends JpaRepository<TaskRun, UUID> {

    Optional<TaskRun> findByWorkflowRun_IdAndWorkflowTask_Id(
            UUID workflowRunId,
            UUID workflowTaskId);

    List<TaskRun> findAllByWorkflowRun_Id(UUID workflowRunId);

    List<TaskRun> findAllByStatus(TaskRunStatus taskRunStatus);
}
