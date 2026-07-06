# Log & History Management

CWS provides centralized logging through the **Logs** page in the web console, backed by Elasticsearch. History retention is controlled by two configuration properties that govern how long process instance data and log records are kept.

## Logs Page

Navigate to **Logs** in the CWS console sidebar. This page aggregates log messages from the console host and all connected workers in one view.

### Filtering

The Logs page offers several filters to narrow results:

| Filter | Description |
|--------|-------------|
| **Process Definitions** | Show logs for one or more specific process definitions |
| **Log Sources** | Limit to Console logs, Worker logs, or both |
| **Process Instances** | Enter a specific process instance ID to see its full log trail. You can also navigate here from the Processes page with an instance pre-selected. |
| **Log Level** | Show only DEBUG, INFO, WARN, ERROR, or combinations |
| **Search by Keyword** | Free-text search within log message content |
| **Start Date / End Date** | Restrict results to a time range |

### Additional columns

Click the column selector to enable optional columns:

| Column | Shows |
|--------|-------|
| **CWS Host** | IP or hostname of the worker that produced the message |
| **CWS Host ID** | Internal worker ID for the message source |
| **Thread Name** | Thread on which the logged activity ran |
| **Process Definition Key** | The key of the relevant process definition |
| **Instance ID** | The process instance ID for the logged event |

### Useful filter combinations

**Debugging a failed run:** Select the process definition, set Log Level to WARN + ERROR, enable the Log Level column. This surfaces only problematic messages for that definition.

**Tracing a specific instance:** Enter the instance ID in the Process Instances field, select All log levels, enable the Instance ID and Thread Name columns.

**Comparing worker output:** Enable the CWS Host and CWS Host ID columns to see which worker handled which tasks.

## Camunda Cockpit

For live process monitoring (as opposed to log text), use the **Cockpit** page accessible from the top-right navigation. Cockpit provides:

- A list of all deployed definitions with running instance counts
- A graphical BPMN diagram view with instance heatmaps (blue circles showing where tokens are waiting)
- Version history — view instances that are still on older deployed versions
- Filtering by Business Key, start date, and process variables
- Per-instance drill-down: variables, incidents, audit trail, user task assignments

## History Retention Configuration

CWS stores process history in both the database and Elasticsearch. Two properties control how much is kept.

### `history_level`

Set in `cws-configuration.properties` (must be the same value on the console and all workers).

| Level | What is stored |
|-------|---------------|
| `none` | Nothing |
| `activity` | Process instance start/end, activity instances |
| `audit` | Adds variable updates |
| `full` | Adds form properties and user operation log entries |

```properties
history_level=full
```

The default and recommended value for most deployments is `full`. Lowering this reduces database growth but limits what Cockpit can display for completed instances.

Reference: [Camunda 7.24 History](https://docs.camunda.org/manual/7.24/user-guide/process-engine/history/)

### `history_days_to_live`

Controls how many days of history data is retained before automatic cleanup. This applies to:

- Process instance history in the database
- Log entries
- Elasticsearch indices

```properties
history_days_to_live=7
```

CWS runs a scheduled cleanup job that removes data older than this threshold. Increase this value if you need longer audit trails; decrease it to manage storage on high-volume deployments.

!!! note
    Individual process definitions can also set their own `camunda:historyTimeToLive` attribute in the BPMN XML, which overrides the global default for that definition.

### Per-definition history TTL

In the BPMN modeler, set the **History Time To Live** field on the process properties panel. In the XML this appears as:

```xml
<bpmn:process id="my_process" camunda:historyTimeToLive="30">
```

This value is in days. Setting it to `0` disables history retention for that definition.

## Elasticsearch Index Management

CWS writes log messages to Elasticsearch using date-based indices. The index prefix is configured via:

```properties
elasticsearch_index_prefix=cws
```

Indices take the form `<prefix>-YYYY.MM.DD`. Old indices are pruned automatically when their age exceeds `history_days_to_live`.

### Authentication

If your Elasticsearch cluster requires authentication:

```properties
elasticsearch_use_auth=y
elasticsearch_username=your_username
elasticsearch_password=your_password
```

Set `elasticsearch_protocol=https` if your cluster uses TLS.

### Checking index health

Use Kibana or the Elasticsearch REST API to inspect index size and document counts:

```bash
curl http://<es-host>:9200/_cat/indices/cws-*?v
```

## Further reading

- [Camunda 7.24 History](https://docs.camunda.org/manual/7.24/user-guide/process-engine/history/)
- [Camunda 7.24 History Cleanup](https://docs.camunda.org/manual/7.24/user-guide/process-engine/history/history-cleanup/)
