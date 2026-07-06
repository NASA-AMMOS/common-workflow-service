# Elasticsearch Setup

CWS uses **Elasticsearch 8.12.0+** for log and history aggregation. You can
point CWS at any externally-configured cluster, or run the provided Dockerized
Elasticsearch for development.

## Run Elasticsearch in Docker

In a terminal dedicated to Elasticsearch, start the provided compose stack:

```bash
cd install/docker/es-only
docker-compose up -d
```

This is a self-contained way to run Elasticsearch and serves as an alternative
to installing it directly.

## Connecting CWS to Elasticsearch

CWS supports secure (HTTPS, with or without authentication) and insecure (HTTP)
clusters. The relevant [configuration settings](../reference/configuration.md#elasticsearch)
are:

| Setting | Purpose |
| --- | --- |
| `elasticsearch_protocol` | `HTTP` or `HTTPS`. |
| `elasticsearch_host` | Cluster hostname. |
| `elasticsearch_port` | Port (default `9200`). |
| `elasticsearch_index_prefix` | Prefix for CWS indices. |
| `elasticsearch_use_auth` | `y`/`n` — whether the cluster requires auth. |
| `elasticsearch_username` / `elasticsearch_password` | Credentials when auth is enabled. |

## History retention

The amount of history CWS keeps in Elasticsearch (and the database) is governed
by `history_level` and `history_days_to_live`. All Console and Worker hosts
**must** use the same `history_level`. See
[Log & History Management](../administration/logs-history.md).

Next: [Certificates & Keystore](certificates.md).
