# Configuration Reference

CWS is configured with a properties file that you pass to the configurator.

## How configuration is applied

1. Copy the example configuration and fill in your values:

    ```bash
    cp install/example-cws-configuration.properties my-cws-configuration.properties
    # edit my-cws-configuration.properties, filling in the [YourXXX] placeholders
    ```

2. Run the configurator with your file:

    ```bash
    ./configure.sh my-cws-configuration.properties
    ```

Preset defaults are drawn from `install/installerPresets.properties`,
`install/example-cws-configuration.properties`, and `utils.sh`.

## Where to find every setting

The complete, grouped list of configuration properties — host and installation,
database, security and LDAP, ports, messaging, email, Elasticsearch, history
retention, workers, and optional AWS settings — is documented in the reference:

<p><a class="md-button md-button--primary" href="../../reference/configuration/">Configuration Properties reference</a></p>

## Key choices at install time

| Setting | Why it matters |
| --- | --- |
| `install_type` | Whether this host is a Console, a Worker, or both. |
| `database_*` | Connection to your MariaDB/MySQL schema. |
| `elasticsearch_*` | Connection to your Elasticsearch cluster. |
| `identity_plugin_type` / `cws_ldap_url` | Authentication backend (e.g. LDAP). |
| `history_level` | Must be identical across the Console and all Workers. |
| `hostname` / `amq_host` / `cws_console_host` | How components find each other. |

Next: [Running & Stopping CWS](running.md).
