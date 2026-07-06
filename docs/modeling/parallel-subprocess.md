# Parallel Tasks & Sub-Processes

Running tasks in parallel is one of the most common modeling needs in CWS. This page covers the two main approaches: Parallel Gateways for a fixed set of branches, and Multi-Instance Sub-Processes for a dynamic number of parallel executions.

## Parallel Gateway (fixed branches)

Use a Parallel Gateway when you have a known, fixed number of branches that should all execute simultaneously.

```
Start → [task A] → ╔══╗ → [branch 1] ─┐
                   ║AND║               ├→ ╔══╗ → [join task] → End
                   ╚══╝ → [branch 2] ─┘   ╚══╝
```

The opening Parallel Gateway fans out to all branches. The closing Parallel Gateway waits for all branches to complete before the flow continues.

**Key points:**

- All outgoing paths from a Parallel Gateway are taken unconditionally — no conditions on sequence flows
- The joining Parallel Gateway blocks until every incoming branch arrives
- If any branch fails, the instance goes to an incident state; fix and retry

**BPMN example (`test_parallel_gateway.bpmn`):**

The `test_parallel_gateway.bpmn` example in `install/dev/bpmn/` demonstrates this pattern. It fans out from a Parallel Gateway into several concurrent paths and rejoins at a second Parallel Gateway before the End Event.

## Multi-Instance Sub-Process (dynamic parallelism)

Use a Multi-Instance Sub-Process when you need to run the same sub-process body N times in parallel, where N may be determined at runtime.

This is the pattern used by `test_parallel_with_subprocess.bpmn` in `install/dev/bpmn/`.

### Fixed cardinality

To run exactly N parallel iterations, set the **Loop Cardinality** in the sub-process multi-instance properties:

```xml
<bpmn:subProcess id="parallel_work">
  <bpmn:multiInstanceLoopCharacteristics
    camunda:asyncBefore="true"
    camunda:exclusive="false">
    <bpmn:loopCardinality xsi:type="bpmn:tFormalExpression">100</bpmn:loopCardinality>
  </bpmn:multiInstanceLoopCharacteristics>
  <!-- sub-process body -->
</bpmn:subProcess>
```

Setting `camunda:exclusive="false"` is critical — it allows multiple instances to be acquired by the job executor pool simultaneously rather than being serialized.

### Collection-based iteration

To iterate over a list variable (e.g. a list of file names), use the collection and element variable properties:

```xml
<bpmn:multiInstanceLoopCharacteristics
  camunda:asyncBefore="true"
  camunda:exclusive="false"
  camunda:collection="${fileList}"
  camunda:elementVariable="currentFile">
</bpmn:multiInstanceLoopCharacteristics>
```

Inside the sub-process, `${currentFile}` holds the value for the current iteration.

### Calling a child process from each iteration

A common pattern is to have the sub-process body contain a Call Activity that invokes a separate child process definition for each item. This keeps the child process definition simple and testable independently:

```xml
<bpmn:subProcess id="parallel_launcher">
  <bpmn:multiInstanceLoopCharacteristics
    camunda:asyncBefore="true"
    camunda:exclusive="false">
    <bpmn:loopCardinality xsi:type="bpmn:tFormalExpression">${itemCount}</bpmn:loopCardinality>
  </bpmn:multiInstanceLoopCharacteristics>

  <bpmn:startEvent id="sub_start" camunda:asyncBefore="true">
    <bpmn:outgoing>to_child</bpmn:outgoing>
  </bpmn:startEvent>

  <bpmn:callActivity id="child_call" calledElement="child_process">
    <bpmn:incoming>to_child</bpmn:incoming>
    <bpmn:outgoing>to_sub_end</bpmn:outgoing>
  </bpmn:callActivity>

  <bpmn:endEvent id="sub_end" camunda:asyncAfter="true">
    <bpmn:incoming>to_sub_end</bpmn:incoming>
  </bpmn:endEvent>
</bpmn:subProcess>
```

Setting `camunda:asyncAfter="true"` on the sub-process End Event gives the job executor a checkpoint after each iteration completes.

## Worker Concurrency Considerations

Parallel execution depends on having sufficient executor threads. If you launch 50 parallel sub-processes but the worker has only 4 executor threads, at most 4 will run simultaneously.

Match your parallelism to your worker configuration:

- Set the worker's executor thread count high enough to allow meaningful parallelism
- Use the **Limit** field on the Workers page to cap how many instances of the parent process run at once
- Each parallel branch uses one thread for the duration of the task; short tasks can share threads more efficiently than long-running ones

## Collecting Results from Parallel Branches

To aggregate output from parallel iterations, use a collection output variable:

```xml
<bpmn:multiInstanceLoopCharacteristics
  camunda:collection="${inputList}"
  camunda:elementVariable="item"
  camunda:outputElement="${itemResult}"
  camunda:outputCollection="results">
</bpmn:multiInstanceLoopCharacteristics>
```

After all iterations complete, `${results}` contains the list of collected output values.

## Error Handling in Parallel Branches

By default, an error in one parallel branch creates an incident for that instance but does not affect other branches. The joining gateway will not complete until all branches either finish or are resolved.

To cancel all remaining branches when one fails, use an **Error Boundary Event** on the sub-process itself with a Terminate End Event downstream. This cancels the entire sub-process scope, including all active parallel instances.

## Further reading

- [Camunda 7.24 Multi-Instance](https://docs.camunda.org/manual/7.24/reference/bpmn20/tasks/task-markers/#multi-instance)
- [Camunda 7.24 Parallel Gateway](https://docs.camunda.org/manual/7.24/reference/bpmn20/gateways/parallel-gateway/)
- [Camunda 7.24 Sub-Processes](https://docs.camunda.org/manual/7.24/reference/bpmn20/subprocesses/)