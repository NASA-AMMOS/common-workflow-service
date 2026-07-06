# Installation

This guide covers installing CWS for development or production from source. To
simply try CWS on one machine, use the
[Docker quickstart](../getting-started/quickstart.md) instead.

## Overview

A CWS installation involves:

1. Meeting the [requirements](requirements.md) and installing the
   [prerequisites](prerequisites.md).
2. Setting up a [database](database.md) (MariaDB or MySQL).
3. Setting up an [Elasticsearch](elasticsearch.md) cluster.
4. Providing [SSL certificates](certificates.md) (keystore and truststore).
5. [Building CWS](building.md) from source.
6. Supplying a [configuration](configuration.md) and running the configurator.
7. [Running and stopping](running.md) the console and workers.

Optional / environment-specific topics:

- [LDAP security](ldap.md)
- [Installing the Modeler](modeler.md)
- [Installation security considerations](security-considerations.md)

## Installation types

CWS can be installed as a **Console**, a **Worker**, or **both** on the same
host. You select this with the `install_type` configuration setting:

| `install_type` | Meaning |
| --- | --- |
| `1` | Console and Worker |
| `2` | Console only |
| `3` | Worker only |

A typical deployment has one Console host and one or more Worker hosts, all
pointing at a shared database, message broker, and Elasticsearch cluster.

Start with [Requirements & Compatibility](requirements.md).
