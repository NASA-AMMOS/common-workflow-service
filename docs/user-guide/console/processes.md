# Processes Page

The **Processes** page shows individual process instances and their statuses.
Use it to monitor, troubleshoot, and take action on running or completed
instances.

## What you'll see

For each process instance the page shows its status, the worker it ran on, and
its timestamp. Selecting an instance takes you to the
[Logs page](logs.md) with a pre-set filter for that instance, letting you see
all relevant log output without sifting through the entire log.

## Filtering

Use the filter controls to narrow the view:

- **Status** — select a status radio (Pending, Running, Completed, Failed,
  Incident, etc.) and click **Filter** to show only matching instances.
- **Process Definition** — limit to a specific deployment.

This is especially useful for debugging: filtering by *Failed* shows you
exactly which instances failed, on which worker, and when — then clicking
through to the logs gives you the full context.

## Process actions

Depending on the instance status, you can perform bulk actions via the
**Actions** dropdown:

| Instance status | Action | Description |
| --- | --- | --- |
| Pending | Disable selected rows | Changes status to *disabled*; prevents workers from processing them. |
| Disabled | Enable selected rows | Changes status back to *pending*; allows workers to process them. |
| Incident | Retry all selected | Retries execution from the last successful [commit point](https://docs.camunda.org/manual/7.24/user-guide/process-engine/transactions-in-processes/#asynchronous-continuations). |
| Failed to Start | Retry all selected | Changes status to *pending*, enabling workers to pick them up. |
| Failed | Mark as resolved | Acknowledges the failure; the instance is counted as *completed* in statistics and displays green on the deployments page. |

!!! warning "Same-type selection required"
    All selected rows must have the **same status** for an action to be
    available. If you select rows of mixed status, all actions are disabled.
