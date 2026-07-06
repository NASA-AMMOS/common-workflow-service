# Building from Source

CWS is built with Maven. For development, the project convention is a small
**personal build script** that sets your environment values and calls
`dev.sh`, which builds CWS and starts the console and workers.

## A personal build script

Create a script such as `dev-yourname.sh` in the repository root. The template
below works for local development. Set `ES_PROTOCOL` (`HTTP` or `HTTPS`) and
`ES_HOST` to match your Elasticsearch:

```bash
#!/bin/bash
# File: dev-yourname.sh

HOSTNAME=localhost

# Used in cws-test
echo "$HOSTNAME" > cws-test/src/test/resources/hostname.txt

SECURITY="camunda"

# Stop CWS if it is currently running
./stop_dev.sh

# DB config
DB_TYPE=mariadb
DB_HOST=127.0.0.1
DB_NAME=cws_dev   # must match the database you created beforehand
DB_USER=root      # must match the user you created beforehand
DB_PASS=          # can also be supplied via environment variables
DB_PORT=3306      # mariadb default

USER=             # your username
CLOUD=            # enable cloudwatch monitoring (leave blank to disable)

EMAIL_LIST="{email}"

ADMIN_FIRST="{first}"
ADMIN_LAST="{last}"
ADMIN_EMAIL="{email}"

# Elasticsearch config
ES_PROTOCOL="HTTP"   # 'HTTP' or 'HTTPS'
ES_HOST="localhost"
ES_PORT=9200
ES_USE_AUTH=n
ES_USERNAME="na"
ES_PASSWORD="na"

# Number of workers to start (1 is the minimum)
NUM_WORKERS=1

# Max concurrently running process instances per worker (default 16, min 1)
WORKER_MAX_NUM_RUNNING_PROCS=16

# Days until abandoned workers are cleaned from the cws_workers table
WORKER_ABANDONED_DAYS=1

# Run the dev script
./dev.sh `pwd` ${USER} ${DB_TYPE} ${DB_HOST} ${DB_PORT} ${DB_NAME} ${DB_USER} ${DB_PASS} ${ES_PROTOCOL} ${ES_HOST} ${ES_PORT} ${ES_USE_AUTH} ${ES_USERNAME} ${ES_PASSWORD} ${CLOUD} ${SECURITY} ${HOSTNAME} ${EMAIL_LIST} ${ADMIN_FIRST} ${ADMIN_LAST} ${ADMIN_EMAIL} ${NUM_WORKERS} ${WORKER_MAX_NUM_RUNNING_PROCS} ${WORKER_ABANDONED_DAYS}
```

Run it from the repository root:

```bash
./dev-yourname.sh
```

The script builds CWS, verifies your configuration, and starts the console and
workers. When everything is up, it prints a link to the console dashboard.

## Full (non-dev) build

To produce a server distribution without the dev workflow:

```bash
./build.sh
```

This cleans the module library directories, runs
`mvn -DskipTests -Dskip.integration.tests clean install -P core`, and creates
the server distribution via `create_server_dist.sh`.

## Checking dependencies for security vulnerabilities

```bash
mvn dependency-check:check
# or
mvn dependency-check:aggregate
```

## Running tests

```bash
./test.sh
```

This runs the unit and integration tests and produces JaCoCo code-coverage
reports. To run a single test, use Maven directly:

```bash
mvn test -Dtest=ClassName#methodName
mvn integration-test -Dit.test=IntegrationTestClass
```

Next: [Configuration Reference](configuration.md).