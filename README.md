# NASA-AMMOS Common Workflow Service (CWS)

This repository is the open-source release of the [NASA-AMMOS](https://ammos.nasa.gov/) Common Workflow Service (CWS). It is the culmination of many years of development internally at JPL, and we are now bringing it to the public in hopes that the open-source community might benefit from its release.

CWS is built on top of the [BPMN Workflow Engine](https://camunda.com/products/camunda-bpm/bpmn-engine/). CWS extends Camunda's functionality by layering an intuitive user interface, auditable logging, extensibility with code snippits and adaptation layers, and plenty of other useful tools such a powerful external task engine, custom process initiators, and much more.

While this repository is mostly complete, the documentation will be a work-in-progress for some time as we parse through our internal docs and add them here. The `cws-test` package is also in need of an update, but we've included it here as it contains useful examples of integration testing for CWS.

While documentation is still in the works, please feel free to [open an issue](https://github.com/NASA-AMMOS/commoan-workflow-service/issues/new/choose) with your inquiry.

See the [wiki](https://github.com/NASA-AMMOS/common-workflow-service/wiki) for more information. 

# Installation

## Prerequisites

  - Mariadb or mysql database set up on either your local machine or a remote host. You will also need to create the following:
    - A database for CWS to use. `cws` is a good default.
    - A database user with full access to the above database.
  - ITerm2: Currently these build scripts include commands to open new terminal windows using ITerm2, so they are best run from that terminal.
  - Logstash: You will need to place the logstash 6.4.2 zip in `install/logging/`. This is a temporary workaround while we clean up our installation process. You can find the zip download [here](https://www.elastic.co/downloads/past-releases/logstash-6-4-2).

> **Note:** You will need to add your own Tomcat keystore and truststore to the `install/` directory for the CWS web console to work properly. See https://tomcat.apache.org/tomcat-9.0-doc/ssl-howto.html.

## Building CWS

To build and run CWS, use the `dev.sh` script - its usage is as follows:
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

USER=<your username for cws login>
CLOUD=<y|n> # Enable cloudwatch monitoring

EMAIL_LIST="<comma separated list of admin emails>"

ADMIN_FIRST="<admin first name>"
ADMIN_LAST="<admin last name>"
ADMIN_EMAIL="<admin email>"

# Num of workers to start. 1 is the minimum.
NUM_WORKERS=1

# Run the dev script
./dev.sh `pwd` ${USER} ${DB_TYPE} ${DB_HOST} ${DB_PORT} ${DB_NAME} ${DB_USER} ${DB_PASS} ${CLOUD} ${SECURITY} ${HOSTNAME} ${EMAIL_LIST} ${ADMIN_FIRST} ${ADMIN_LAST} ${ADMIN_EMAIL} ${NUM_WORKERS}
```

The above script will build CWS, verify your configuration, then will start the CWS console and workers. The script will provide a link to access the console dashboard once everything has started up!

## Stopping CWS

You can stop CWS by running `./stop_dev.sh`. The script will bring down the console and all local workers.

# Contributing

Please see our [contribution guidelines](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/CONTRIBUTING.md).

# License

The source files in this repository are made available under the [Apache License Version 2.0](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/LICENSE).
