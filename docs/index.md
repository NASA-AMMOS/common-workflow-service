# Common Workflow Service

The **Common Workflow Service (CWS)** is an open-source, enterprise workflow
management platform from [NASA-AMMOS](https://ammos.nasa.gov/). It is built on
top of the [Camunda BPMN workflow engine](https://camunda.com/products/camunda-platform/bpmn-engine/)
and extends it with an intuitive web console, auditable logging, a powerful
external-task engine, custom process initiators, code snippets, and a
mission-adaptation layer.

This site is the home for the CWS **user guide**, **installation guide**,
**administration** and **adaptation** documentation, and pointers to the
generated **API and code reference**.

!!! tip "New to CWS?"
    Start with the [Quickstart](getting-started/quickstart.md) to stand up CWS
    with Docker, then read the [Core Concepts](getting-started/concepts.md) to
    learn how processes, workers, initiators, and adaptations fit together.

## Explore the documentation

<div class="grid cards" markdown>

-   :material-rocket-launch-outline: __Getting Started__

    ---

    Stand up CWS quickly and learn the core concepts behind BPMN processes,
    workers, and initiators.

    [:octicons-arrow-right-24: Get started](getting-started/index.md)

-   :material-download-outline: __Installation__

    ---

    Requirements, prerequisites, database and Elasticsearch setup, building
    from source, and configuration.

    [:octicons-arrow-right-24: Install CWS](install/index.md)

-   :material-monitor-dashboard: __User Guide__

    ---

    Tour the web console, deploy process definitions, and launch, schedule,
    and monitor your workflows.

    [:octicons-arrow-right-24: Use CWS](user-guide/index.md)

-   :material-sitemap-outline: __Modeling__

    ---

    Design BPMN processes with best practices, examples, and reusable script
    task recipes.

    [:octicons-arrow-right-24: Model workflows](modeling/index.md)

-   :material-shield-account-outline: __Administration__

    ---

    Manage users and security, workers, logging, and history retention.

    [:octicons-arrow-right-24: Administer CWS](administration/index.md)

-   :material-code-braces: __Developer & Adaptation__

    ---

    Architecture, adapting CWS for a mission, custom tasks and initiators, and
    contributing.

    [:octicons-arrow-right-24: Extend CWS](developer/index.md)

</div>

## API & code reference

- **[REST API](reference/rest-api.md)** — the CWS console serves interactive
  Swagger UI for its REST endpoints.
- **[Javadoc](reference/javadoc.md)** — generated API documentation for the
  CWS Java modules.
- **[Configuration properties](reference/configuration.md)** — every
  configurable CWS setting.

## About this documentation

This documentation is versioned alongside CWS releases. The version you are
reading is shown in the selector at the top of the page. The current release is
**CWS <<< cws_version >>>**.

CWS is released under the [Apache License 2.0](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/LICENSE).
Found a problem or a gap? Please
[open an issue](https://github.com/NASA-AMMOS/common-workflow-service/issues/new/choose).
