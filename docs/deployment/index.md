# Deployment

CWS can be deployed in several configurations depending on your infrastructure and scale requirements. All deployments require three core services to be accessible:

- **Database** — MariaDB or MySQL (one instance per cluster)
- **Elasticsearch** — version 8.12.0+ (one instance per cluster)
- **CWS** — one or more instances (console, workers, or combined)

## Deployment options

| Option | Description |
|--------|-------------|
| [Docker](docker.md) | All-in-one stack or split services using Docker Compose — recommended for development and smaller deployments |
| [AWS](aws.md) | EC2-based deployment with optional auto-scaling workers |

## Console vs. worker roles

A CWS installation runs in one of three modes, set by `install_type` in `config.properties`:

| `install_type` | Role |
|----------------|------|
| `1` | Console **and** Worker (default) |
| `2` | Console only |
| `3` | Worker only |

For production workloads, run the console as `install_type=2` and scale workers separately as `install_type=3`. Workers connect back to the console via the ActiveMQ broker (`amq_host`).

## TLS certificates

CWS requires two certificate files at startup:

- `install/.keystore` — Tomcat SSL keystore
- `install/tomcat_lib/cws_truststore.jks` — trust store for outbound TLS

The keystore password must be stored in `~/.cws/creds` with permissions `400`. See the [Docker deployment guide](docker.md#volume-mounts-for-certificates) for how to mount these files into containers.

## Upgrading

See the [Upgrade & Migration guide](upgrade.md) for procedures to move between CWS versions, including database schema migrations and Elasticsearch data migration.
