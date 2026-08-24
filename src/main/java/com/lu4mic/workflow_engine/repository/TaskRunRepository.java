package com.lu4mic.workflow_engine.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lu4mic.workflow_engine.model.TaskRun;

public interface TaskRunRepository extends JpaRepository<TaskRun, UUID> {

    Optional<TaskRun> findByWorkflowRun_IdAndWorkflowTask_Id(
            UUID workflowRunId,
            UUID workflowTaskId);
}
