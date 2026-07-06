# Initiators Page

The **Initiators** page in the console lets you create, configure, and manage
automatic triggers for your process definitions.

An **initiator** starts process instances automatically when a condition is met,
without manual intervention. From this page you can:

- View all configured initiators and their status (active/inactive).
- Create new initiators for any deployed process definition.
- Edit initiator parameters (schedule, file path, etc.).
- Enable or disable initiators.

## Initiator types

CWS ships several built-in initiator types:

| Type | Trigger |
| --- | --- |
| [Cron](../initiators/cron.md) | A cron-like schedule. |
| [File](../initiators/file.md) | A file appears at a watched location. |
| [Message Arrival](../initiators/message-arrival.md) | A message is received. |
| [Repeating Delay](../initiators/repeating-delay.md) | Repeatedly, after a configurable delay. |

Custom initiators (internal or external) can also be developed — see
[Custom Initiators](../../developer/custom-initiators.md).

## Creating an initiator

1. Click **Create Initiator**.
2. Select the process definition to trigger.
3. Choose an initiator type and fill in the type-specific parameters.
4. Save and enable.

The initiator runs in the background; once active, it starts instances
automatically according to its configuration.
