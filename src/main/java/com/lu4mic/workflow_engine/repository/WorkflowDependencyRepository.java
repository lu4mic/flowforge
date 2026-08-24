package com.lu4mic.workflow_engine.repository;

import com.lu4mic.workflow_engine.model.WorkflowDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowDependencyRepository extends JpaRepository<WorkflowDependency, UUID> {

    List<WorkflowDependency> findAllByPrerequisiteTask_Workflow_Id(UUID workflowId);

    List<WorkflowDependency> findAllByPrerequisiteTask_Id(UUID prerequisiteTaskId);

    List<WorkflowDependency> findAllByDependentTask_Id(UUID dependentTaskId);

    boolean existsByDependentTask_Id(UUID dependentTaskId);

    boolean existsByPrerequisiteTask_IdAndDependentTask_Id(
            UUID prerequisiteTaskId,
            UUID dependentTaskId);
}
