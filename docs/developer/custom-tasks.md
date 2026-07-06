# Writing Custom Tasks

CWS lets you create your own task types by extending the `CwsTask` base class
in the `cws-tasks` module.

## Template method pattern

`CwsTask` uses the template method pattern. To create a custom task:

1. Create a new Java class extending `CwsTask`.
2. Override the required lifecycle methods.
3. Register your task with the engine so it appears in the modeler.
4. Build and deploy.

## Minimal example

```java
package jpl.cws.task;

public class MyCustomTask extends CwsTask {

    @Override
    public void initParams() {
        // Read input parameters from the process context
    }

    @Override
    public void executeTask() {
        // Your task logic here
        // Use log.info(...) for output
        // Set output variables on the execution context
    }
}
```

## Input and output variables

- **Inputs** are read from the process execution context in `initParams()`.
- **Outputs** are set back on the context so downstream tasks can use them.
- All variables are scoped by the task ID (see
  [Built-in Task Types](task-types.md) for the scoping convention).

## Building and deploying

1. Add your class to the `cws-tasks` module (or `cws-adaptation` for
   project-specific tasks).
2. Build: `mvn clean package`
3. Deploy the resulting JAR into the CWS console `WEB-INF/lib/` (or rebuild
   the full distribution).

## See also

- [Built-in Task Types](task-types.md) — reference for the shipped tasks.
- [Architecture](architecture.md) — how the task layer fits into CWS.
