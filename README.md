# NASA-AMMOS Common Workflow Service (CWS)

This repository is the open-source release of the [NASA-AMMOS](https://ammos.nasa.gov/) Common Workflow Service (CWS). It is the culmination of many years of development internally at JPL, and we are now bringing it to the public in hopes that the open-source community might benefit from its release.

CWS is built on top of the [BPMN Workflow Engine](https://camunda.com/products/camunda-bpm/bpmn-engine/). CWS extends Camunda's functionality by layering an intuitive user interface, auditable logging, extensibility with code snippits and adaptation layers, and plenty of other useful tools such a powerful external task engine, custom process initiators, and much more.

While this repository is mostly complete, the documentation will be a work-in-progress for some time as we parse through our internal docs and add them here. The `cws-test` package is also in need of an update, but we've included it here as it contains useful examples of integration testing for CWS.

While documentation is still in the works, please feel free to [open an issue](https://github.com/NASA-AMMOS/commoan-workflow-service/issues/new/choose) with your inquiry.

See the [wiki](https://github.com/NASA-AMMOS/common-workflow-service/wiki) for more information. 

# Installation

## Prerequisites

  - Mariadb or mysql database set up on either your local machine or a remote host. You will also need to create the following:
    - A database for CWS to use. `cws_dev` is a good default.
    - A database user with full access to the above database.
  - [ITerm2](https://iterm2.com/): Currently these build scripts include commands to open new terminal windows using ITerm2, so they are best run from that terminal.
  - **Logstash 7.9+**: You will need to place the logstash 7.9.0 zip in `install/logging/`. This is a temporary workaround while we clean up our installation process. You can find the zip download [here](https://www.elastic.co/downloads/past-releases/logstash-7-9-0).
  - **Elasticsearch 7.9+**: CWS requires an externally-configured elasticsearch cluster to be set up. You can use elasticsearch with or without authentication. Please note that CWS currently only supports basic HTTP authentication.
  - Tomcat **keystore and truststore files** (needed for CWS web console to work properly):
    - You will need to add your own Tomcat keystore file to this path: `install/.keystore`
    - You will need to add your own truststore file to this path: `install/tomcat_lib/cws_truststore.jks`
    - See: https://tomcat.apache.org/tomcat-9.0-doc/ssl-howto.html
    

#### Development Environment Configuration



##### Generate mariaDB Docker Container and Create Database Instance for CWS:
```
docker run -d -p 3306:3306 -e MYSQL_DATABASE=__DB_NAME__ -e MYSQL_ROOT_PASSWORD=__ROOT_PW__ -e TZ=America/Los_Angeles --name mdb103 mariadb:1
```

`__DB_NAME__` and `__ROOT_PW__` must match parameters set in script file: `<personal-dev>.sh`

##### Directly access mariaDB with:

```
mysql -h 127.0.0.1 -u root -p
```

_Make sure `cws_dev` database in created mariaDB instance before moving forward to build CWS_

## Building CWS

#### Pre-CWS Build: Start ElasticSearch
Open new Shell terminal designated for running ElasticSearch.

* `cd` into `install/docker/es-only` directory and run Docker Compose: 
```
docker-compose up
```



#### Build CWS

_In a different terminal window `cd` into root of **common-workflow-service** folder and follow Build CWS instructions._

For development we tend to create our own separate build script `<personal-dev.sh>` (firstinitial-lastname.sh), i.e.:`jsmith.sh`, that calls `dev.sh`. Here's an template for your personal build script that will work for development on a local machine:

```
#File: jsmith.sh

#!/bin/bash

HOSTNAME=localhost

# Used in cws-test
echo "$HOSTNAME" > cws-test/src/test/resources/hostname.txt

SECURITY="camunda"

# Stop CWS is it is currently running
./stop_dev.sh

# DB config
DB_TYPE=mariadb
DB_HOST=127.0.0.1
DB_NAME=cws_dev # needs to match the db you set up beforehand
DB_USER=root # needs to match the user you set up beforehand
DB_PASS=     # could also be specified with environment vars
DB_PORT=3306 # mariadb default

USER=   # Username
CLOUD=  # Enable cloudwatch monitoring

EMAIL_LIST="{email}"

ADMIN_FIRST="{first}"
ADMIN_LAST="{last}"
ADMIN_EMAIL="{email}"

# ES config
ES_HOST="localhost"
ES_PORT=9200
ES_USE_AUTH=n
ES_USERNAME="na"
ES_PASSWORD="na"

# Num of workers to start. 1 is the minimum.
NUM_WORKERS=1

# Default value is 1. Maximum is 20. Specifies the number of days until the
# abandoned workers in the cws_workers database table are cleaned out.
WORKER_ABANDONED_DAYS=1

# Run the dev script
./dev.sh `pwd` ${USER} ${DB_TYPE} ${DB_HOST} ${DB_PORT} ${DB_NAME} ${DB_USER} ${DB_PASS} ${ES_HOST} ${ES_PORT} ${ES_USE_AUTH} ${ES_USERNAME} ${ES_PASSWORD} ${CLOUD} ${SECURITY} ${HOSTNAME} ${EMAIL_LIST} ${ADMIN_FIRST} ${ADMIN_LAST} ${ADMIN_EMAIL} ${NUM_WORKERS} ${WORKER_ABANDONED_DAYS}
```

###### Run Personal Dev Script
To build and run CWS, use your <personal-dev.sh> i.e.:`jsmith.sh` script - its usage is as follows:

```
./jsmith.sh
```



The above script will build CWS, verify your configuration, then will start the CWS console and workers. The script will provide a link to access the console dashboard once everything has started up!

## Stopping CWS

You can stop CWS by running `./stop_dev.sh`. The script will bring down the console and all local workers.

# Contributing

Please see our [contribution guidelines](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/CONTRIBUTING.md).

# License

The source files in this repository are made available under the [Apache License Version 2.0](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/LICENSE).
