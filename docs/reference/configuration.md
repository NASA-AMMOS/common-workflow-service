# Configuration Properties

CWS is configured with a properties file passed to the configurator
(`./configure.sh <your-configuration>.properties`). A complete, annotated
starting point ships in the repository at
[`install/example-cws-configuration.properties`](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/install/example-cws-configuration.properties).

Copy that file, fill in the values marked `[YourXXX]`, and run the configurator.
The tables below group the settings by area. Values shown are the example
defaults; adjust them for your environment.

## Host & installation

| Property | Description |
| --- | --- |
| `hostname` | Hostname of the machine you are installing CWS on (Console, Worker, or both). Must be reachable by all other components. |
| `install_type` | `1` = Console and Worker, `2` = Console only, `3` = Worker only. |
| `cws_console_host` | Host running the Console. Only needed when installing a non-console (Worker-only) host. |
| `project_webapp_root` | Optional. Name of a webapp created inside the CWS web server, accessible without CWS security; logging in redirects to its `index.html`. |
| `brand_header` | Text shown in the console header. |

## Database

| Property | Description |
| --- | --- |
| `database_type` | `mariadb` or `mysql`. |
| `database_host` | Database hostname; must be reachable by all Workers and the Console. |
| `database_port` | Database port (default `3306`). |
| `database_name` | Database schema name. |
| `database_username` | User with CRUD access to the schema. |
| `database_password` | Password for the database user. |

## Security & authentication

| Property | Description |
| --- | --- |
| `identity_plugin_type` | Identity backend, e.g. `LDAP`. |
| `cws_ldap_url` / `cws_ldap_url_default` | LDAP(S) URL, e.g. `ldaps://<your-ldap-host>:636`. |
| `ldap_identity_plugin_class` | Camunda LDAP identity provider plugin class. |
| `ldap_security_filter_class` | CWS LDAP security filter class. |
| `camunda_security_filter_class` | CWS Camunda security filter class. |
| `admin_user` | LDAP username of the initial CWS administrator. |
| `admin_firstname`, `admin_lastname`, `admin_email` | Administrator identity (required with Camunda security). |
| `cws_token_expiration_hours` | Hours a CWS security token stays valid before re-authentication (default `24`). |

## Network ports

| Property | Default | Description |
| --- | --- | --- |
| `cws_web_port` | `38080` | HTTP port. |
| `cws_ssl_port` | `38443` | HTTPS port. |
| `cws_ajp_port` | `38009` | AJP port. |
| `cws_shutdown_port` | `38005` | Tomcat shutdown port. |
| `cws_console_ssl_port` | `38443` | Console HTTPS port. |
| `amq_port` | `31616` | Message broker port. |
| `cws_amq_jmx_port` | `37099` | Broker JMX port. |
| `cws_jmx_port` | `31099` | CWS JMX port. |

## Messaging (broker)

| Property | Description |
| --- | --- |
| `amq_host` | Host of the CWS message broker. For a Console, use the same value as `hostname`; for a Worker, use the Console's hostname. |

## Email & notifications

| Property | Description |
| --- | --- |
| `notify_users_email` | `y`/`n` — email users when assigned a task. |
| `email_subject` | Task-assignment email subject template (supports `CWS_*` tokens). |
| `email_body` | Task-assignment email body template. |
| `smtp_hostname` | SMTP server hostname. |
| `smtp_port` | SMTP port (default `25`). |
| `cws_notification_emails` | Comma-separated addresses that receive alerts for major system errors (DB/JMS/auth failures). |

## Elasticsearch

| Property | Description |
| --- | --- |
| `elasticsearch_protocol` | `HTTP` or `HTTPS`. |
| `elasticsearch_host` | Elasticsearch hostname. |
| `elasticsearch_port` | Port (default `9200`). |
| `elasticsearch_index_prefix` | Prefix for CWS indices. |
| `elasticsearch_use_auth` | `y`/`n` — whether the cluster requires authentication. |
| `elasticsearch_username`, `elasticsearch_password` | Credentials when `elasticsearch_use_auth=y`. |
| `user_provided_logstash` | `y`/`n` — if `y`, CWS will not install or start its own Logstash. |

## History & retention

| Property | Description |
| --- | --- |
| `history_level` | Amount of history stored: `none`, `activity`, `audit`, or `full`. Console and all Workers **must** use the same value. See the [Camunda history docs](https://docs.camunda.org/manual/7.24/user-guide/process-engine/history/). |
| `history_days_to_live` | Days to keep history (process instance history, log files, Elasticsearch indices) before automatic purge. High values can consume large disk over time. |

## Workers

| Property | Description |
| --- | --- |
| `worker_max_num_running_procs` | Max actively running process instances per worker (integer ≥ 1; default `16`). Configurable per worker in the `cws_worker` DB table after install. |
| `worker_abandoned_days` | Days before an unseen worker's row is cleaned from the `cws_workers` table (integer ≥ 1). |
| `startup_autoregister_process_defs` | Whether to auto-register process definitions at startup. |

## AWS (optional autoscaling & S3/SQS initiators)

These settings are only relevant when running on AWS with autoscaling or the
S3/SQS initiators. They are optional and disabled by default.

| Property | Description |
| --- | --- |
| `cws_enable_cloud_autoscaling` | `y`/`n` — publish metrics to CloudWatch for autoscaling. Requires a valid CloudWatch endpoint. |
| `aws_cloudwatch_endpoint` | CloudWatch endpoint used when autoscaling is enabled. |
| `metrics_publishing_interval` | Seconds between metric publications (default `10`). |
| `aws_default_region` | Default AWS region (defaults to `us-west-2`). |
| `aws_sqs_dispatcher_sqsUrl` | SQS queue URL for the S3 initiator. |
| `aws_sqs_dispatcher_msgFetchLimit` | SQS fetch limit, `1`–`10` (default `1`). |

!!! tip
    Presets and default values also live in
    [`install/installerPresets.properties`](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/install/installerPresets.properties)
    and `utils.sh`. See [Configuration Reference](../install/configuration.md) in
    the installation guide for how these are applied.
