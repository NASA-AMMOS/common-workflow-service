# Core Concepts

A short tour of the building blocks you'll work with in CWS.

## Processes (BPMN)

A **process** is a workflow defined in [BPMN 2.0](https://docs.camunda.org/manual/7.24/reference/bpmn20/)
— a standard graphical notation for modeling business processes. You design a
process as a diagram of tasks, gateways, and events, then deploy it to CWS. A
running copy of a deployed process is a **process instance**.

CWS runs BPMN on top of the Camunda 7 engine, so any BPMN construct Camunda
supports is available, plus CWS's own task types and extensions.

## Tasks

A **task** is a single step in a process. CWS ships several built-in task
types — for example command-line execution, REST calls, email, file
operations, and sleep — and lets you add your own. See
[Built-in Task Types](../developer/task-types.md) and
[Writing Custom Tasks](../developer/custom-tasks.md).

Tasks that CWS executes outside the engine are handled by the **external task**
mechanism (below).

## Workers & external tasks

CWS distributes work using an **external task engine**. Instead of the engine
executing every task in-process, eligible tasks are placed on a queue and
picked up by **workers**.

- A **worker** is a CWS process that fetches and executes external tasks.
- Workers can run as separate JVM processes and on separate machines, letting
  you scale execution horizontally.
- Worker behavior is bounded by configuration such as the maximum number of
  concurrently running process instances per worker.

This separation means the console stays responsive while heavy or long-running
work happens on the workers.

## Initiators

An **initiator** starts process instances automatically in response to some
trigger, so you don't have to launch them by hand. CWS includes initiators such
as:

- **[Cron](../user-guide/initiators/cron.md)** — start on a schedule.
- **[File](../user-guide/initiators/file.md)** — start when a file appears.
- **[Message Arrival](../user-guide/initiators/message-arrival.md)** — start
  when a message is received.
- **[Repeating Delay](../user-guide/initiators/repeating-delay.md)** — start
  repeatedly after a delay.

You can also develop [custom initiators](../developer/custom-initiators.md).

## Adaptations

An **adaptation** tailors CWS for a specific mission or project — custom Java,
custom REST endpoints, custom initiators, and project-specific configuration —
without modifying the core CWS codebase. See
[Adapting CWS for a Mission](../developer/adaptation.md).

## Snippets

**Snippets** are small pieces of reusable code you can invoke from your
processes, making it easy to share logic across workflows. See
[Snippets](../user-guide/snippets.md).

## Logging & history

CWS records auditable logs and process history, backed by **Elasticsearch** for
search and aggregation. How much history is retained, and for how long, is
controlled by the `history_level` and `history_days_to_live`
[configuration settings](../reference/configuration.md#history-retention).
