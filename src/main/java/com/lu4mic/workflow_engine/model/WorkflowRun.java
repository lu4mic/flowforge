package com.lu4mic.workflow_engine.model;

import java.time.LocalDateTime;
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

@Entity
@Table(name = "run")
public class WorkflowRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private Integer workflowVersion;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkflowRunStatus status;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    protected WorkflowRun() {
    }

    public WorkflowRun(Workflow workflow) {
        this.workflow = workflow;
        this.workflowVersion = workflow.getVersion();
        this.status = WorkflowRunStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.startedAt = null;
        this.completedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public WorkflowRunStatus getStatus() {
        return status;
    }

    public void start() {
        if (status != WorkflowRunStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot start workflow. Current status is " + this.status + ", but PENDING is required.");
        }
        status = WorkflowRunStatus.RUNNING;
        startedAt = LocalDateTime.now();
    }

    public void succeed() {
        if (status != WorkflowRunStatus.RUNNING) {
            throw new IllegalStateException(
                    "Cannot start workflow. Current status is " + this.status + ", but RUNNING is required.");
        }
        status = WorkflowRunStatus.SUCCEEDED;
        completedAt = LocalDateTime.now();
    }

    public void fail() {
        status = WorkflowRunStatus.FAILED;
        completedAt = LocalDateTime.now();
    }

    public void cancel() {
        status = WorkflowRunStatus.CANCELLED;
        completedAt = LocalDateTime.now();
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

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

}
