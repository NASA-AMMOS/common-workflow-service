# Deploying with Docker

CWS ships with several Docker Compose configurations under `install/docker/`. This guide covers building the image, running the all-in-one stack, and common variations.

## Prerequisites

- Docker and Docker Compose installed
- A valid Camunda EE license file at `~/.camunda/license.txt`
- Recommended Docker resources: 5 CPUs, 14 GB RAM, 1 GB swap, 64 GB disk

## Building the CWS image

The CWS Docker image is based on Oracle Linux 9 and bundles Java 17 and the CWS server package.

```bash
cd install/docker/cws-image
./build.sh
```

`build.sh` will:

1. Run `./build.sh` from the repository root to produce `dist/cws_server.tar.gz`
2. Copy the package into the image build context
3. Build and tag the image as `nasa-ammos/common-workflow-service:2.9.0`

If you have already built the package separately, `build.sh` skips the Maven build and uses the existing artifact.

The resulting image tag follows the pattern `nasa-ammos/common-workflow-service:<version>`.

## All-in-one stack (console + worker + database + Elasticsearch)

The `console-db-es-ls-kibana` compose file starts a full CWS environment on a single host:

```bash
cd install/docker/console-db-es-ls-kibana
docker-compose up
```

This brings up:

| Container | Image | Exposed ports |
|-----------|-------|---------------|
| `cws-db` | `mariadb:10.11` | `3306` |
| `cws-es` | `elasticsearch:8.12.0` | `9200`, `9300` |
| `cws-console` | `nasa-ammos/common-workflow-service:2.9.0` | `38080`, `38443`, `31616` |
| `cws-worker1` | `nasa-ammos/common-workflow-service:2.9.0` | — |
| `ldapsearch` | OpenLDAP (used for authentication) | `389` |

Access the CWS UI at `https://localhost:38443/cws-ui/` (HTTP on `38080` redirects to HTTPS).

To stop the stack:

```bash
docker-compose down
```

### Configuration

The console reads `config.properties` mounted as a read-only volume. Edit `console-db-es-ls-kibana/config.properties` before starting. Key settings:

```properties
hostname=cws-console
install_type=2          # 1=console+worker, 2=console only, 3=worker only
database_host=db
elasticsearch_host=es
elasticsearch_port=9200
cws_console_host=cws-console
amq_host=cws-console
```

The worker reads `worker-config.properties` from the same directory.

## Elasticsearch-only container

Use the `es-only` setup when you need a standalone Elasticsearch instance — for example, to support a non-Docker CWS installation during development.

```bash
cd install/docker/es-only
docker-compose up
```

This starts Elasticsearch 8.12.0 on port `9200` with security disabled (`xpack.security.enabled=false`). Verify it is running:

```bash
curl http://localhost:9200/_cluster/health
```

Then configure CWS to connect:

```properties
elasticsearch_protocol=http
elasticsearch_host=localhost
elasticsearch_port=9200
elasticsearch_use_auth=n
```

## Database-only container

To run only MariaDB in Docker:

```bash
docker run -d \
  -p 3306:3306 \
  -e MYSQL_DATABASE=<your-db-name> \
  -e MYSQL_ROOT_PASSWORD=<your-password> \
  -e TZ=America/Los_Angeles \
  --name cws-db \
  mariadb:10.11
```

Test the connection:

```bash
mysql -h 127.0.0.1 -u root -p
```

Then configure CWS:

```properties
database_type=mariadb
database_host=127.0.0.1
database_name=<your-db-name>
database_username=root
database_password=<your-password>
```

## Adding additional workers

The `worker-ls` compose file adds a standalone worker that joins an existing CWS cluster.

```bash
cd install/docker/worker-ls
# Edit config.properties to point at the console host
docker-compose up
```

The worker compose file uses `cws-network` as an external network, so it must be on the same Docker network as the console stack. The worker's `config.properties` must set:

```properties
install_type=3
cws_console_host=<console-hostname>
amq_host=<console-hostname>
database_host=<db-hostname>
elasticsearch_host=<es-hostname>
```

To scale further, repeat this pattern with unique `hostname` and `container_name` values per worker.

## Volume mounts for certificates

By default the compose files use a self-signed certificate baked into the image. To supply your own certificates, uncomment and update the volume entries in `docker-compose.yml`:

```yaml
volumes:
  - ../../.keystore:/home/cws_user/cws/server/apache-tomcat-11.0.20/conf/.keystore:ro
  - ../../tomcat_lib/cws_truststore.jks:/home/cws_user/cws/server/apache-tomcat-11.0.20/lib/cws_truststore.jks:ro
  - ~/.cws/creds:/root/.cws/creds:ro
```

- `.keystore` must be a PKCS12 or JKS keystore with your server certificate
- `cws_truststore.jks` is the outbound trust store
- `~/.cws/creds` is a single-line file containing the keystore password, with permissions `400`

## Deploying without external database or Elasticsearch

If you already have dedicated database and Elasticsearch services:

1. In `docker-compose.yml`, remove the `db` and/or `es` service blocks.
2. Remove the corresponding `depends_on` entries from the `cws` and `cws-worker` services.
3. Set the `DB_HOST` / `ES_HOST` environment variables to your external service hostnames.
4. Update `config.properties` to match.
