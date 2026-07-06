# Administration

CWS administration covers the day-to-day tasks needed to keep a deployment healthy: managing users and security, configuring workers, reviewing logs, and monitoring system resources.

## Topics

| Page | What it covers |
|------|---------------|
| [Security & Roles](security.md) | LDAP integration, authentication schemes, CWS token auth, security filters |
| [User Administration](users.md) | Adding users, groups, and Camunda permissions |
| [Worker Management](workers.md) | Viewing workers, executor threads, enabling process definitions per worker |
| [Log & History Management](logs-history.md) | Log page usage, filtering, history retention, Elasticsearch index management |
| [Resource Monitoring](monitoring.md) | Grafana and Prometheus setup for CWS infrastructure monitoring |

## Quick reference

**Accessing admin pages**

The CWS web console provides direct links to the Camunda Cockpit, TaskList, and Admin pages in the top-right navigation bar. These are Camunda's own management UIs, seamlessly integrated into the CWS interface.

**Roles overview**

CWS uses Camunda's built-in authorization model. Users belong to groups; groups receive authorizations on applications, process definitions, and tasks. LDAP mode syncs users and groups from your directory service automatically.

**Default admin credential**

In Camunda auth mode, the default password is `changeme`. Change it immediately after installation.
