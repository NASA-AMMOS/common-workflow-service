# Docker Setup

### Quickly run common-workflow-service on a single machine with all required services running on the same machine.

#### It will deploy the following services in Docker:
- db (MariaDb)
- es (Elasticsearch)
- cws (CWS console)
- cws-worker1 (CWS Worker)

### Prerequisites:

1. Be sure to increase your Docker Resources to at least 4 CPUs and 10GB memory
2. Build `common-workflow-service` Docker Image using the `build.sh` script in the cws-image dir
   1. Update the version in the `build.sh` script if necessary
3. Update the `config.properties` and `docker-compose.yml` accordingly.
4. Create a creds file on your machine in path `~/.cws/creds` and set the file permission with `chmod 700 ~/.cws/creds`
5. Run the command `docker network create cws-network` to create a shared network space for other workers to join

To run use the command:

    docker-compose up

## Adding more workers

### Prerequisites:

1. Be sure to increase your Docker Resources and add 4GB memory per extra worker you want to run
2. You can easily add another worker (worker2) to this deployment by doing the following:
   1. `cd ../worker-ls`  (Change to the worker-ls directory)
   2. `docker-compose up`  (Startup worker2)
3. If you want to add even more workers you'll need to do the following:
   1. Copy the `worker-ls` directory to a new location
   2. Modify those `config.properites` and `docker-compose.yml` accordingly.
   3. Run `docker-compose up` in each new worker directory
