# Running & Stopping CWS

## Starting CWS

During development, your [personal build script](building.md) builds CWS,
verifies configuration, and starts the console and workers for you. When
startup finishes, it prints a link to the console dashboard.

Under the hood, the installed distribution provides scripts for starting and
stopping CWS:

- `install/start_cws.sh` — start the console/worker(s).
- `install/stop_cws.sh` — stop them.

## Stopping CWS

To stop the development instance — bringing down the console and all local
workers:

```bash
./stop_dev.sh
```

## Verifying the instance

Once CWS is running:

1. Open the console in a browser over HTTPS on the configured console port.
2. Log in with your configured administrator credentials.
3. From the console you can [deploy process definitions](../user-guide/deploying.md),
   [launch and schedule processes](../user-guide/launching/index.md), and watch
   [workers](../user-guide/console/workers.md) pick up external tasks.

## Troubleshooting startup

- Confirm the database exists and the `database_*` settings are correct.
- Confirm Elasticsearch is reachable with the configured protocol/host/port.
- Confirm the keystore, truststore, and `~/.cws/creds` password file are in
  place (see [Certificates & Keystore](certificates.md)).
- Check the console and worker logs under the Tomcat `logs/` directory
  (`cws.log` and `catalina.out`).
