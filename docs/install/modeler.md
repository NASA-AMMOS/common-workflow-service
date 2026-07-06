# Installing the Modeler

The **CWS Modeler** is a desktop application for designing BPMN process
definitions that are then deployed to CWS.

## Installation

CWS provides install scripts for the modeler:

=== "macOS"

    ```bash
    cd install/modeler
    ./install_mac_modeler.sh
    ```

=== "Linux"

    ```bash
    cd install/modeler
    ./install_linux_modeler.sh
    ```

The script downloads and configures the Camunda Modeler with CWS-specific
element templates (`elements.json`) so the built-in CWS task types appear in
the properties panel.

## Using the modeler

1. Open the modeler application.
2. Create or open a `.bpmn` file.
3. Drag elements onto the canvas — service tasks, gateways, events, etc.
4. For CWS-specific tasks, select the element and choose the CWS task type
   from the properties panel (Command Line Execution, Email, REST GET, etc.).
5. Save the `.bpmn` file and [deploy it](../user-guide/deploying.md) to CWS.

## CWS element templates

The `elements.json` file (in `install/modeler/`) defines the CWS task-type
templates. If you add [custom tasks](../developer/custom-tasks.md), update this
file to make them available in the modeler's UI.

## See also

- [Modeling](../modeling/index.md) — best practices, examples, and tips.
- [Built-in Task Types](../developer/task-types.md)
