# Cron Initiator

The **Cron Initiator** starts a process instance on a cron-like schedule.

## Configuration

| Field | Description |
| --- | --- |
| **Process Definition** | The deployed process to start. |
| **Cron Expression** | A standard cron expression defining the schedule (e.g. `0 0/5 * * * ?` = every 5 minutes). |
| **Enabled** | Toggle the initiator on/off without deleting it. |

## How it works

CWS evaluates the cron expression at startup and schedules a timer. Each time
the timer fires, a new process instance is created and queued for a worker.

## Example

To run a cleanup process every night at midnight:

```
0 0 0 * * ?
```

## Notes

- The cron expression uses the [Quartz cron syntax](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
  (6–7 fields: seconds minutes hours day-of-month month day-of-week [year]).
- If the process takes longer than the interval, instances can overlap. Use
  executor thread limits on the [Workers page](../console/workers.md) to
  control concurrency.
