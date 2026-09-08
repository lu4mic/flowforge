package com.lu4mic.workflow_engine.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class WorkerIdentity {
    private final UUID id;

    public WorkerIdentity() {
        this(UUID.randomUUID());
    }

    WorkerIdentity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
