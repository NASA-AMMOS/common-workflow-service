# Workers Page

The **Workers** page displays information about all workers connected to your
console.

## Worker details

A **worker** is a CWS component that executes process instances using the BPMN
engine. You need at least one worker to execute processes, but can scale
horizontally with as many as needed.

For each worker you'll see:

- **Status** — whether the worker is active and connected.
- **Configuration** — expand to view the worker's settings, including the
  number of **executor threads** (how many concurrent process instances the
  worker can run simultaneously). You can adjust this value here.

## Process definitions per worker

Under **Process Definitions**, expand to see all processes the worker is
configured to handle. From here you can:

- **Enable / disable** specific process definitions on a per-worker basis.
- Set a **Limit** — the maximum number of threads that should run concurrently
  for a particular process on this worker.

This lets you control how work is distributed across your worker fleet: you can
dedicate specific workers to specific processes, throttle expensive processes,
or ensure critical workflows get priority.

See also: [Worker Management (admin)](../../administration/workers.md).
