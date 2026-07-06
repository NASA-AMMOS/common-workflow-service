# File Initiator

The **File Initiator** starts a process instance when a file appears at a
watched directory location.

## Configuration

| Field | Description |
| --- | --- |
| **Process Definition** | The deployed process to start. |
| **Watch Directory** | The filesystem path to monitor for new files. |
| **File Pattern** | Optional glob or regex to match specific filenames. |
| **Enabled** | Toggle the initiator on/off. |

## How it works

CWS polls the watch directory. When a new file matching the pattern appears, a
process instance is created. The filename (and optionally its full path) is
passed into the process as a variable so the workflow can act on it.

## Use cases

- Processing incoming data files from an external system.
- Triggering a pipeline when a partner drops a file in a shared location.
- Watch a staging directory for new artifacts to validate.

## Notes

- The polling interval depends on CWS internal settings.
- Ensure the watch directory is accessible by the CWS console host.
- For S3-based file triggers, see the S3 Initiator / SQS integration
  (configured via the `aws_sqs_dispatcher_*` settings).
