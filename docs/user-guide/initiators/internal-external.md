# Internal & External Initiators

Beyond the built-in initiator types (cron, file, message, repeating delay), CWS
supports **custom initiators** that you develop yourself.

## Internal initiators

An **internal initiator** runs inside the CWS console JVM. It has direct access
to CWS services and can schedule processes without making REST calls.

- **Use when:** your trigger logic needs tight integration with CWS internals
  or must run with low latency.
- **Develop by:** implementing the internal initiator interface and registering
  it with CWS. See
  [Developing Custom Initiators](../../developer/custom-initiators.md).

## External initiators

An **external initiator** runs outside CWS — in a separate process, on a
different host, or as part of another system. It schedules processes by calling
the CWS [REST API](../launching/rest.md).

- **Use when:** the trigger source is an external system (a monitoring tool, a
  CI/CD pipeline, a partner's service) and you don't want to couple it to the
  CWS JVM.
- **Develop by:** writing a script or service that detects the trigger condition
  and `POST`s to the CWS schedule endpoint.

## Choosing between them

| Consideration | Internal | External |
| --- | --- | --- |
| Deployment | Inside CWS (must redeploy CWS to update) | Independent lifecycle |
| Access to CWS internals | Full | REST API only |
| Language | Java | Any (Python, Bash, Go…) |
| Failure isolation | Shares CWS JVM | Isolated process |

For most cases, an external initiator is simpler and more maintainable. Use
an internal initiator only when you need direct access to CWS services that
aren't exposed via REST.
