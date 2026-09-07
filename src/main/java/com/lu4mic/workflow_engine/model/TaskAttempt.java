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
@Table(name = "task_attempts", uniqueConstraints = @UniqueConstraint(name = "uk_task_attempts_task_run_attempt_number", columnNames = {
        "task_run_id", "attempt_number" }))
public class TaskAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false) // Many task attempts can point to one task run
    @JoinColumn(name = "task_run_id", nullable = false)
    private TaskRun taskRun;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskAttemptStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    protected TaskAttempt() {
    }

    public TaskAttempt(TaskRun taskRun, Integer attemptNumber) {
        this.taskRun = Objects.requireNonNull(taskRun, "taskRun is required");
        this.attemptNumber = Objects.requireNonNull(attemptNumber, "attemptNumber is required");
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber must be greater than zero");
        }
        this.status = TaskAttemptStatus.RUNNING;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.startedAt = now;
        this.completedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public TaskRun getTaskRun() {
        return taskRun;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public TaskAttemptStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void succeed() {
        completeAs(TaskAttemptStatus.SUCCEEDED);
    }

    public void fail() {
        completeAs(TaskAttemptStatus.FAILED);
    }

    private void completeAs(TaskAttemptStatus completedStatus) {
        if (status != TaskAttemptStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot complete task attempt. Current status is " + status + ", but RUNNING is required.");
        }

        status = completedStatus;
        completedAt = LocalDateTime.now();
    }
}
