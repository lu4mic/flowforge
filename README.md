# Durable Workflow Engine

A backend workflow orchestration engine built with **Java 21, Spring Boot, JPA, and PostgreSQL**.

The project models workflows as **DAGs (Directed Acyclic Graphs)** and keeps execution state durable in the database. The goal is to evolve it into a distributed workflow engine with scheduling, retries, worker coordination, and Azure-based execution.

## What it supports today

- Create and query workflows
- Add `HTTP` and `DELAY` tasks
- Define task dependencies
- Prevent self-dependencies, duplicates, and graph cycles
- Create workflow executions (`WorkflowRun`)
- Create one execution record per task (`TaskRun`)
- Detect root tasks and mark them `READY`
- Track task lifecycle: `PENDING -> READY -> RUNNING -> SUCCEEDED/FAILED`
- Unlock dependent tasks when all prerequisites succeed
- Execute `DELAY` tasks
- Prepare `HTTP` tasks for execution

## Architecture

```text
Client
  |
  v
Controller
  |
  v
Service
  |
  v
Repository
  |
  v
PostgreSQL
```

Definition and execution are intentionally separated:

```text
Workflow       -> WorkflowRun
WorkflowTask   -> TaskRun
```

Dependencies are stored as:

```text
prerequisiteTask -> dependentTask
```

Example:

```text
A -> B -> C
```

means B depends on A, and C depends on B.

## Current execution flow

```text
Create WorkflowRun
      |
      v
Create TaskRuns
      |
      v
Root tasks -> READY
      |
      v
Execute task
      |
      v
RUNNING
      |
      v
SUCCEEDED / FAILED
      |
      v
Unlock downstream tasks
```

## Tech stack

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- Bean Validation

## Next milestones

- Execute real HTTP requests
- Automatic scheduler for `READY` tasks
- Workflow completion/failure propagation
- Retries and backoff
- Task attempts
- Worker leases and crash recovery
- Azure Queue Storage
- Azure Container Apps
- Docker and CI/CD

## Why this project

The project is designed to explore practical backend and distributed-systems problems such as:

- DAG scheduling
- durable state
- transactional state transitions
- retries
- idempotency
- duplicate delivery
- worker coordination
- crash recovery

See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for a short technical overview.
