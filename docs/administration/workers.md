# Worker Management

A CWS worker is a component capable of executing process instances using the BPMN 2.0 engine. You must have at least one worker running to execute processes. Large deployments can run many workers across multiple hosts.

## Viewing Workers

Navigate to the **Workers** tab in the CWS web console. The page lists all registered workers and their current status.

For each worker you can see:

- Worker ID and host
- Current status (active / inactive)
- **Configuration** — expand to see all settings this worker was started with
- **Process Definitions** — expand to see which process definitions this worker handles

## Worker Configuration

Expanding the **Configuration** column for a worker reveals its startup settings. The most important tunable from the UI is the number of **executor threads**.

### Executor threads

Executor threads control how many process instances a worker can execute concurrently. Increasing this value allows more parallel work but consumes more memory and CPU on the worker host.

To change the executor thread count for a running worker, expand its configuration and update the value. The change takes effect without restarting the worker.

!!! tip
    Start conservatively (e.g. 4–8 threads) and increase based on observed CPU and memory utilization. Overcommitting threads on a memory-constrained host will degrade overall throughput.

## Process Definitions per Worker

Expanding the **Process Definitions** section for a worker shows every process definition registered to that worker. From this view you can:

### Enable / Disable a process definition

Toggle a process definition on or off for a given worker. Disabling a definition on a worker prevents that worker from picking up new instances of that definition. Instances already in progress will complete normally.

This is useful for:
- Draining a worker before maintenance
- Routing specific process definitions to specific worker hosts
- Temporarily pausing execution of a definition without undeploying it

### Set a concurrency limit

The **Limit** field controls the maximum number of instances of a specific process definition that can run concurrently on this worker. This is separate from the overall executor thread count.

For example, if a process definition is resource-intensive (heavy disk I/O or network), set its limit to a small value to prevent it from monopolizing the worker's threads.

## Worker Types

CWS workers can be started in different modes depending on what work they should perform:

| Mode | Description |
|------|-------------|
| `run_all` | Handles all process types (default) |
| `run_models_only` | Executes only BPMN service tasks running in-process |
| `run_external_tasks_only` | Polls for and executes external tasks only |

The mode is set at worker startup via the `worker_type` configuration property.

## Adding a New Worker

Workers are registered automatically when started. To add a worker:

1. Provision a host with Java 17 and the CWS distribution
2. Create a configuration file pointing to the shared database and Elasticsearch cluster
3. Start the worker with `./dev.sh` or your deployment script

The worker will appear in the Workers tab once it connects to the CWS console and registers itself.

## Removing a Worker

Stopping a worker process removes it from active rotation. The worker record remains visible in the UI until its registration expires. There is no explicit delete action required.

Before stopping a worker, disable all its process definitions (see above) and wait for any in-progress instances to complete to avoid abrupt termination.

## Further reading

- [Camunda 7.24 External Tasks](https://docs.camunda.org/manual/7.24/user-guide/process-engine/external-tasks/)
- [CWS Configuration Reference](../reference/configuration.md)
