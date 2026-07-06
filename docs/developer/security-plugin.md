# Custom Security Scheme Plugins

CWS supports pluggable security schemes. You can implement a custom
authentication/authorization mechanism to replace or augment the built-in LDAP
and Camunda security.

## How it works

CWS uses security filters to intercept requests and authenticate users. The
`identity_plugin_type` configuration setting selects which identity provider
is active.

To implement a custom security scheme:

1. Create a class in `cws-adaptation` that implements the security filter
   interface.
2. Register your filter in the web application configuration.
3. Set `identity_plugin_type` and related settings in your
   [configuration file](../install/configuration.md).

## Extension points

| Class / Interface | Purpose |
| --- | --- |
| `CwsLdapSecurityFilter` | The built-in LDAP security filter (reference implementation). |
| `CwsCamundaSecurityFilter` | The built-in Camunda security filter. |

Study these implementations in `cws-core` to understand the contract your
custom filter must satisfy.

## Configuration

Set the filter class names in your configuration properties:

```properties
ldap_security_filter_class=your.package.CustomSecurityFilter
camunda_security_filter_class=your.package.CustomCamundaFilter
```

## See also

- [Security & Roles (admin)](../administration/security.md) — configuring the
  built-in security modes.
- [LDAP Security](../install/ldap.md) — LDAP-specific installation.
