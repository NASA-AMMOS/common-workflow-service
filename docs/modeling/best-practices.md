# Modeling Best Practices

These guidelines help you build process models that are maintainable, debuggable, and resilient in production CWS deployments.

## Naming

**Use descriptive, verb-phrase names for tasks.** A task name should describe what it does, not what it is. Prefer `Validate Input File` over `Task1` or `Validation`.

**Use noun phrases for events.** Start events name the trigger (`File Arrived`, `Request Received`) and end events name the outcome (`Process Complete`, `Error Reported`).

**Use consistent prefixes for similar tasks.** If you have multiple command-line tasks, name them consistently: `EXEC: Run Preprocessor`, `EXEC: Run Analysis`, `EXEC: Package Results`. This makes the Logs page easier to scan.

**Keep process definition IDs lowercase with underscores.** The ID is used as a key throughout CWS and in log messages. `my_data_pipeline` is easier to filter on than `MyDataPipeline`.

## Async Continuations

**Set `camunda:asyncBefore="true"` on the Start Event.** This causes Camunda to commit the process instance to the database before any task executes. Without it, a crash during the first task can leave no trace in the database.

**Use async continuations on tasks that run after gateways.** If a gateway splits into branches that run long operations, add `camunda:asyncBefore="true"` to the first task of each branch. This ensures the branch choice is persisted before execution begins.

**Use `camunda:exclusive="false"` on multi-instance sub-processes.** This allows multiple instances to run in parallel on the same job executor thread pool, rather than being serialized.

```xml
<bpmn:multiInstanceLoopCharacteristics
  camunda:asyncBefore="true"
  camunda:exclusive="false">
  <bpmn:loopCardinality>10</bpmn:loopCardinality>
</bpmn:multiInstanceLoopCharacteristics>
```

## Error Handling

**Use Error Boundary Events for recoverable task failures.** Attach a boundary event to a service task to catch errors and route to a recovery or notification path, rather than leaving the instance stuck in an incident state.

**Distinguish incidents from expected errors.** Use Error End Events (with named error codes) for business-level failures that have a defined handling path. Let unexpected exceptions become incidents so the Cockpit alerts you.

**Use a Terminate End Event for fatal conditions.** When an unrecoverable error is detected and the whole process instance should stop, route to a Terminate End Event rather than a plain End Event. Terminate stops all active tokens; plain End only terminates the current path.

**Log at the error boundary.** Add a Log Task (or Script Task with a logging call) on the error boundary path before routing to the error handling flow. This gives you a log message tied to the specific instance when the error occurred.

## Subprocess Design

**Use call activities to decompose large processes.** A process that would have more than about 15 tasks is usually clearer as a parent process with several call activities invoking child process definitions. This also makes testing individual stages easier.

**Use embedded sub-processes to group related tasks.** If several tasks share a common error boundary or transaction scope, wrap them in an embedded sub-process. Boundary events on the sub-process apply to all tasks inside it.

**Keep child processes self-contained.** Child processes called via call activity should work from variables passed in at launch — they should not depend on implicit knowledge of the parent process's variable names.

**Use input/output variable mappings on call activities.** Explicitly map what goes in and comes out:

```xml
<bpmn:callActivity id="run_analysis" calledElement="analysis_process">
  <bpmn:extensionElements>
    <camunda:in source="inputFile" target="inputFile" />
    <camunda:out source="analysisResult" target="analysisResult" />
  </bpmn:extensionElements>
</bpmn:callActivity>
```

This makes data flow explicit and prevents variable namespace collisions.

## Gateway Usage

**Prefer Exclusive Gateways (XOR) for single-path decisions.** Use Exclusive Gateways when exactly one outgoing path should be taken. Always set a default sequence flow to catch unexpected conditions.

**Use Parallel Gateways (AND) for fan-out/join, not XOR.** When multiple paths should all execute, use a Parallel Gateway. The joining Parallel Gateway waits for all branches to complete before proceeding.

**Name gateway conditions.** Label each outgoing sequence flow from an Exclusive Gateway (e.g. `success`, `failure`, `skip`). Anonymous flows make diagrams hard to read.

## Variable Management

**Set variables explicitly in Script Tasks.** Avoid relying on side effects to create variables. Use `execution.setVariable("name", value)` explicitly.

**Prefer process-scope variables over local variables for shared state.** Local variables are visible only within the current scope (sub-process, call activity). If downstream tasks need a value, set it on the execution scope that encompasses those tasks.

**Initialize variables at the start of a process.** If a later task depends on a variable that might not exist (e.g. an optional initiator variable), initialize it to a safe default in the first Script Task.

## History and Cleanup

**Always set `camunda:historyTimeToLive`.** Camunda requires this on every process definition. Set it to a value that matches your operational retention needs (e.g. `30` days for 30 days of history).

```xml
<bpmn:process id="my_process" camunda:historyTimeToLive="30">
```

**Remove completed instances from Elasticsearch.** CWS's `history_days_to_live` property governs automatic cleanup of both database history and Elasticsearch log indices.

## Further reading

- [Camunda 7.24 BPMN Reference](https://docs.camunda.org/manual/7.24/reference/bpmn20/)
- [Camunda 7.24 Best Practices](https://docs.camunda.org/manual/7.24/user-guide/process-engine/transactions-in-processes/)
