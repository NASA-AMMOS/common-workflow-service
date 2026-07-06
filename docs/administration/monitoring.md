# Resource Monitoring

CWS deployments can optionally be monitored with [Grafana](https://grafana.com/) and [Prometheus](https://prometheus.io/). This page walks through the setup and explains how to build dashboards for CWS infrastructure.

## Architecture Overview

```
                       ┌─────────────────┐
  ┌──────────────┐     │  Console Host   │
  │ Worker Hosts │────▶│   Prometheus    │◀─── Grafana (port 3000)
  │ node_exporter│     │   (port 9090)   │
  │ (port 9100)  │     └────────┬────────┘
  └──────────────┘              │
  ┌──────────────┐              │ scrapes
  │ Database Host│◀─────────────┘
  │ node_exporter│
  │ (port 9100)  │
  │mysqld_exporter│
  │ (port 9104)  │
  └──────────────┘
```

Prometheus scrapes `node_exporter` (system metrics) from every host, and `mysqld_exporter` (database metrics) from the database host. Grafana queries Prometheus and renders dashboards.

## Requirements

- SSH access to each worker machine, the database host, and the console host
- Network connectivity: the Prometheus host must reach each worker on port 9100 and the database host on ports 9100 and 9104
- Grafana must reach Prometheus on port 9090
- A read-only MariaDB/MySQL user for `mysqld_exporter`

## Setting up Prometheus Exporters

### Database host

Two exporters are needed: `node_exporter` for system metrics and `mysqld_exporter` for database metrics.

First create a read-only database user:

```sql
CREATE USER 'mysqld_exporter'@'localhost'
  IDENTIFIED BY 'StrongPassword'
  WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'mysqld_exporter'@'localhost';
```

Then run this setup script on the database host (adjust paths and versions as needed):

```bash
#!/bin/bash
mkdir -p ~/prometheus/exporters
cd ~/prometheus/exporters

# mysqld_exporter
wget -q https://github.com/prometheus/mysqld_exporter/releases/download/v0.12.0/mysqld_exporter-0.12.0.linux-amd64.tar.gz
tar zxf mysqld_exporter-0.12.0.linux-amd64.tar.gz
cd mysqld_exporter-0.12.0.linux-amd64

cat > .my.cnf <<EOF
[client]
user=mysqld_exporter
password=YOUR_PASSWORD_HERE
EOF

nohup ./mysqld_exporter --config.my-cnf=".my.cnf" > mysqld_exporter.log 2>&1 &

cd ~/prometheus/exporters

# node_exporter
wget -q https://github.com/prometheus/node_exporter/releases/download/v0.18.1/node_exporter-0.18.1.linux-amd64.tar.gz
tar zxf node_exporter-0.18.1.linux-amd64.tar.gz
cd node_exporter-0.18.1.linux-amd64
nohup ./node_exporter > node_exporter.log 2>&1 &
```

Replace `YOUR_PASSWORD_HERE` with the password you set above.

### Worker hosts

Run this script on each worker you want to monitor:

```bash
#!/bin/bash
mkdir -p ~/prometheus/exporters
cd ~/prometheus/exporters

wget -q https://github.com/prometheus/node_exporter/releases/download/v0.18.1/node_exporter-0.18.1.linux-amd64.tar.gz
tar zxf node_exporter-0.18.1.linux-amd64.tar.gz
cd node_exporter-0.18.1.linux-amd64
nohup ./node_exporter > node_exporter.log 2>&1 &
```

## Setting up Prometheus

Install Prometheus on the console host (or any host that can reach all exporters).

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 5s
  evaluation_interval: 5s

scrape_configs:
  - job_name: node
    static_configs:
      - labels:
          alias: cws
        targets:
          - "db-host:9100"
          - "worker1-host:9100"
          - "worker2-host:9100"

  - job_name: mysql
    static_configs:
      - labels:
          alias: cws
        targets:
          - "db-host:9104"
```

Replace the target hostnames with your actual hosts.

Then start Prometheus:

```bash
#!/bin/bash
mkdir -p ~/prometheus
cd ~/prometheus

wget -q https://github.com/prometheus/prometheus/releases/download/v2.11.1/prometheus-2.11.1.linux-amd64.tar.gz
tar zxf prometheus-2.11.1.linux-amd64.tar.gz
cd prometheus-2.11.1.linux-amd64

nohup ./prometheus --config.file=/path/to/prometheus.yml > prometheus.log 2>&1 &
```

Verify targets are up at `http://<console-host>:9090/targets`.

### Troubleshooting exporters

If a target shows as DOWN in Prometheus:

```bash
# Check if node_exporter is running
ps -ax | grep node_exporter

# Check if mysqld_exporter is running
ps -ax | grep mysqld_exporter
```

A missing process means the exporter failed to start. Check the log file in its directory for errors.

## Setting up Grafana

Install Grafana on the same host as Prometheus (simplest setup):

```bash
#!/bin/bash
mkdir -p ~/grafana
cd ~/grafana

wget -q https://dl.grafana.com/oss/release/grafana-6.2.5.linux-amd64.tar.gz
tar -zxf grafana-6.2.5.linux-amd64.tar.gz
cd grafana-6.2.5.linux-amd64

nohup ./bin/grafana-server web > grafana.log 2>&1 &
```

Grafana is now available at `http://localhost:3000/`. The default credentials are `admin` / `admin` — you will be prompted to change the password on first login.

For newer Grafana releases see the [official download page](https://grafana.com/grafana/download).

## Configuring Dashboards

### Add data sources

1. Open Grafana and go to **Configuration > Data Sources**
2. Add a **Prometheus** data source pointing to `http://localhost:9090`
3. Optionally add a **MySQL** data source pointed at your CWS database — this lets you query CWS tables directly in dashboards

### Recommended dashboards

**System metrics (node_exporter):**
Import dashboard ID `1860` (Node Exporter Full) from the [Grafana dashboard library](https://grafana.com/grafana/dashboards/1860).

**MySQL metrics:**
Install the Percona plugin and enable it:

```bash
# Run from Grafana install directory
./bin/grafana-cli plugins install percona-percona-app
```

Once enabled, Percona provides the **InnoDB Overview** and **MySQL Overview** dashboards. These require the MySQL data source to be named `CWS MySQL Database`.

### Reverse proxy (optional)

To serve Grafana under a path prefix (e.g. `/grafana`) rather than its own port, configure the `root_url` in `grafana.ini`:

```ini
[server]
root_url = %(protocol)s://%(domain)s:%(http_port)s/grafana/
serve_from_sub_path = true
```

Then configure your web server or load balancer to proxy `/grafana` to port 3000.

## Further reading

- [Prometheus documentation](https://prometheus.io/docs/)
- [Grafana documentation](https://grafana.com/docs/)
- [node_exporter releases](https://github.com/prometheus/node_exporter/releases)
- [mysqld_exporter releases](https://github.com/prometheus/mysqld_exporter/releases)
