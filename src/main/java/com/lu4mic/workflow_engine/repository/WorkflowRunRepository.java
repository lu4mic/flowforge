package com.lu4mic.workflow_engine.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lu4mic.workflow_engine.model.WorkflowRun;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

}
