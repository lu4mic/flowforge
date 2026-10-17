# Durable Workflow Engine

[![Application CI](https://github.com/lu4mic/flowforge/actions/workflows/application-ci.yml/badge.svg)](https://github.com/lu4mic/flowforge/actions/workflows/application-ci.yml)

A backend workflow orchestration engine built with **Java 21, Spring Boot, JPA, and PostgreSQL**.

The project models workflows as **DAGs (Directed Acyclic Graphs)** and keeps execution state durable in the database. It currently executes tasks through database polling, with retries, task attempts and worker leases. Queue dispatch, separate coordinator/worker roles and Azure deployment remain planned.

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
- Execute `HTTP` tasks with connection/read timeouts and treat non-2xx responses as failed attempts
- Poll eligible `READY` tasks and claim them atomically using a per-process worker identity
- Persist `TaskAttempt` records and retry failed attempts with exponential backoff (up to three total attempts)
- Renew owned running leases and recover expired running attempts through the retry/failure path
- Mark a workflow run successful when all tasks succeed, or failed when a task exhausts its attempts

These are implemented mechanisms, not a claim of exactly-once external effects or
production readiness. Re-executing an HTTP task after a failure or expired lease
can repeat an external side effect; task endpoints must handle that possibility.
PostgreSQL concurrency verification and backend hardening remain open backlog work.

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

## Local debug database boundary

The VS Code `Start debug PostgreSQL` task publishes PostgreSQL as
`127.0.0.1:5432:5432`, and the debugger connects to `127.0.0.1:5432`. Keep this
explicit loopback binding instead of publishing the development database on all
host interfaces. The checked-in database credentials are for local debugging
only, not shared or deployed environments.

Use Docker Engine 28.0.0 or newer for this boundary: older releases can expose
localhost-published ports to peers on the same network segment. See
[Docker's port-publishing security notes](https://docs.docker.com/engine/network/port-publishing/).
This host-port restriction is not authentication or isolation from other local
processes/containers; do not treat the debug database as production-hardened.

## Verify changes locally

Use Java 21 and a running Docker daemon accessible to your user. The PostgreSQL
lease tests use Testcontainers to start an isolated `postgres:16-alpine` container;
the first run needs access to pull that image and download Maven dependencies.
The suite supplies its own database connection settings and does not require a
manually provisioned PostgreSQL instance.

Run the same Maven lifecycle used by application CI:

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

To run only the PostgreSQL lease and concurrency suite:

```bash
./mvnw --batch-mode --no-transfer-progress -Dtest=TaskLeasePostgresIntegrationTest test
```

The same-task claim race and competing lease-recovery race each run three times
per suite invocation. These repetitions help catch timing-sensitive regressions;
they do not prove the absence of concurrency defects. Local test reports are
written to `target/surefire-reports/`.

The workflow runs for pull requests and pushes to `main`. Failed test reports are retained as short-lived workflow artifacts when Surefire produces them.

## Remaining milestones

Follow the [execution roadmap](https://github.com/lu4mic/flowforge/issues/12)
for dependency order and work that can proceed in parallel:

- Complete [PostgreSQL concurrency verification](https://github.com/lu4mic/flowforge/issues/2) and [backend hardening](https://github.com/lu4mic/flowforge/issues/3)
- Publish [immutable application images](https://github.com/lu4mic/flowforge/issues/4)
- Decide and integrate a [durable queue](https://github.com/lu4mic/flowforge/issues/5), then [separate coordinator/worker roles](https://github.com/lu4mic/flowforge/issues/6)
- Build cloud-free distributed tests, a repeatable recovery demo and final documentation as tracked in the roadmap

Azure/Terraform work belongs in [flowforge-infra](https://github.com/lu4mic/flowforge-infra).
Cloud provisioning, deployment and teardown are manual milestones, not part of
automatic application CI. No Azure deployment is implemented by this repository's
current CI workflow.

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
