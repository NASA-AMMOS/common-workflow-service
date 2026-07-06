# Initiators

An **initiator** automatically starts process instances in response to a
trigger — no manual intervention required. Configure initiators from the
[Initiators page](../console/initiators.md) in the console.

## Built-in initiator types

| Type | Starts a process when… |
| --- | --- |
| **[Cron](cron.md)** | A cron schedule fires. |
| **[File](file.md)** | A file appears at a watched location. |
| **[Message Arrival](message-arrival.md)** | A message is received on the broker. |
| **[Repeating Delay](repeating-delay.md)** | A configurable delay elapses, then repeats. |

## Custom initiators

CWS supports developing your own initiators:

- **[Internal initiators](internal-external.md)** — run inside the CWS
  console JVM.
- **[External initiators](internal-external.md)** — run outside CWS and
  schedule processes via the REST API.

See [Developing Custom Initiators](../../developer/custom-initiators.md) for
the development guide.
