# Deploying on AWS

This page describes a general architecture for running CWS on AWS. It covers the required infrastructure components, auto-scaling workers, and CloudWatch integration. All account-specific values (VPC IDs, AMI IDs, subnet IDs, etc.) are your own to supply.

## Required AWS components

| Component | AWS service | Notes |
|-----------|-------------|-------|
| Networking | VPC + security groups | Console, database, and ES must be able to reach each other |
| Console instance | EC2 | Runs CWS with `install_type=2` |
| Worker instances | EC2 | Run CWS with `install_type=3`; scale independently |
| Database | RDS (MariaDB/MySQL) or EC2 | One instance shared by all CWS nodes |
| Elasticsearch | Self-managed EC2 or Amazon OpenSearch | One cluster shared by all CWS nodes |
| IAM | Role + instance profile | Required for CloudWatch metrics and optional S3 access |

## Security group rules

At minimum:

- Console → database: port `3306`
- Console → Elasticsearch: port `9200`
- Workers → console: ports `38443` (HTTPS), `31616` (ActiveMQ)
- Workers → database: port `3306`
- Workers → Elasticsearch: port `9200`
- Inbound to console from users: ports `38080` (HTTP redirect) and `38443` (HTTPS)

## IAM permissions

The EC2 instance profile for CWS nodes needs:

- `cloudwatch:PutMetricData` — required for the `queueMaxPendingDuration` metric used by auto-scaling
- `s3:GetObject` / `s3:PutObject` on your CWS S3 bucket — if using S3 storage for process artifacts

## Infrastructure as code

CWS includes Terraform scripts under the repository for automating the EC2 setup. Refer to the scripts in `install/` for the canonical resource definitions. Adapt them to your VPC layout, AMI IDs, and naming conventions.

## Auto-scaling workers

CWS publishes a CloudWatch custom metric — `queueMaxPendingDuration` — that represents the age of the oldest pending item in the worker queue. You can drive an EC2 Auto Scaling Group off this metric to scale workers up when work accumulates and down when the queue is idle.

### Enable auto-scaling on the console

Add to the console's `config.properties`:

```properties
cws_enable_cloud_autoscaling=y
```

### Enable auto-registration on workers

Workers that join the cluster through auto-scaling must register their process definitions automatically:

```properties
startup_autoregister_process_defs=true
```

### CloudWatch alarms

Create two alarms on the `queueMaxPendingDuration` metric:

**Scale-up alarm** — triggers when pending duration exceeds your threshold (e.g., > 300 seconds for 2 evaluation periods). Associate it with a scale-out policy on the Auto Scaling Group.

**Scale-down alarm** — triggers when pending duration drops below a lower threshold (e.g., ≤ 30 seconds for 5 evaluation periods). Associate it with a scale-in policy.

### Launch template

Create an EC2 Launch Template for worker nodes. In the **User data** field, pass the CWS worker configuration as URL-encoded key=value pairs, for example:

```
INSTALL_TYPE=worker&DB_HOST=<your-db-host>&DB_USER=<db-user>&DB_PASS=<db-pass>&CWS_CONSOLE_HOST=<console-host>
```

Attach the Launch Template to an IAM instance profile that has the CloudWatch and S3 permissions described above.

### Auto Scaling Group

Create an EC2 Auto Scaling Group using the Launch Template above. Attach two **automatic scaling policies** — one for each CloudWatch alarm.

!!! note "Manual process definition enablement"
    When a new auto-scaled worker joins the cluster, you may need to manually enable process definitions on it through the CWS console. The `startup_autoregister_process_defs=true` setting reduces this, but verify that new workers pick up definitions as expected in your environment.

## Using S3 for storage

CWS can read and write files to S3. Enable it by adding the S3 bucket name and region to `config.properties` and ensuring the IAM role attached to CWS EC2 instances has the appropriate S3 permissions. See `install/example-cws-configuration.properties` for the full list of S3-related properties.

## CloudWatch log aggregation

For centralized log collection from multiple CWS nodes, configure the CloudWatch Logs agent (or the AWS-provided unified agent) on each instance to ship the Tomcat and CWS log directories. The default log path inside a CWS installation is:

```
<install-dir>/server/apache-tomcat-11.0.20/logs/
```
