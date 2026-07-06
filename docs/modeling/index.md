# Modeling

CWS uses BPMN 2.0 as its process definition language, executed by the Camunda 7.24 engine. This section covers how to design, organize, and deploy process models that run reliably in CWS.

## Topics

| Page | What it covers |
|------|---------------|
| [Best Practices](best-practices.md) | Naming conventions, error handling, async continuations, subprocess design |
| [Tips & Tricks](tips.md) | Practical modeler tips for common CWS patterns |
| [Parallel Sub-Processes](parallel-subprocess.md) | Running tasks in parallel using multi-instance sub-processes |
| [BPMN Examples](examples.md) | Annotated examples from the CWS example library |
| [DMN Examples](dmn.md) | Decision table examples for routing and rule evaluation |
| [Script Task Recipes](script-recipes.md) | Groovy and JavaScript snippets for common scripting needs |

## Tooling

Models are created with the **Camunda Modeler** desktop application, which produces `.bpmn` and `.dmn` files. Deploying a model to CWS is done through the CWS web console (Processes page > Deploy) or via the REST API.

Download the Camunda Modeler from [camunda.com/download/modeler/](https://camunda.com/download/modeler/).

## CWS-specific conventions

- Every process definition must have `camunda:historyTimeToLive` set (in days) to comply with Camunda's history cleanup requirements. Use the process properties panel or set it in the XML: `camunda:historyTimeToLive="30"`.
- The Start Event should have `camunda:asyncBefore="true"` to ensure the process instance is committed to the database before execution begins.
- External tasks use the topic `__CWS_CMD_TOPIC__` for command-line execution tasks.
