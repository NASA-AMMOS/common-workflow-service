# Installation Security Considerations

This page covers security-related decisions and best practices to keep in mind
during CWS installation.

## TLS / SSL

- CWS **requires** HTTPS for the console. Ensure your keystore and truststore
  are in place before starting (see [Certificates & Keystore](certificates.md)).
- Use certificates signed by a trusted CA for production. Self-signed
  certificates are acceptable only for development.
- Restrict the `~/.cws/creds` keystore password file to owner-only permissions
  (`chmod 400`).

## Network exposure

- The CWS console ports (`cws_web_port`, `cws_ssl_port`) should only be
  exposed to trusted networks.
- The message broker port (`amq_port`) and JMX ports should **not** be exposed
  to the public internet.
- Use firewall rules or security groups to limit access to known hosts.

## Authentication

- Use **LDAP** in production (see [LDAP Security](ldap.md)). Camunda security
  mode is for development only.
- Rotate the `admin_user` password after initial setup.
- Set `cws_token_expiration_hours` to a reasonable value (default 24 hours);
  shorter is more secure.

## Database

- Use a dedicated database user for CWS with only the permissions it needs
  (CRUD on its schema, not global admin).
- Secure the database connection — use network-level controls or SSL if the
  database is on a remote host.

## Elasticsearch

- If using HTTPS Elasticsearch with authentication
  (`elasticsearch_use_auth=y`), keep the credentials out of version control.
- Elasticsearch should not be directly accessible from untrusted networks.

## Docker deployments

- The default Docker image ships with the `changeit` keystore password — change
  it for any non-development use.
- Mount your own certificates and credentials via Docker volumes rather than
  embedding them in images.
