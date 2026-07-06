# Configuration Page

The **Configuration** page in the console displays the current CWS system
configuration. It shows key information such as:

- **CWS version** — the running release version.
- **Java version** — the JDK powering this instance.
- **Database** — type, host, and schema in use.
- **Elasticsearch** — connection parameters.
- **Security mode** — the configured authentication/identity plugin.
- **Worker settings** — max processes, abandoned-worker cleanup, etc.

This page is read-only in the console. To change settings, edit the
[configuration file](../../install/configuration.md) and re-run the
configurator, or update specific values in the database as noted in the
[Configuration Properties reference](../../reference/configuration.md).
