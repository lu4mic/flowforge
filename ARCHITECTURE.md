# Architecture

## Overview

The engine separates **workflow definitions** from **workflow executions**.

```text
DEFINITION                  EXECUTION

Workflow                    WorkflowRun
   |                            |
WorkflowTask                 TaskRun
```

`WorkflowDependency` connects task definitions:

```text
prerequisiteTask -> dependentTask
```

The graph is kept acyclic, so workflows remain valid DAGs.

## Core models

### Workflow

Reusable workflow definition.

```text
id
name
description
version
createdAt
updatedAt
```

### WorkflowTask

A step inside a workflow.

```text
id
key
name
type
workflow
delayDurationMs
httpMethod
httpUrl
```

Current task types:

```text
DELAY
HTTP
```

### WorkflowDependency

Represents one edge in the DAG.

```text
prerequisiteTask
dependentTask
```

The engine currently rejects:

- self-dependencies
- cross-workflow dependencies
- duplicate edges
- cycles

### WorkflowRun

One execution of a workflow definition.

```text
id
workflow
workflowVersion
status
createdAt
startedAt
completedAt
```

### TaskRun

One execution of one task inside one `WorkflowRun`.

```text
id
workflowRun
workflowTask
status
createdAt
readyAt
startedAt
completedAt
```

Current lifecycle:

```text
PENDING -> READY -> RUNNING -> SUCCEEDED
                          \-> FAILED
```

## Scheduling logic

When a workflow run is created:

```text
create WorkflowRun
      |
      v
create one TaskRun per WorkflowTask
      |
      v
tasks with no prerequisites -> READY
```

When a task succeeds:

```text
completed TaskRun
      |
      v
find dependent tasks
      |
      v
check all prerequisite TaskRuns
      |
      v
all SUCCEEDED?
      |
     yes
      |
      v
dependent TaskRun -> READY
```

All dependency checks are scoped to the same `WorkflowRun`.

## Transaction strategy

Database state changes are transactional.

Example:

```text
RUNNING -> SUCCEEDED
+
unlock downstream tasks
```

External work should not hold a database transaction open.

For example, DELAY execution is split into:

```text
transaction:
READY -> RUNNING
commit

Thread.sleep(...)

transaction:
RUNNING -> SUCCEEDED
unlock dependents
commit
```

The same pattern will be used for HTTP execution.

## Current services

```text
WorkflowService
WorkflowTaskService
WorkflowDependencyService
WorkflowRunService
TaskRunService
TaskExecutionService
```

Responsibilities are intentionally separated:

- Controllers: HTTP
- Services: application logic
- Repositories: persistence
- Entities: domain state and transitions
- TaskExecutionService: external/blocking work

## Current state

Implemented:

```text
Workflow definition
-> DAG validation
-> WorkflowRun
-> TaskRun
-> initial READY detection
-> DELAY execution
-> downstream task unlocking
-> HTTP execution preparation
```

Current next step:

```text
HTTP TaskRun
-> real HTTP request
-> status handling
```

## Planned distributed direction

Long term:

```text
API / Scheduler
      |
      v
PostgreSQL  <- source of truth
      |
      v
Azure Queue
      |
      v
Workers
```

The future design will assume **at-least-once delivery** and add retries, task attempts, leases, idempotency, and crash recovery.
