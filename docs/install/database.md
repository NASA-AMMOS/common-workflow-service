# Database Setup

CWS stores its state in a **MariaDB** or **MySQL** database. All database access
is centralized through `cws-core`'s `DbService`. This page shows a Dockerized
MariaDB suitable for development.

## Run MariaDB in Docker

Create a MariaDB container and a database instance for CWS:

```bash
docker run -d -p 3306:3306 \
  -e MYSQL_DATABASE=<your-db-name> \
  -e MYSQL_ROOT_PASSWORD=<your-root-password> \
  -e TZ=Etc/UTC \
  --name mdb1011 mariadb:10.11
```

- Replace `<your-db-name>` with your desired database name (for example
  `cws_dev`).
- Replace `<your-root-password>` with a password of your choice.
- `TZ` sets the container timezone — set it to whatever your environment
  requires (e.g. `Etc/UTC` or a region such as `America/New_York`).

!!! important
    The database name and password you use here must match the values in your
    [build/configuration](building.md).

## Connect to MariaDB

```bash
mysql -h 127.0.0.1 -u root -p
```

Enter the password you set above when prompted.

!!! note
    Directly accessing MariaDB via the MySQL monitor assumes CWS has been built
    (the build script carries the information required to access the database).
    See [Building from Source](building.md).

Make sure your CWS database (e.g. `cws_dev`) exists in the running MariaDB
instance before building CWS.

## Presets & defaults

Preset configuration variables (such as default SMTP and LDAP settings) live in:

- `install/installerPresets.properties`
- `install/example-cws-configuration.properties`
- `utils.sh`

See the [Configuration Reference](../reference/configuration.md) for the full
list of database settings (`database_type`, `database_host`, `database_port`,
`database_name`, `database_username`, `database_password`).

Next: [Elasticsearch Setup](elasticsearch.md).
