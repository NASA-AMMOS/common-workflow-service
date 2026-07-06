# Getting Started

New to CWS? This section gets you from zero to a running instance and explains
the concepts you'll use every day.

<div class="grid cards" markdown>

-   :material-rocket-launch-outline: __[Quickstart with Docker](quickstart.md)__

    Stand up a full CWS stack — console, worker, database, and Elasticsearch —
    on a single machine with Docker.

-   :material-lightbulb-on-outline: __[Core Concepts](concepts.md)__

    Understand processes, workers, external tasks, initiators, and adaptations
    and how they fit together.

-   :material-timeline-clock-outline: __[Process Instance Lifecycle](lifecycle.md)__

    Follow a process instance from launch through completion.

</div>

## What is CWS?

CWS wraps the [Camunda 7 BPMN engine](https://camunda.com/products/camunda-platform/bpmn-engine/)
with the pieces you need to run workflows in production:

- A **web console** for deploying, launching, monitoring, and troubleshooting
  processes.
- An **external task engine** that distributes work to one or more **workers**,
  which can run as separate JVM processes or on separate machines.
- **Initiators** that start processes automatically — on a schedule, when a
  file arrives, when a message is received, and more.
- **Adaptation layers** and **code snippets** for tailoring CWS to a specific
  mission or project without forking the codebase.
- **Auditable logging** with Elasticsearch-backed history.

## Which path is right for me?

| I want to… | Start here |
| --- | --- |
| Try CWS quickly on one machine | [Quickstart with Docker](quickstart.md) |
| Install CWS for real (dev or production) | [Installation](../install/index.md) |
| Learn the moving parts first | [Core Concepts](concepts.md) |
| Understand the codebase / extend CWS | [Architecture](../developer/architecture.md) |
