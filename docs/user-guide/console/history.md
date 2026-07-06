# History Page

The **History** page provides a searchable view of completed and ended process
instances. Use it to review past workflow executions, check outcomes, and audit
process history.

The history page draws from the same Elasticsearch-backed data store as the
[Logs page](logs.md), but is oriented around process instances rather than
individual log lines.

## What you'll see

- Process definition name and version.
- Instance ID and business key.
- Start and end time.
- Final status (completed, failed, etc.).

## Retention

How long history is retained is controlled by the `history_days_to_live`
[configuration property](../../reference/configuration.md#history-retention).
Older data is automatically purged to manage disk usage.

See [Log & History Management](../../administration/logs-history.md) for
details on retention policies.
