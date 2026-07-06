# Repeating Delay Initiator

The **Repeating Delay Initiator** starts a process instance repeatedly, waiting
a fixed delay between launches.

## Configuration

| Field | Description |
| --- | --- |
| **Process Definition** | The deployed process to start. |
| **Delay** | Time to wait between launches (e.g. seconds or minutes). |
| **Enabled** | Toggle on/off. |

## How it works

After the initiator is enabled, CWS waits the configured delay, then launches
an instance. Once that instance is scheduled (not necessarily completed), CWS
waits the delay again and repeats.

## Difference from Cron

- **Cron** fires at absolute wall-clock times regardless of the previous
  instance's state.
- **Repeating Delay** measures the interval *from the end of the last launch*,
  so it naturally spaces executions even if the process duration varies.

## Use cases

- Polling an external system at a regular interval.
- Producing a heartbeat or watchdog process.
- Steady-state load generation for testing.
