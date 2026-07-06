# Security & Roles

CWS supports multiple authentication schemes to accommodate different deployment environments. The scheme is selected at install time and affects how users authenticate to both the CWS UI and its REST API.

## Authentication Schemes

| Scheme | Description | Default password |
|--------|-------------|-----------------|
| **LDAP** | Users authenticate with their LDAP credentials. CWS issues its own session token (`cwsToken`). | Your LDAP password |
| **Camunda** | Users and passwords managed in the Camunda database. | `changeme` (must be changed immediately) |
| **Custom** | Project-defined authentication plugin. | Depends on implementation |

!!! warning
    The Camunda scheme ships with the default password `changeme`. Change it immediately after installation through the Camunda Admin page.

## CWS Token Authentication (LDAP scheme)

When running in LDAP mode, CWS issues a session token after successful authentication. This token is stored as the `cwsToken` cookie and can be used to authenticate REST API calls from scripts or other automation.

### Generating a token

Use the `refresh_cws_token.sh` script bundled with CWS:

```bash
cd cws
./refresh_cws_token.sh
```

You will be prompted for your username and password. On success the script outputs:

- `cws_token.txt` — the raw token value, suitable for use in BPMN processes making REST calls
- `cookies.txt` — a curl-compatible cookie file for subsequent requests

Example output:

```
CWS authorization scheme is : LDAP
CWS token is   : 'CCE85F56A8AF0637B58B201E0BB1CE6A'
cookie file is : cookies.txt
token file is  : cws_token.txt
```

### Using the token in curl

**Inline token:**

```bash
curl https://<cws-host>:38443/cws-ui/rest/process/my_proc/schedule \
  -b "cwsToken=CCE85F56A8AF0637B58B201E0BB1CE6A" \
  --data "param1=value1"
```

**Cookie file:**

```bash
curl https://<cws-host>:38443/cws-ui/rest/process/my_proc/schedule \
  -b "/path/to/cookies.txt" \
  --data "param1=value1"
```

## HTTP Basic Authentication

All schemes support HTTP Basic Auth as a fallback. Pass credentials directly with curl:

```bash
curl https://<cws-host>:38443/cws-ui/rest/process/my_proc/schedule \
  -u myusername:mypassword \
  --data "param1=value1"
```

For GET requests, omit the `--data` flag.

## LDAP Identity Provider Plugin

CWS can be configured to use the Camunda `LdapIdentityProviderPlugin`, which synchronizes users and groups from your LDAP directory into Camunda's identity model.

### Configuration

The plugin is configured in `$CWS_HOME/server/conf/bpm-platform.xml`. Uncomment the LDAP section and set the following properties:

```xml
<plugin>
  <class>org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin</class>
  <properties>
    <property name="serverUrl">ldaps://your-ldap-server:636</property>
    <property name="acceptUntrustedCertificates">false</property>
    <property name="baseDn">dc=example,dc=com</property>

    <!-- User search -->
    <property name="userSearchBase">ou=personnel</property>
    <property name="userSearchFilter">(objectclass=person)</property>
    <property name="userIdAttribute">uid</property>
    <property name="userFirstnameAttribute">givenName</property>
    <property name="userLastnameAttribute">sn</property>
    <property name="userEmailAttribute">mail</property>
    <property name="userPasswordAttribute">userpassword</property>

    <!-- Group search — limit to specific groups to avoid loading the entire directory -->
    <property name="groupSearchBase">ou=personnel</property>
    <property name="groupSearchFilter">(|(cn=my-app-admin)(cn=my-app-users))</property>
    <property name="groupIdAttribute">cn</property>
    <property name="groupNameAttribute">cn</property>
    <property name="groupMemberAttribute">uniqueMember</property>
  </properties>
</plugin>
```

**Important:** The `groupSearchFilter` must restrict which groups are returned. Directories with hundreds of groups will cause performance problems and potential timeouts if the filter is too broad.

### User search options

**All users in the directory:**

```
userSearchFilter: (objectclass=person)
```

**Specific users only:**

```
userSearchFilter: (|(uid=alice)(uid=bob)(uid=carol))
```

## Security Filters

CWS applies security at multiple layers:

1. **LDAP filter** — validates credentials against the directory when in LDAP mode
2. **Camunda security filter** — enforces Camunda's own authorization model on cockpit/tasklist/admin pages
3. **General web security filter** — covers the CWS REST API and UI endpoints

These are configured through Spring Security and the Camunda process engine configuration. Refer to the adaptation guide if you need to customize filter behavior for your deployment.

## SSL / TLS

CWS requires SSL certificates to be installed before startup:

- `install/.keystore` — server keystore
- `install/tomcat_lib/cws_truststore.jks` — trust store

The keystore password is stored in `~/.cws/creds` with permissions set to `400`. See the installation guide for the certificate setup procedure.

## Further reading

- [Camunda 7.24 Identity / LDAP](https://docs.camunda.org/manual/7.24/user-guide/process-engine/identity-service/)
- [Camunda 7.24 Authorization](https://docs.camunda.org/manual/7.24/user-guide/process-engine/authorization-service/)
