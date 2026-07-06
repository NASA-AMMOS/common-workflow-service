# Scheduling from Another Process

A running process can schedule another process as part of its own flow using
the built-in **Schedule Process** task type.

## How it works

1. In your BPMN model, add a Service Task of type **Schedule Process** (see
   [Built-in Task Types](../../developer/task-types.md)).
2. Configure it with the **process definition key** of the process you want to
   start.
3. Optionally pass variables from the parent process to the child.
4. At runtime, when the parent reaches this task, CWS schedules the target
   process for execution on an enabled worker.

## Use cases

- **Orchestration** — a parent workflow coordinates multiple child workflows.
- **Chained processing** — output from one process triggers the next step in a
  pipeline.
- **Fan-out** — dynamically start multiple instances based on loop data.

## Relationship to the parent instance

The child process runs as a separate, independent instance. It does **not**
share a transaction with the parent. If you need tighter coupling (shared scope,
error propagation), consider using a
[Call Activity](https://docs.camunda.org/manual/7.24/reference/bpmn20/subprocesses/call-activity/)
instead.
