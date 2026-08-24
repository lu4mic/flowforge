package com.lu4mic.workflow_engine.repository;

import com.lu4mic.workflow_engine.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

}
