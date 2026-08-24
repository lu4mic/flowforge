package com.lu4mic.workflow_engine.repository;

import com.lu4mic.workflow_engine.model.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, UUID> {

    List<WorkflowTask> findAllByWorkflow_Id(UUID workflowId);

}
