package com.lu4mic.workflow_engine.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * ENTITY.
 *
 * Representa um Workflow dentro da nossa aplicação
 * e também representa uma tabela no banco de dados.
 *
 * Diferente do Request, esse objeto possui informações
 * que o próprio sistema controla.
 */
@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;
    private String description;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private Integer version;

    /*
     * JPA precisa de um construtor vazio.
     */
    protected Workflow() {
    }

    public Workflow(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.version = 1;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getVersion() {
        return version;
    }

    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}