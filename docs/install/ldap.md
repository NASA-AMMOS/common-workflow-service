# LDAP Security

CWS can authenticate users against an **LDAP** (or LDAPS) directory. This is
the recommended security mode for production deployments.

## Configuration

Set the following in your [configuration properties](../reference/configuration.md#security-authentication):

| Property | Value |
| --- | --- |
| `identity_plugin_type` | `LDAP` |
| `cws_ldap_url` | Your LDAP(S) URL, e.g. `ldaps://<ldap-host>:636` |
| `ldap_identity_plugin_class` | `org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin` |
| `ldap_security_filter_class` | `jpl.cws.core.web.CwsLdapSecurityFilter` |
| `admin_user` | The LDAP username of the initial CWS administrator. |

## How it works

- On login, CWS authenticates the user against the configured LDAP server.
- The `admin_user` receives full administrative permissions at first login.
- Additional users and permissions are managed from the Camunda Admin page (see
  [User Administration](../administration/users.md)).

## OpenLDAP for development

For development and testing without an enterprise LDAP server, CWS provides a
Dockerized OpenLDAP setup:

```bash
cd cws-opensource-ldap
docker-compose up -d
```

See `cws-opensource-ldap/README.md` for LDIF customization and user
provisioning.

## Camunda security mode (alternative)

If LDAP is not available, CWS can use Camunda's built-in identity service
(`SECURITY="camunda"` in the dev script). In this mode, users are managed
entirely within Camunda's database rather than an external directory.

## See also

- [Security & Roles (admin)](../administration/security.md)
- [Custom Security Scheme Plugins](../developer/security-plugin.md)
