package com.lu4mic.workflow_engine.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.model.Workflow;
import com.lu4mic.workflow_engine.repository.WorkflowRepository;

@Service
public class WorkflowService {

    private final WorkflowRepository repository;

    public WorkflowService(WorkflowRepository repository) {
        this.repository = repository;
    }

    public Workflow createWorkflow(String name, String description) {
        Workflow workflow = new Workflow(name, description);

        return repository.save(workflow);
    }

    public Optional<Workflow> findWorkflow(UUID id) {
        return repository.findById(id);
    }

    public List<Workflow> getAllWorkflow() {
        return repository.findAll();
    }
}
