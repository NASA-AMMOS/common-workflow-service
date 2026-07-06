# Deployments Page

The **Deployments** page is the starting point for managing your process
definitions. From here you can:

- View all deployed process definitions.
- Deploy new BPMN process definitions (created in the modeler).
- Enable or disable workers for specific processes.
- View instance statistics for each deployment.

## Instance statistics

The **Instance Statistics** column shows a color-coded bar representing the
status distribution of all instances for each process:

| Color | Meaning |
| --- | --- |
| :material-circle:{ style="color: green" } Green | Completed successfully (or marked *resolved*). |
| :material-circle:{ style="color: red" } Red | Failed to complete. |
| :material-circle:{ style="color: gold" } Yellow | Pending — waiting for an available worker or executor threads. |
| :material-circle:{ style="color: orange" } Orange | Failed to start. |
| :material-circle:{ style="color: blue" } Blue | Currently running. |
| :material-circle:{ style="color: hotpink" } Pink | Raised one or more incidents. |

Click on a colored section to jump to the [Processes page](processes.md) filtered
to that status for the selected deployment.

!!! note "Incidents and subprocesses"
    Incidents are reported for the **parent** instance. A single pink entry may
    represent multiple subprocess-level failures. Click through to the Camunda
    cockpit to inspect individual incidents.

## Deploying a new process

See [Deploying Processes](../deploying.md) for the full workflow.
