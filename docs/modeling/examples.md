# BPMN Examples

CWS ships with a set of example process definitions in `install/dev/bpmn/`. These can be deployed to a running CWS instance for testing and as starting points for new processes.

## Available Examples

| File | Description |
|------|-------------|
| `log_core_vars.bpmn` | Logs CWS console host and port using built-in CWS variables |
| `cmd_sleep_n.bpmn` | Runs a command-line sleep task with a configurable duration |
| `simple_sleep_30.bpmn` | Minimal process: start → 30-second sleep → end |
| `external_pwd.bpmn` | External task that runs `pwd` as a command-line task |
| `test_parallel_gateway.bpmn` | Demonstrates fan-out/join with a Parallel Gateway |
| `test_parallel_gateway_sync_tasks.bpmn` | Parallel gateway with synchronized task completion |
| `test_parallel_with_subprocess.bpmn` | Multi-instance sub-process calling a child process definition |
| `test_error_handling.bpmn` | Comprehensive error handling patterns (boundary events, throw/catch) |
| `test_error_end_event.bpmn` | Error End Event that propagates to a parent process |
| `message_passing_example_parent.bpmn` | Parent process using message events to communicate with a child |
| `message_passing_example_child.bpmn` | Child process that receives and sends messages |
| `test_model_outputs.bpmn` | Demonstrates output variable mapping from service tasks |
| `test_out_var.bpmn` | Simple output variable capture from an external task |
| `test_schedule_task.bpmn` | Uses a timer intermediate event for scheduled execution |

## Annotated Examples

### Logging CWS context variables (`log_core_vars.bpmn`)

This is the simplest useful process — it logs the console hostname and port, demonstrating how to use CWS built-in variables:

```xml
<bpmn:process id="log_core_vars" name="Log Core Variables"
              isExecutable="true"
              camunda:historyTimeToLive="30">

  <bpmn:startEvent id="start" camunda:asyncBefore="true">
    <bpmn:outgoing>to_log</bpmn:outgoing>
  </bpmn:startEvent>

  <bpmn:serviceTask id="log_task"
                    camunda:modelerTemplate="jpl.cws.task.LogTask"
                    camunda:class="jpl.cws.task.LogTask">
    <bpmn:extensionElements>
      <camunda:field name="message">
        <camunda:expression><![CDATA[CWS console host = ${cws.hostname}
CWS console port = ${cws.port}]]></camunda:expression>
      </camunda:field>
      <camunda:field name="preCondition">
        <camunda:expression>none</camunda:expression>
      </camunda:field>
      <camunda:field name="onPreConditionFail">
        <camunda:expression>ABORT_PROCESS</camunda:expression>
      </camunda:field>
    </bpmn:extensionElements>
    <bpmn:incoming>to_log</bpmn:incoming>
    <bpmn:outgoing>to_end</bpmn:outgoing>
  </bpmn:serviceTask>

  <bpmn:endEvent id="end">
    <bpmn:incoming>to_end</bpmn:incoming>
  </bpmn:endEvent>
</bpmn:process>
```

Key points:
- `camunda:historyTimeToLive="30"` is set on the process — required by Camunda
- `camunda:asyncBefore="true"` on the Start Event ensures the instance is persisted before execution
- `preCondition` is set to `none` to skip pre-condition evaluation

### Parallel sub-process with call activity (`test_parallel_with_subprocess.bpmn`)

This pattern runs a child process definition N times in parallel:

```xml
<bpmn:subProcess id="parallel_launcher">
  <bpmn:multiInstanceLoopCharacteristics
    camunda:asyncBefore="true"
    camunda:exclusive="false">
    <bpmn:loopCardinality xsi:type="bpmn:tFormalExpression">100</bpmn:loopCardinality>
  </bpmn:multiInstanceLoopCharacteristics>

  <bpmn:startEvent id="sub_start" camunda:asyncBefore="true">
    <bpmn:outgoing>to_child</bpmn:outgoing>
  </bpmn:startEvent>

  <bpmn:callActivity id="child" calledElement="test">
    <bpmn:incoming>to_child</bpmn:incoming>
    <bpmn:outgoing>to_sub_end</bpmn:outgoing>
  </bpmn:callActivity>

  <bpmn:endEvent id="sub_end" camunda:asyncAfter="true">
    <bpmn:incoming>to_sub_end</bpmn:incoming>
  </bpmn:endEvent>
</bpmn:subProcess>
```

The `camunda:exclusive="false"` attribute allows multiple instances to run concurrently on the job executor thread pool.

### Command-line task (`external_pwd.bpmn`)

The `CmdLineExecTask` is a CWS external task that executes a shell command:

```xml
<bpmn:serviceTask id="pwd_task"
                  name="Run pwd"
                  camunda:type="external"
                  camunda:topic="__CWS_CMD_TOPIC__">
  <bpmn:extensionElements>
    <camunda:field name="cmdLine">
      <camunda:expression>pwd</camunda:expression>
    </camunda:field>
    <camunda:field name="workingDir">
      <camunda:expression>.</camunda:expression>
    </camunda:field>
    <camunda:field name="successExitValues">
      <camunda:expression>0</camunda:expression>
    </camunda:field>
    <camunda:field name="throwOnFailures">
      <camunda:expression>true</camunda:expression>
    </camunda:field>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

- `camunda:topic="__CWS_CMD_TOPIC__"` routes the task to the CWS command executor
- `successExitValues` lists exit codes that are considered successful (comma-separated)
- `throwOnFailures=true` causes a non-success exit code to throw an error (and create an incident)

### Error handling (`test_error_handling.bpmn`)

This example demonstrates multiple error-handling scenarios:

- A task that succeeds normally
- A task with `throwOnFailures=false` that continues even on failure
- A task with an **Error Boundary Event** that catches the error and routes to a recovery path
- An **Error End Event** that propagates the error to the parent process

Deploy this process to observe how CWS handles each scenario in the Cockpit and Logs pages.

## Deploying the Examples

To deploy any example to your running CWS instance:

1. Open the CWS web console
2. Navigate to **Processes**
3. Click **Deploy Process**
4. Select the `.bpmn` file from `install/dev/bpmn/`

Some examples (like `test_parallel_with_subprocess.bpmn`) depend on a child process definition (`test.bpmn`) also being deployed. Deploy the child first.