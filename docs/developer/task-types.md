# Built-in Task Types

CWS ships several built-in task types you can drop into a process without
writing any code. To use one, drag a task box onto the modeler canvas, make it
a **Service Task**, then select the CWS task type from the element type list.

!!! note "Task-scoped variables"
    Variables produced by a task are **scoped by the ID of the task that
    generates them**. For example, an `exitValue` produced by a Command Line
    task with the ID `Task_1ydalhn` is stored as `Task_1ydalhn_exitValue`. This
    lets multiple tasks of the same type appear in one process without their
    output variables colliding.

## The built-in tasks

| Task type | Description |
| --- | --- |
| **Command Line Execution** | Invoke an executable program, as if launched from a command-line prompt, on an enabled worker. |
| **Log Message** | Write a message to the log file. |
| **Send Email** | Send an email. |
| **Sleep** | Pause for a specified amount of time. |
| **REST GET** | Make a REST `GET` call to a URL with the specified parameters. |
| **REST POST** | Make a REST `POST` call to a URL with the specified parameters. |
| **Set Variables** | Set process variables from a properties file. |
| **Move File** | Move a file from one location to another. |
| **Schedule Process** | Schedule another process for execution. |

You can also write your own — see [Writing Custom Tasks](custom-tasks.md).

## Command Line Execution

Runs an executable program on a worker that is enabled for the process
definition.

!!! warning "Executables only — not shell commands"
    The Command Line task invokes **executables**, not shell scripting
    constructs. It uses
    [Apache Commons Exec](https://commons.apache.org/proper/commons-exec/)
    behind the scenes, so only platform-independent invocations are allowed —
    **not** environment variables, `if`/`then`/`else`, redirection, piping, or
    other OS/shell-specific constructs.

    Allowed:

    ```bash
    echo "hello world"
    ```

    Not allowed (uses shell redirection):

    ```bash
    echo "hello world" >> my_output_file.txt
    ```

    To reference an environment variable, use the CWS expression form rather
    than a shell variable:

    ```bash
    echo "my home directory is ${cws.getEnv("HOME")}"    # allowed
    echo "my home directory is $HOME"                    # NOT allowed
    ```

Executables must be on the `PATH`; otherwise specify the full absolute path to
the executable.

### Options

| Option | Purpose |
| --- | --- |
| **Command** | The command to execute. |
| **Working Directory** | Current working directory for the command (e.g. `/home/user`). |
| **Success Value(s)** | Comma-separated exit values considered a success (e.g. `0,4,10`). |
| **throwOnFailures** | If `true`, a non-success exit throws a catchable `BpmnError` (which a [boundary error event](https://docs.camunda.org/manual/7.24/reference/bpmn20/events/error-events/) can catch). |
| **Exit Event Map** | Key/value pairs mapping exit codes to named events (e.g. `0=success,1=fail`). |
| **throwOnTruncateVariable** | If `true`, throw a `BpmnError` when the output variable is truncated for length. Default `false`. |
| **timeout** | Seconds to allow the command to run before timing out. |
| **retries** / **retryDelay** | Number of retries on timeout, and the delay (ms) before retrying. |
| **Pre-condition** | An expression that must evaluate to `true` for the task to proceed. |
| **onPreConditionFail** | Behavior when the pre-condition fails. |

### Input and output variables

The task reads its inputs from an `in` JSON object (populated from the modeler
options: `command`, `workingDir`, `successfulValues`, `exitCodeEvents`,
`throwOnFailures`, `throwOnTruncatedVariable`, `timeout`, `retries`,
`retryDelay`) and writes results to an `out` JSON object:

| `out` field | Meaning |
| --- | --- |
| `exitCode` | Exit code returned by the program. |
| `success` | Whether `exitCode` is one of the success values. |
| `event` | The event mapped from `exitCode` via the exit event map. |
| `stdout` | Standard output. |
| `stderr` | Standard error. |
| `lockedTime` | Start time of the command. |

Access an output field in a model expression with the task ID and a JSON path,
for example:

```
Task_id_out.jsonPath("$.exitCode").numberValue() != 5
Task_id_out.jsonPath("$.success").boolValue() == true
Task_id_out.jsonPath("$.stdout").stringValue() == "my text output"
```

### Command Line Execution (short-lived / blocking)

A blocking variant of the Command Line task, for short-lived work. It differs
from the standard task in that it:

- **Blocks** the process thread instead of releasing it for other work.
- Is guaranteed to run on the **same worker** as the previous activity (unless
  you set an async flag).
- Does **not** have the `timeout` / `retry` settings of the standard task.

!!! warning
    Because it blocks, it is bound by the **process engine** timeout and retry
    settings — by default a 5-minute timeout with up to 3 immediate retries.
    Only use it for executions that reliably finish within that window;
    retries on tasks with side effects can cause errors.

## See also

- [Script Task Recipes](../modeling/script-recipes.md) — parsing JSON/REST
  responses, setting variables, and logging from script tasks.
- [Writing Custom Tasks](custom-tasks.md) — implement your own task by
  extending `CwsTask`.