# DMN Examples

CWS supports DMN (Decision Model and Notation) decision tables, executed by the Camunda DMN engine. Decision tables externalize conditional logic from BPMN, making rules easier to read and maintain without modifying the process diagram.

## What is a Decision Table?

A DMN decision table maps input conditions to output values. Each row is a rule: if all input conditions in the row match, the corresponding outputs are returned.

This is useful for:
- Routing decisions based on multiple conditions (e.g., priority level + data type → processing queue)
- Deriving configuration values from input parameters
- Replacing nested exclusive gateways with a more readable rules table

## Creating a Decision Table

Use the Camunda Modeler to create `.dmn` files. Deploy them to CWS the same way as BPMN files.

1. Open Camunda Modeler
2. File > New File > DMN Table
3. Define input and output columns
4. Add rules (rows)
5. Save as `my_decision.dmn`

## Hit Policies

The hit policy determines what happens when multiple rules match:

| Hit Policy | Behavior |
|------------|----------|
| `UNIQUE` | Exactly one rule may match; error if multiple match |
| `FIRST` | Returns the first matching rule (top to bottom) |
| `ANY` | Multiple rules may match, all must produce the same output |
| `COLLECT` | Returns all matching outputs as a list |

For most routing decisions, `FIRST` is the most intuitive choice.

## Calling a Decision Table from BPMN

Use a **Business Rule Task** in your BPMN diagram to invoke a DMN decision:

```xml
<bpmn:businessRuleTask id="route_decision"
                       name="Determine Processing Route"
                       camunda:decisionRef="processing_route_decision"
                       camunda:resultVariable="routingResult"
                       camunda:mapDecisionResult="singleEntry">
  <bpmn:incoming>from_start</bpmn:incoming>
  <bpmn:outgoing>to_gateway</bpmn:outgoing>
</bpmn:businessRuleTask>
```

| Attribute | Purpose |
|-----------|---------|
| `camunda:decisionRef` | The ID of the deployed DMN decision definition |
| `camunda:resultVariable` | Process variable name to store the result |
| `camunda:mapDecisionResult` | How the result is mapped: `singleEntry`, `singleResult`, `collectEntries`, `resultList` |

Use `singleEntry` when the table returns a single output column and one row. Use `resultList` when multiple rows may match (`COLLECT` hit policy).

## Example: Priority-Based Routing

This decision table maps a `priority` input to a `queue` output:

| Input: `priority` | Output: `queue` |
|-------------------|----------------|
| `"high"` | `"urgent"` |
| `"medium"` | `"standard"` |
| `"low"` | `"batch"` |
| _(any other)_ | `"standard"` |

In the BPMN, after the Business Rule Task:

```
${routingResult == "urgent"}  →  high-priority handler
${routingResult == "standard"}  →  normal handler
${routingResult == "batch"}  →  batch handler
```

## Example: Multi-Input Decision

A decision table with two inputs — `fileSize` (in MB) and `processingMode` — determines `workerThreads`:

| Input: `fileSize` | Input: `processingMode` | Output: `workerThreads` |
|-------------------|------------------------|------------------------|
| `< 100` | `"fast"` | `4` |
| `< 100` | `"safe"` | `2` |
| `>= 100` | `"fast"` | `8` |
| `>= 100` | `"safe"` | `4` |

The DMN engine evaluates both conditions for each row. Use FEEL (Friendly Enough Expression Language) syntax for input expressions:

| FEEL expression | Meaning |
|-----------------|---------|
| `< 100` | Less than 100 |
| `>= 100` | Greater than or equal to 100 |
| `"fast"` | String equality |
| `"fast", "express"` | String in set |
| _(blank)_ | Always matches (wildcard) |

## DMN in the BPMN XML

A full Business Rule Task with DMN call:

```xml
<bpmn:businessRuleTask
    id="calculate_threads"
    name="Calculate Worker Threads"
    camunda:decisionRef="worker_thread_decision"
    camunda:decisionRefBinding="latest"
    camunda:resultVariable="workerThreads"
    camunda:mapDecisionResult="singleEntry">
  <bpmn:incoming>from_init</bpmn:incoming>
  <bpmn:outgoing>to_process</bpmn:outgoing>
</bpmn:businessRuleTask>
```

`camunda:decisionRefBinding="latest"` always uses the most recently deployed version of the decision (recommended for most cases).

## Accessing Results

After the Business Rule Task, the result is available as a process variable:

```
// singleEntry — result is a scalar value
${workerThreads}

// collectEntries — result is a list
${routeList[0]}
```

In a Script Task following the Business Rule Task:

```groovy
// Access a singleEntry result
def threads = execution.getVariable("workerThreads")
execution.setVariable("maxThreads", threads)
```

## Testing Decision Tables

The Camunda Modeler has a built-in test runner for DMN files. Open the `.dmn` file, click **Decision Table**, and use the test cases panel to provide sample inputs and verify expected outputs before deploying.

## Further reading

- [Camunda 7.24 DMN Reference](https://docs.camunda.org/manual/7.24/reference/dmn/)
- [Camunda 7.24 Business Rule Task](https://docs.camunda.org/manual/7.24/reference/bpmn20/tasks/business-rule-task/)
- [FEEL Language Reference](https://docs.camunda.org/manual/7.24/reference/dmn/feel/)