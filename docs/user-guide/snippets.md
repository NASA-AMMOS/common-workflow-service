# Snippets

**Snippets** are small, reusable pieces of code that you can inject into your
process definitions from the CWS console.

## What snippets are for

- Share common logic across multiple processes without duplicating it in each
  BPMN model.
- Centrally manage utility functions (string processing, date formatting,
  variable transformations) that many workflows use.
- Change shared behavior in one place — all processes using the snippet pick up
  the update on their next execution.

## Managing snippets

From the **Snippets** tab in the CWS console:

- **View** — see all available snippets and their code.
- **Create** — write a new snippet with a name and body.
- **Edit** — update an existing snippet's code.
- **Delete** — remove a snippet that is no longer needed.

## Using a snippet in a process

Reference a snippet by name in a script task or expression within your BPMN
model. The CWS engine resolves the snippet at execution time and runs its code
in the script context.

## Best practices

- Keep snippets focused — one snippet, one responsibility.
- Name snippets clearly so their purpose is obvious in the modeler.
- Test snippets with a simple process before relying on them in production
  workflows.
