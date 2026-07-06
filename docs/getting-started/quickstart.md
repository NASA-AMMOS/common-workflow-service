# Quickstart with Docker

The fastest way to try CWS is the single-machine Docker stack, which runs the
database, Elasticsearch, the CWS console, and a worker together on one host.

!!! warning "For evaluation and development"
    This all-in-one stack is meant for trying CWS and local development. For a
    real deployment, see the [Installation guide](../install/index.md).

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) with at least **4 CPUs and
  10 GB memory** allocated (Docker → Settings → Resources).
- A local clone of the
  [CWS repository](https://github.com/NASA-AMMOS/common-workflow-service).

## Steps

1. **Build the CWS Docker image.** From the repository, run the `build.sh`
   script in the CWS image directory:

    ```bash
    cd install/docker/cws-image
    ./build.sh
    ```

    Update the version in `build.sh` if needed.

2. **Provide a keystore password file.** CWS uses SSL certificates that need a
   password at startup. Create the credentials file and restrict its
   permissions:

    ```bash
    mkdir -p ~/.cws
    echo "changeit" > ~/.cws/creds   # the default password for the bundled self-signed certs
    chmod 700 ~/.cws
    chmod 400 ~/.cws/creds
    ```

    See [Certificates & Keystore](../install/certificates.md) to use your own
    certificates.

3. **Create the shared Docker network** so additional workers can join:

    ```bash
    docker network create cws-network
    ```

4. **Review configuration.** Adjust `config.properties` and
   `docker-compose.yml` in `install/docker/console-db-es-ls-kibana/` if you
   need non-default settings.

5. **Start the stack:**

    ```bash
    cd install/docker/console-db-es-ls-kibana
    docker-compose up
    ```

    This brings up the database, Elasticsearch, the CWS console, and one
    worker.

6. **Open the console.** Once startup completes, browse to the CWS console over
   HTTPS on the configured console port and log in.

## Adding more workers

Each additional worker needs about **4 GB more memory**. To add a second worker
to this deployment:

```bash
cd ../worker-ls
docker-compose up
```

For further workers, copy the `worker-ls` directory, adjust its
`config.properties` and `docker-compose.yml`, and run `docker-compose up` in
each new directory.

## Next steps

- [Deploy a process definition](../user-guide/deploying.md)
- [Launch and schedule processes](../user-guide/launching/index.md)
- [Tour the web console](../user-guide/console/index.md)
