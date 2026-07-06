# Scheduling via the REST API

External systems and scripts can start CWS processes programmatically through
the REST API. This is useful for integrating CWS into pipelines, CI/CD, or
other automation.

## Endpoint

```
POST https://<cws-host>:<ssl-port>/cws-ui/rest/process/<procDefKey>/schedule
```

### Authentication

All CWS REST endpoints require authentication. Include your `cwsToken` header
(see [REST API reference](../../reference/rest-api.md) for how to obtain it),
or use HTTP basic authentication (`-u username:password`) for scripted calls.

### Parameters

Pass process variables as form-encoded key/value pairs in the request body.

## Example (cURL)

Schedule a process named `my_process`, setting a variable `input_file` to
`/data/input.csv`:

```bash
curl -k -X POST \
  "https://<cws-host>:38443/cws-ui/rest/process/my_process/schedule" \
  -H "cwsToken: <your-token>" \
  --data "input_file=/data/input.csv"
```

### Response

```json
{
  "uuid": "254c73fe-c31d-482b-a193-86dd1e5bc9cd",
  "createdTime": "2024-06-15 13:46:50",
  "procDefKey": "my_process",
  "procPriority": 10,
  "procVariables": {
    "procDefKey": "my_process",
    "priority": 10,
    "input_file": "/data/input.csv",
    "uuid": "254c73fe-c31d-482b-a193-86dd1e5bc9cd",
    "procBusinessKey": "254c73fe-c31d-482b-a193-86dd1e5bc9cd"
  },
  "procBusinessKey": "254c73fe-c31d-482b-a193-86dd1e5bc9cd",
  "status": "pending"
}
```

## Monitoring instance status

Use the returned `uuid` to poll the status endpoint:

```
GET https://<cws-host>:<ssl-port>/cws-ui/rest/process-instance/<uuid>/status
```

The `status` field transitions through:

```
pending → inSchedulerQueue → claimedByWorker → running → success
```

If the process fails, `status` will be `fail` and `errorMessage` may contain
details.

### Status response examples

**Pending (not yet started):**

```json
{
  "status": "pending",
  "procDefKey": "my_process",
  "uuid": "07d3311f-ef5e-49cb-af60-426f19f3b12b"
}
```

**Completed successfully:**

```json
{
  "status": "complete",
  "procDefKey": "my_process",
  "procInstId": "5eb4a8fd-f78f-11e5-9041-685b357b8867",
  "uuid": "07d3311f-ef5e-49cb-af60-426f19f3b12b",
  "startTime": "2024-06-15 15:24:44",
  "endTime": "2024-06-15 15:24:44",
  "duration": 808,
  "endActivityId": "EndEvent_1"
}
```

**Failed:**

```json
{
  "status": "fail",
  "procDefKey": "my_process",
  "procInstId": "b46129f8-6eb5-11e4-9b38-10ddb1f141ab",
  "startTime": "2024-06-15 15:59:02",
  "endTime": "2024-06-15 15:59:02",
  "duration": 335,
  "endActivityId": "ServiceTask_1"
}
```

!!! tip
    For additional querying on a completed instance, use the `procInstId` with
    the [Camunda REST API](https://docs.camunda.org/rest/camunda-bpm-platform/7.24/).
