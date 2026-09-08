package com.lu4mic.workflow_engine.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowRunStatus;
import com.lu4mic.workflow_engine.repository.TaskRunRepository;

import jakarta.transaction.Transactional;

@Service
public class TaskLeaseService {
    static final Duration LEASE_DURATION = Duration.ofSeconds(30);
    // Execution is synchronous today, so claiming one row avoids leases expiring while
    // claimed work waits behind another task in this process.
    static final int CLAIM_BATCH_SIZE = 1;
    private static final List<WorkflowRunStatus> EXECUTABLE_WORKFLOW_STATUSES = List.of(
            WorkflowRunStatus.PENDING,
            WorkflowRunStatus.RUNNING);

    private final TaskRunRepository taskRunRepository;
    private final WorkerIdentity workerIdentity;

    public TaskLeaseService(TaskRunRepository taskRunRepository, WorkerIdentity workerIdentity) {
        this.taskRunRepository = taskRunRepository;
        this.workerIdentity = workerIdentity;
    }

    @Transactional
    public List<UUID> claimEligibleTaskRuns() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaseExpiresAt = now.plus(LEASE_DURATION);
        List<UUID> candidates = taskRunRepository.findEligibleIds(
                TaskRunStatus.READY,
                EXECUTABLE_WORKFLOW_STATUSES,
                now,
                PageRequest.of(0, CLAIM_BATCH_SIZE));
        List<UUID> claimed = new ArrayList<>(candidates.size());

        for (UUID taskRunId : candidates) {
            int updated = taskRunRepository.claimIfEligible(
                    taskRunId,
                    workerIdentity.getId(),
                    TaskRunStatus.READY,
                    EXECUTABLE_WORKFLOW_STATUSES,
                    now,
                    leaseExpiresAt);
            if (updated == 1) {
                claimed.add(taskRunId);
            }
        }

        return claimed;
    }

    @Transactional
    @Scheduled(fixedDelay = 10_000)
    public void renewOwnedRunningLeases() {
        LocalDateTime now = LocalDateTime.now();
        taskRunRepository.renewOwnedLeases(
                workerIdentity.getId(),
                TaskRunStatus.RUNNING,
                now,
                now.plus(LEASE_DURATION));
    }

    public UUID getWorkerId() {
        return workerIdentity.getId();
    }
}
