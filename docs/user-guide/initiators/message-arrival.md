# Message Arrival Initiator

The **Message Arrival Initiator** starts a process instance when a message is
received on the CWS message broker (Apache Artemis).

## Configuration

| Field | Description |
| --- | --- |
| **Process Definition** | The deployed process to start. |
| **Queue / Topic** | The broker destination to listen on. |
| **Enabled** | Toggle on/off. |

## How it works

CWS listens on the configured broker queue. When a message arrives, it creates
a process instance and passes the message content (or selected fields) as
process variables.

## Use cases

- Event-driven workflows triggered by an upstream system posting to the broker.
- Decoupled architectures where producers don't call the CWS REST API directly.
- Fanout patterns where one message triggers one or more workflows.

## Notes

- The message broker must be reachable at the `amq_host`:`amq_port` configured
  during installation.
- Messages that fail to start a process are dead-lettered per broker
  configuration.
