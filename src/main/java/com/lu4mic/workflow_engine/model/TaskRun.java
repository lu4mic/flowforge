package com.lu4mic.workflow_engine.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "task_runs", uniqueConstraints = @UniqueConstraint(name = "uk_task_runs_workflow_run_task", columnNames = {
        "workflow_run_id", "workflow_task_id" }))
public class TaskRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "workflow_run_id", nullable = false)
    private WorkflowRun workflowRun;

    @ManyToOne
    @JoinColumn(name = "workflow_task_id", nullable = false)
    private WorkflowTask workflowTask;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskRunStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readyAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime nextAttemptAt;
    private UUID leaseOwner;
    private LocalDateTime leaseExpiresAt;

    protected TaskRun() {
    }

    public TaskRun(WorkflowRun workflowRun, WorkflowTask workflowTask) {
        this.workflowRun = workflowRun;
        this.workflowTask = workflowTask;
        this.status = TaskRunStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.readyAt = null;
        this.startedAt = null;
        this.completedAt = null;
        this.nextAttemptAt = null;
        this.leaseOwner = null;
        this.leaseExpiresAt = null;
    }

    public UUID getId() {
        return id;
    }

    public WorkflowRun getWorkflowRun() {
        return workflowRun;
    }

    public WorkflowTask getWorkflowTask() {
        return workflowTask;
    }

    public TaskRunStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public UUID getLeaseOwner() {
        return leaseOwner;
    }

    public LocalDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void markReady() {
        if (status != TaskRunStatus.PENDING) {
            throw new IllegalStateException("Only a pending task can become ready");
        }

        status = TaskRunStatus.READY;
        readyAt = LocalDateTime.now();
    }

    public void start(UUID workerId, LocalDateTime now) {
        if (status != TaskRunStatus.READY) {
            throw new IllegalStateException("Only a ready task can start");
        }
        requireActiveLease(workerId, now);
        status = TaskRunStatus.RUNNING;
        startedAt = now;
        nextAttemptAt = null;
    }

    public void succeed() {
        if (status != TaskRunStatus.RUNNING) {
            throw new IllegalStateException("Only a running task can succeed");
        }
        status = TaskRunStatus.SUCCEEDED;
        completedAt = LocalDateTime.now();
        clearLease();
    }

    public void fail() {
        if (status != TaskRunStatus.RUNNING) {
            throw new IllegalStateException("Only a running task can fail");
        }

        status = TaskRunStatus.FAILED;
        completedAt = LocalDateTime.now();
        clearLease();
    }

    public void prepareRetry(LocalDateTime nextAttemptAt) {
        if (status != TaskRunStatus.RUNNING) {
            throw new IllegalStateException("Only a running task can be retried");
        }

        status = TaskRunStatus.READY;
        readyAt = LocalDateTime.now();
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt is required");
        clearLease();
    }

    public void requireActiveLease(UUID workerId, LocalDateTime now) {
        if (!Objects.equals(leaseOwner, workerId)
                || leaseExpiresAt == null
                || !leaseExpiresAt.isAfter(now)) {
            throw new IllegalStateException("TaskRun is not leased by worker " + workerId);
        }
    }

    private void clearLease() {
        leaseOwner = null;
        leaseExpiresAt = null;
    }
}
