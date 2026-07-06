# Deploying Processes

Before you can launch a process, you must **deploy** the BPMN process
definition to CWS.

## How to deploy

1. Open the [Deployments page](console/deployments.md) in the CWS console.
2. Click the **Deploy** button.
3. Upload your `.bpmn` file (created in the CWS modeler or any compatible BPMN
   editor).
4. CWS validates and registers the process definition.

Once deployed, the process appears in the Deployments table and is available
for [launching](launching/index.md) manually, via the REST API, or through an
[initiator](initiators/index.md).

## Enabling workers

After deployment, you need to enable at least one worker for the process before
instances can execute:

1. On the Deployments page, expand the process entry.
2. Use the worker toggle to enable one or more workers.

You can control per-worker limits from the [Workers page](console/workers.md).

## Redeploying / updating a process

Deploying a new version of an existing process definition creates a new
version while preserving the previous one. Running instances continue on the
version they were started with; new instances use the latest version.

## Auto-registering processes at startup

If `startup_autoregister_process_defs` is set to `true` in your
[configuration](../install/configuration.md), processes placed in the
configured BPMN directory are deployed automatically when CWS starts.
