# Logs Page

The **Logs** page lets you view and filter log messages from the console and all
connected workers in one place.

## Filtering options

| Filter | Purpose |
| --- | --- |
| **Process Definitions** | Show logs for a specific process definition. |
| **Log Sources** | Show only Console or only Worker logs. |
| **Process Instances** | Enter a process instance ID to see its specific messages. (Clicking an instance on the [Processes page](processes.md) sends you here with the ID pre-filled.) |
| **Log Level** | Filter by severity — useful for showing only warnings and errors during debugging. |
| **Search by Keyword** | Free-text search within log messages. |
| **Start Date / End Date** | Limit results to a time range. |

## Additional columns

Beyond the default columns, you can enable:

| Column | Shows |
| --- | --- |
| **CWS Host** | The host IP for each worker log message. |
| **CWS Host ID** | The worker ID for the message. |
| **Thread Name** | The thread on which the logged item ran. |
| **Process Definition Key** | The PD key, when the message relates to a specific definition. |
| **Instance ID** | The process instance ID. |

## Useful configurations

A few examples of productive filter setups:

- Select a target process definition + log levels *Warning* and *Error* +
  enable the *Log Level* column → quickly spot failures within a specific
  workflow.
- Filter by a specific process instance ID → trace the full execution path of
  one run.
- Set a date range + keyword → find a known error that occurred during a
  specific time window.

## Related

- [Log & History Management (admin)](../../administration/logs-history.md) —
  controlling history retention and Elasticsearch index lifecycle.
