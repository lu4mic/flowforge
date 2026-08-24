package com.lu4mic.workflow_engine.model;

import java.util.UUID;

import com.lu4mic.workflow_engine.exception.InvalidWorkflowTaskException;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class WorkflowTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String key;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;
    @Enumerated(EnumType.STRING)
    private TaskHttpMethod httpMethod;
    private String httpUrl;
    private Long delayDurationMs;
    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    protected WorkflowTask() {
    }

    public WorkflowTask(String key, String name, TaskType type, Long delayDurationMs, TaskHttpMethod httpMethod,
            String httpUrl, Workflow workflow) {
        validateTaskType(type, delayDurationMs, httpMethod, httpUrl);

        this.key = key;
        this.name = name;
        this.type = type;
        this.delayDurationMs = delayDurationMs;
        this.httpMethod = httpMethod;
        this.httpUrl = httpUrl;
        this.workflow = workflow;
    }

    private void validateTaskType(TaskType type, Long delayDurationMs, TaskHttpMethod httpMethod, String httpUrl) {
        if (type == null) {
            throw new InvalidWorkflowTaskException("Task type is required");
        }

        if (type == TaskType.HTTP) {
            if (httpMethod == null) {
                throw new InvalidWorkflowTaskException("httpMethod is required for HTTP tasks");
            }

            if (httpUrl == null || httpUrl.isBlank()) {
                throw new InvalidWorkflowTaskException("httpUrl is required and must not be blank for HTTP tasks");
            }

            if (delayDurationMs != null) {
                throw new InvalidWorkflowTaskException("delayDurationMs must be null for HTTP tasks");
            }
        }

        if (type == TaskType.DELAY) {
            if (delayDurationMs == null || delayDurationMs <= 0) {
                throw new InvalidWorkflowTaskException(
                        "delayDurationMs is required and must be greater than zero for DELAY tasks");
            }

            if (httpMethod != null) {
                throw new InvalidWorkflowTaskException("httpMethod must be null for DELAY tasks");
            }

            if (httpUrl != null) {
                throw new InvalidWorkflowTaskException("httpUrl must be null for DELAY tasks");
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public TaskType getTaskType() {
        return type;
    }

    public Long getDelayDurationMs() {
        return delayDurationMs;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public TaskHttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getHttpUrl() {
        return httpUrl;
    }
}
