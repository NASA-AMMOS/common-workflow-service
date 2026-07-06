# Modeling Tips & Tricks

Practical tips for working with the Camunda Modeler and deploying processes in CWS.

## Modeler shortcuts

| Action | Shortcut |
|--------|----------|
| Open properties panel | Click element, then `F4` or the wrench icon |
| Quick-create connected task | Click element, hover edge, click the task icon |
| Align selected elements | Select multiple, right-click > Align |
| Toggle XML editor | Click the code button in the toolbar |
| Validate diagram | Click the checkmark button; errors show in the bottom panel |

## Setting the asyncBefore flag

Select the Start Event or any task, open the properties panel, go to the
**General** tab, and check **Asynchronous Before** under the job configuration
section.

This should be set on the Start Event of every process deployed to CWS so that
the process engine creates a save point before execution begins.

## Using CWS built-in variables in expressions

CWS exposes several variables accessible in BPMN expression language using the
dollar-brace syntax:

- `cws.hostname` — Hostname of the CWS console
- `cws.port` — Port of the CWS console

These are useful in Log Task messages, for example:
`Console is at` followed by `cws.hostname` and `cws.port` references.

## Calling code snippets from BPMN

CWS snippets are Java methods in the `CustomMethods` class, callable via
dollar-brace expressions (e.g. `cws.methodName(arg1, arg2)`) in any expression
field — sequence flow conditions, task parameters, log messages, etc.

Changes to snippets take effect immediately — no redeployment needed.

## Using pre-conditions on tasks

CWS service tasks support a `preCondition` field that is evaluated before the
task runs. If the condition evaluates to false, the `onPreConditionFail` action
is taken instead of executing the task.

| `onPreConditionFail` value | Behavior |
|---------------------------|----------|
| `SKIP_TASK` | Skip this task and continue the flow |
| `ABORT_PROCESS` | Terminate the process instance |

Set `preCondition` to `none` to disable this check (default behavior).

## Deploying a process

From the CWS console:

1. Go to the **Deployments** page.
2. Click **Deploy Process**.
3. Select your `.bpmn` file.
4. Confirm the deployment.

On success, the new version appears in the process list. Existing running
instances continue on their current version.

## Inspecting a running instance in Cockpit

1. Click **Cockpit** in the console navigation.
2. Select the process definition.
3. Find the instance in the list (filter by business key if needed).
4. Click the instance ID to open the detail view.

The detail view shows:

- Current token positions on the BPMN diagram (blue circles with count).
- All process variables with their current values.
- Any active incidents with the error message and stack trace.
- The full audit trail of completed activities.

## Testing with Camunda Tasklist

You can manually start a process instance from the **Tasklist** page. This is
useful for testing a process end-to-end with specific variables.

!!! note
    Instances started via Tasklist are not recorded in the CWS Processes tab.
    Use this only for testing; production triggering should go through CWS
    initiators or the REST API.

## Viewing process variables mid-run

In Cockpit, select a running instance, then look at the **Variables** panel.
You can see all current variable values and their scopes. This is the fastest
way to check why a gateway took an unexpected path.

## Incident resolution

When a task fails with an unhandled exception, Camunda creates an **Incident**.
Incidents appear in Cockpit as red markers on the diagram. To resolve:

1. Fix the root cause (repair a file, fix a script, update a variable).
2. In Cockpit, click the incident and choose **Retry**.
3. The task will execute again from the beginning.

If you need to update a variable before retrying, use the Variables panel to
edit it, then retry the incident.

## Cleaning up stuck instances

If a process instance is stuck and cannot be recovered, cancel it from Cockpit:

1. Select the instance.
2. Click **Cancel Process Instance**.

This terminates the instance and marks it as cancelled in the history. Any
resources held by external tasks will be released when the worker's lock timeout
expires.
