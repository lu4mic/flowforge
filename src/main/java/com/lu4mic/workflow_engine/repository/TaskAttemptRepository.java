package com.lu4mic.workflow_engine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lu4mic.workflow_engine.model.TaskAttempt;
import com.lu4mic.workflow_engine.model.TaskRun;

public interface TaskAttemptRepository extends JpaRepository<TaskAttempt, UUID> {
    Optional<TaskAttempt> findTopByTaskRunOrderByAttemptNumberDesc(TaskRun taskRun);

    List<TaskAttempt> findAllByTaskRun_Id(UUID taskRunId);
}
