# Prerequisites

Install and prepare the following before building CWS. See
[Requirements & Compatibility](requirements.md) for version details.

## Java 17 JDK

CWS runs only on a **JDK 17** (not a JRE).

On macOS with Homebrew, Amazon Corretto 17 works well:

```bash
brew install --cask corretto@17
```

Then point `JAVA_HOME` at it in your shell startup (for example `.zprofile`):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v17)
```

## Maven

Maven downloads project dependencies and drives the build.

```bash
brew install maven
mvn -v   # verify
```

## Docker

[Docker](https://docs.docker.com/get-docker/) runs an external Elasticsearch
and the MariaDB database container. Recommended Docker resource allocation:

- CPUs: 5
- Memory: 14 GB
- Swap: 1 GB
- Disk image size: 64 GB

## Database (MariaDB or MySQL)

Set up MariaDB or MySQL on your local machine or a remote host, and create:

- A database (schema) for CWS — `cws_dev` is a good default for development.
- A database user with full (CRUD) access to that schema.

See [Database Setup](database.md) for a Dockerized MariaDB.

## Elasticsearch 8.12.0+

CWS requires an externally-configured Elasticsearch cluster. You may use a
secure (HTTPS) cluster with or without authentication, or an insecure (HTTP)
one. The [Elasticsearch Setup](elasticsearch.md) page provides a Dockerized
option.

## Logstash 8.12.0+ (temporary build input)

Download Logstash for your platform, then (only if it's a `.tar.gz`)
decompress it and re-zip it as `logstash-8.12.0.zip`, and place it in
`install/logging/`. This is a temporary step in the current installation
process. Download from the
[Elastic downloads page](https://www.elastic.co/downloads/logstash).

## SSL keystore, truststore, and password

The CWS web console requires TLS. You need:

- A Tomcat **keystore** at `install/.keystore`.
- A **truststore** at `install/tomcat_lib/cws_truststore.jks`.
- A **credentials file** at `~/.cws/creds` containing the keystore password.

You can generate open-source self-signed certificates with the project's
`generate-certs.sh` script — see [Certificates & Keystore](certificates.md).
Lock down the credentials file:

```bash
chmod 700 ~/.cws/
chmod 400 ~/.cws/creds
```

See the [Apache Tomcat SSL How-To](https://tomcat.apache.org/tomcat-9.0-doc/ssl-howto.html)
for background.

## A note on terminal tooling

The development build scripts open additional terminal windows. On macOS they
currently do this via iTerm2, so running them from
[iTerm2](https://iterm2.com/) is the smoothest experience during development.

Next: [Database Setup](database.md).
