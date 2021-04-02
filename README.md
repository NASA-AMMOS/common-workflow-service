# NASA-AMMOS Common Workflow Service (CWS)

This repository is the open-source release of the [NASA-AMMOS](https://ammos.nasa.gov/) Common Workflow Service (CWS). It is the culmination of many years of development internally at JPL, and we are now bringing it to the public in hopes that the open-source community might benefit from its release.

CWS is built on top of the [BPMN Workflow Engine](https://camunda.com/products/camunda-bpm/bpmn-engine/). CWS extends Camunda's functionality by layering an intuitive user interface, auditable logging, extensibility with code snippits and adaptation layers, and plenty of other useful tools such a powerful external task engine, custom process initiators, and much more.

While this repository is mostly complete, the documentation will be a work-in-progress for some time as we parse through our internal docs and add them here. The `cws-test` package is also in need of an update, but we've included it here as it contains useful examples of integration testing for CWS.

While documentation is still in the works, please feel free to [open an issue](https://github.com/NASA-AMMOS/commoan-workflow-service/issues/new/choose) with your inquiry.

See the [wiki](https://github.com/NASA-AMMOS/common-workflow-service/wiki) for more information. 

# Installation

You may install CWS from either an official release of from source. We recommend the use of our official releases, but have included instructions for building from source as well as building CWS in a development environment.

## Common Prerequisites

The following are common prerequisites that all build methods share.
 
 - Mariadb or MySQL database set up on either your local machine or a remote host. You will also need to create the following:
    - A database for CWS to use. `cws` is a good default.
      - `CREATE DATABASE cws;`
    - A database user with full access to the above database.
      - `CREATE USER 'cws'@'localhost' IDENTIFIED BY '<password>';`
      - `GRANT ALL PRIVILEGES ON cws.* TO 'cws'@'localhost';`
  - **Logstash 7.9+**: You will need to download the logstash 7.9.0 zip and place it in the directory corresponding to your installation type. You can find the zip download [here](https://www.elastic.co/downloads/past-releases/logstash-7-9-0).
    - When installing from a release build or custom build from source, place the logstash zip in `cws/server/`
    - For development, place the logstash zip in `install/logging/`.
  - **Elasticsearch 7.9+**: CWS requires an externally-configured elasticsearch cluster to be set up. You can use elasticsearch with or without authentication. Please note that CWS currently only supports basic HTTP authentication.
  - Tomcat **keystore and truststore files** (see https://tomcat.apache.org/tomcat-9.0-doc/ssl-howto.html)
    - When installing from a release build or custom build from source:
      - Add your Tomcat keystore file to the `cws/server/apache-tomcat-9.0.33/conf/` directory
      - Add your truststore file to the `cws/server/apache-tomcat-9.0.33/lib/` directory
    - For development:
      - Add your Tomcat keystore file to the `install/` direcotory
      - Add your truststore file to the `install/tomcat_lib/` directory
      
## Installing from a release build

Intalling from an official release is the easiest way to get up-and-running with CWS. Before you can start up the server, there are a few steps you must follow to prepare CWS for deployment.

1. Extract the CWS release

    ```sh
    tar xvzf cws_server.tar.gz
    ```

2. Follow the steps in [Configuring CWS](docs/configure\.md#Configuring-CWS) to configure CWS

3. Start CWS

    ```sh
    ./start_cws.sh
    ```

    > NOTE: Machines containing Console-type installations (install type 1 or 2) should be started before worker machines (install type 3)

## Building from source

You may wish to build CWS from source. All that's required is to ensure the common prerequisites are in place, then set the desired version number in each file returned by this command: `grep -R "update this each CWS release" .`.

Once you have followed the above steps, run `build.sh`. It will create `dist/cws-server.tar.gz`. You can now follow the above steps to [install from a release build](#installing-from-a-release-build).

## Developing for CWS

If you wish to make changes to the CWS codebase, you will need to get set up for development. CWS is written mostly in Java, and we use Maven as our build tool. There is a little bit of initial setup required, but once things are in place, the process of iterating on code changes and building/running development CWS is simple.

### Prerequisites

  - All common prerequisites
  - Java JDK 1.8 & Apache Maven 3.6+
  - [ITerm2](https://iterm2.com/): Currently these build scripts include commands to open new terminal windows using ITerm2, so they are best run from that terminal.

### Running in development mode

To build and run CWS during development, use the `dev.sh` script - its usage is as follows:
```
./dev.sh <Install directory> <ldap_username> <DB type - mariadb|mysql> <DB host> <DB port> <DB name> <DB user> <DB password> <Enable cloud? y|n> <Security scheme - CAMUNDA|LDAP> <hostname> <Emails list for alerts> <Admin first name> <Admin last name> <Admin email> <Number of workers>
```

For development we tend to create our own build scripts that call `dev.sh`. Here's an example that will work for development on a local machine:

```
#!/bin/bash

HOSTNAME=localhost

# Used in cws-test
echo "$HOSTNAME" > cws-test/src/test/resources/hostname.txt

SECURITY="camunda"

# Stop CWS is it is currently running
./stop_dev.sh

# DB config
DB_TYPE=mariadb
DB_HOST=localhost
DB_NAME=<your database name> # needs to match the db you set up beforehand
DB_USER=<your database user> # needs to match the user you set up beforehand
DB_PASS=<your database pass> # could also be specified with environment vars
DB_PORT=3306 # mariadb default

# ES config
ES_HOST=<your es host>
ES_PORT=<your es port>
ES_USE_AUTH=y/n
ES_USERNAME=<your es username> # matches externally configured es auth
ES_PASSWORD=<your es password> # matches externally configured es auth

USER=<your username for cws login>
CLOUD=<y|n> # Enable cloudwatch monitoring

EMAIL_LIST="<comma separated list of admin emails>"

ADMIN_FIRST="<admin first name>"
ADMIN_LAST="<admin last name>"
ADMIN_EMAIL="<admin email>"

# Num of workers to start. 1 is the minimum.
NUM_WORKERS=1

# Run the dev script
./dev.sh `pwd` ${USER} ${DB_TYPE} ${DB_HOST} ${DB_PORT} ${DB_NAME} ${DB_USER} ${DB_PASS} ${ES_HOST} ${ES_PORT} ${ES_USE_AUTH} ${ES_USERNAME} ${ES_PASSWORD} ${CLOUD} ${SECURITY} ${HOSTNAME} ${EMAIL_LIST} ${ADMIN_FIRST} ${ADMIN_LAST} ${ADMIN_EMAIL} ${NUM_WORKERS}
```

The above script will build CWS, verify your configuration, then will start the CWS console and workers. The script will provide a link to access the console dashboard (on the console terminal) once everything has started up!

### Stopping CWS in development mode

You can stop CWS by running `./stop_dev.sh`. The script will bring down the console and all local workers.

# Contributing

Please see our [contribution guidelines](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/CONTRIBUTING.md).

# License

The source files in this repository are made available under the [Apache License Version 2.0](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/LICENSE).
