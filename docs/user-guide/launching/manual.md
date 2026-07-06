# Launching Processes Manually

The simplest way to run a deployed process is to launch it manually from the
CWS console.

## Steps

1. Open the [Deployments page](../console/deployments.md).
2. Find your process definition in the list.
3. Click the **Launch** (play) button for the process.
4. Optionally set process variables or a business key in the launch dialog.
5. Confirm — an instance is created with status *pending* and picked up by an
   enabled worker.

Track the instance on the [Processes page](../console/processes.md) or via the
[Logs page](../console/logs.md).

## When to use manual launch

- Testing a process during development.
- Running a one-off workflow that doesn't need automation.
- Ad-hoc execution where no initiator is configured.

For repeated or event-driven execution, use an
[Initiator](../initiators/index.md) or the
[REST API](rest.md) instead.
