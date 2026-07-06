# Upgrade & Migration

This guide covers the general procedure for upgrading CWS and documents version-specific steps for known breaking changes.

## General upgrade procedure

!!! warning "Back up before upgrading"
    Always back up your database and any custom configuration files before starting an upgrade.

1. **Stop all CWS instances** — console and all workers:

    ```bash
    cd <install-dir>
    ./stop_cws.sh --shutdown_all
    ```

2. **Move the existing installation aside** — you will need to copy data from it:

    ```bash
    mv <install-dir> <install-dir>-backup
    ```

3. **Install the new version** — unpack the new CWS server package and run the configuration script with your existing `config.properties`:

    ```bash
    tar zxvf cws_server-<new-version>.tar.gz
    cd cws_server-<new-version>
    ./configure.sh /path/to/your/config.properties
    ```

4. **Run any required database migration scripts** — check the version-specific notes below for your upgrade path. Scripts are located under `install/upgrade/` in the new package.

5. **Start the new CWS console** and immediately stop it (console only) — this initialises the new schema:

    ```bash
    ./start_cws.sh
    ./stop_cws.sh
    ```

6. **Migrate Elasticsearch data** if required — see [Elasticsearch data migration](#elasticsearch-data-migration) below.

7. **Restart CWS** on the console and all workers:

    ```bash
    ./start_cws.sh
    ```

## Elasticsearch data migration

If you are upgrading to an Elasticsearch version that is not backward-compatible with your existing data, you must migrate the data directory.

1. Stop CWS on the console.
2. Copy the old Elasticsearch `data` directory to the new installation:

    ```bash
    mv <new-install>/server/elasticsearch-<new-ver>/data \
       <new-install>/server/elasticsearch-<new-ver>/data_orig
    cp -R <old-install>/server/elasticsearch-<old-ver>/data \
          <new-install>/server/elasticsearch-<new-ver>/data
    ```

3. Start the new CWS. Elasticsearch will reindex the migrated data on first start.

If using an external Elasticsearch cluster (the recommended configuration), back up and restore your indices using the standard Elasticsearch snapshot/restore API rather than copying data directories.

## Version-specific notes

### v2.3 → v2.4

**Database schema change:** A new column `max_num_running_procs` was added to the `cws_worker` table. Run the provided upgrade script before restarting CWS:

```bash
cd <new-install>
./install/upgrade/upgrade_2.3_to_2.4.sh
```

The script also removes stale worker rows from previous deployments.

The `max_num_running_procs` value controls the maximum number of concurrently running process instances per worker. It is configurable per worker row in the database and takes effect immediately without a restart.

### v2.0 → v2.1

No database changes required. Follow the [general upgrade procedure](#general-upgrade-procedure).

### v1.8 → v2.0

**Camunda engine upgrade:** CWS v1.8 used Camunda 7.10; CWS v2.0 uses Camunda 7.13. If you want to retain your existing workflow history, you must migrate the Camunda database schema in three steps. Scripts are in `<install-dir>/sql/upgrade/`:

```bash
# Execute in order — substitute mariadb_ prefix with mysql_ if using MySQL
mariadb_engine_7.10_to_7.11.sql
mariadb_engine_7.11_to_7.12.sql
mariadb_engine_7.12_to_7.13.sql
```

Procedure:

1. Stop all CWS instances.
2. Back up the database.
3. Execute the three SQL scripts against your database in the order listed.
4. Upgrade all CWS installations to v2.0.
5. Restart CWS.

## Docker upgrades

When upgrading a Docker-based deployment:

1. Pull or rebuild the new image (`nasa-ammos/common-workflow-service:<new-version>`).
2. Update the image tag in `docker-compose.yml`.
3. Apply any database migration scripts by running them against the database container:

    ```bash
    docker exec -i cws-db mysql -u root -p<password> cws < upgrade_script.sql
    ```

4. Bring the stack back up with `docker-compose up`.
