package com.lu4mic.workflow_engine.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "dependencies")
public class WorkflowDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "dependent_task_id", nullable = false)
    private WorkflowTask dependentTask;
    @ManyToOne
    @JoinColumn(name = "prerequisite_task_id", nullable = false)
    private WorkflowTask prerequisiteTask;

    protected WorkflowDependency() {
    }

    public WorkflowDependency(WorkflowTask prerequisiteTask, WorkflowTask dependentTask) {
        this.prerequisiteTask = prerequisiteTask;
        this.dependentTask = dependentTask;
    }

    public UUID getId() {
        return id;
    }

    public WorkflowTask getPrerequisiteTask() {
        return prerequisiteTask;
    }

    public WorkflowTask getDependentTask() {
        return dependentTask;
    }
}
