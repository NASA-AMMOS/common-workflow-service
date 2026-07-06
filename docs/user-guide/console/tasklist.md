# Camunda Tasklist

The **Tasklist** page is Camunda's built-in interface for **user tasks** — manual
steps in a process that require human interaction before the workflow can
continue.

## How user tasks work

When a process reaches a user task, execution pauses until a person claims and
completes it. The Tasklist shows all tasks assigned to you (or unassigned tasks
you can claim).

For each task you'll see:

- Task name.
- The process instance it belongs to.
- Assignee (or unassigned).
- Due date and creation date.
- Priority.

## Completing a task

1. Open the Tasklist and find your assigned task (or claim an unassigned one).
2. Review any form or instructions attached to the task.
3. Fill in required information or confirm completion.
4. Submit — the process continues from where it paused.

## When to use user tasks

User tasks are useful for:

- **Approval gates** — a human must approve before the process continues.
- **Data entry** — collect information that can't be automated.
- **Review steps** — confirm outputs before downstream processing.

For more on modeling user tasks, see [Modeling](../../modeling/index.md) and the
[Camunda User Task reference](https://docs.camunda.org/manual/7.24/reference/bpmn20/tasks/user-task/).
