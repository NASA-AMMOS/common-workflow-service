# LDAP Authentication Setup Guide

## Overview

CWS supports LDAP (Lightweight Directory Access Protocol) authentication for enterprise directory integration. LDAP allows CWS to authenticate users against a central directory server and automatically provision user information and group memberships from LDAP.

LDAP authentication is built into CWS by default and requires no additional dependencies beyond the standard build.

## Quick Start

To build and run CWS with LDAP authentication:

```bash
# Set LDAP manager password (for user lookups)
export LDAP_MANAGER_PASSWORD="your_ldap_password"

# Use the provided LDAP development script
./jr-ldap.sh
```

Or manually with `dev.sh`:

```bash
./dev.sh <args> ldap <ldap_url> <ldap_manager_dn> <ldap_manager_password>
```

## Configuration

### LDAP Connection Settings

The following configuration parameters are required for LDAP authentication:

| Parameter | Description | Example |
|-----------|-------------|---------|
| `ldap_url` | LDAP server URL | `ldaps://ldap-202007.jpl.nasa.gov:636` |
| `ldap_manager_dn` | Manager DN for authenticated binds | `uid=admin,ou=Personnel,dc=dir,dc=jpl,dc=nasa,dc=gov` |
| `ldap_manager_password` | Manager password for binds | (set via env var for security) |

### LDAP Search Filters

Configure how users and groups are found in LDAP:

| Parameter | Description | Example |
|-----------|-------------|---------|
| `ldap_user_search_base` | Base DN for user searches | `ou=Personnel,dc=dir,dc=jpl,dc=nasa,dc=gov` |
| `ldap_group_search_base` | Base DN for group searches | `ou=Groups,dc=dir,dc=jpl,dc=nasa,dc=gov` |
| `ldap_user_search_filter` | Filter for user searches | `(&(uid=*)(objectClass=person))` |
| `ldap_group_search_filter` | Filter for group searches | `(&(cn=*)(objectClass=groupOfNames))` |
| `ldap_user_id_attribute` | User ID attribute | `uid` |
| `ldap_user_firstname_attribute` | First name attribute | `givenName` |
| `ldap_user_lastname_attribute` | Last name attribute | `sn` |
| `ldap_user_email_attribute` | Email attribute | `mail` |
| `ldap_group_id_attribute` | Group ID attribute | `cn` |
| `ldap_group_name_attribute` | Group name attribute | `cn` |
| `ldap_group_member_attribute` | Group members attribute | `member` |

## Development Setup

### Using the Quick Start Script

The `jr-ldap.sh` script provides a preconfigured LDAP setup for development:

```bash
export LDAP_MANAGER_PASSWORD="your_password"
./jr-ldap.sh
```

This script:
- Sets `SECURITY=ldap` to enable LDAP authentication
- Configures LDAP URL and manager DN for JPL LDAP
- Calls `dev.sh` with LDAP configuration

### Manual Configuration

To configure LDAP manually:

1. **Set environment variables:**
   ```bash
   export LDAP_URL="ldaps://your-ldap-server:636"
   export LDAP_MANAGER_DN="uid=admin,ou=Personnel,dc=example,dc=com"
   export LDAP_MANAGER_PASSWORD="your_password"
   ```

2. **Create your personal development script:**
   ```bash
   #!/bin/bash
   ./dev.sh `pwd` ${USER} mariadb 127.0.0.1 3306 cws_dev root mypassword \
     HTTP localhost cws-index 9200 n na na n ldap localhost \
     "admin@example.com" John Doe admin@example.com 1 16 1
   ```

## How LDAP Authentication Works

### Login Flow

1. **User enters credentials** on CWS login page
2. **CWS connects to LDAP** using manager DN and password
3. **CWS searches for user** using configured search filter
4. **CWS binds as user** to verify password
5. **CWS retrieves user info** (name, email, groups)
6. **User is authenticated** and session is created
7. **Camunda integration** links CWS user to Camunda user

### Session Management

- Users are authenticated against LDAP on login
- User information is cached in the CWS session
- Group memberships are retrieved and cached
- Session expires based on CWS session timeout
- Re-authentication required after session timeout

### Group Management

LDAP groups are automatically retrieved and available for:
- Authorization checks in processes
- Displaying user group memberships
- Process initiator group filtering
- Camunda group integration

## Prerequisites

### LDAP Server Requirements

- **Accessible LDAP or LDAPS server** (SSL/TLS recommended for production)
- **Manager account** with permissions to:
  - Search the user base DN
  - Search the group base DN
  - Perform authentication binds as regular users
- **Configured user and group structure** in LDAP

### Access Requirements

- **Network connectivity** to LDAP server
- **Firewall rules** allowing LDAP port (389 for LDAP, 636 for LDAPS)
- **SSL certificates** installed in Java truststore (if using LDAPS)

### LDAP Manager Credentials

You need a service account in LDAP with:
- Distinguished name (DN)
- Password
- Search permissions on user and group base DNs

**Security Note:** Never hardcode the LDAP manager password in scripts. Always set it via environment variable:

```bash
export LDAP_MANAGER_PASSWORD="your_password"
```

## Troubleshooting

### Connection Refused

**Problem:** "Could not connect to LDAP server"

**Causes:**
- LDAP server is not running
- Network connectivity issue
- Incorrect LDAP URL
- Firewall blocking LDAP port

**Solutions:**
1. Verify LDAP server is running and accessible:
   ```bash
   ldapwhoami -H ldaps://your-server:636 -D "cn=admin,dc=example,dc=com" -W
   ```
2. Check firewall rules allow LDAP port
3. Verify LDAP URL is correct (ldap:// or ldaps://)

### Authentication Failed

**Problem:** "Invalid credentials" or "Authentication failed"

**Causes:**
- Incorrect LDAP manager DN
- Incorrect LDAP manager password
- User not found in LDAP
- User search filter doesn't match user

**Solutions:**
1. Verify manager DN is correct for your LDAP structure
2. Verify manager password is correct
3. Test with `ldapsearch` to verify user exists:
   ```bash
   ldapsearch -H ldaps://your-server:636 \
     -D "uid=admin,ou=Personnel,dc=example,dc=com" \
     -W -b "ou=Personnel,dc=example,dc=com" \
     "uid=testuser"
   ```
4. Review and adjust user search filter

### SSL Certificate Errors

**Problem:** "PKIX path building failed" or SSL certificate verification errors

**Causes:**
- LDAP server SSL certificate not trusted
- Self-signed certificate
- Missing intermediate CA certificate

**Solutions:**
1. For self-signed certificates, import the certificate into Java truststore:
   ```bash
   keytool -import -alias ldap-server \
     -file /path/to/ldap-cert.pem \
     -keystore $JAVA_HOME/lib/security/cacerts \
     -storepass changeit
   ```
2. Restart CWS after importing certificate
3. For production, use certificates signed by trusted CA

### Users Not Found

**Problem:** "User not found in LDAP"

**Causes:**
- Incorrect user search base DN
- User search filter doesn't match user structure
- User ID attribute is incorrect

**Solutions:**
1. Verify user search base DN is correct
2. Test search filter with `ldapsearch`:
   ```bash
   ldapsearch -H ldaps://your-server:636 \
     -D "uid=admin,ou=Personnel,dc=example,dc=com" \
     -W -b "ou=Personnel,dc=example,dc=com" \
     "(&(uid=*)(objectClass=person))"
   ```
3. Adjust search filter to match your LDAP structure

### Groups Not Retrieving

**Problem:** Groups are not showing up in user profile

**Causes:**
- Incorrect group search base DN
- Group search filter doesn't match groups
- User is not member of groups in LDAP

**Solutions:**
1. Verify group search base DN
2. Test group search with `ldapsearch`:
   ```bash
   ldapsearch -H ldaps://your-server:636 \
     -D "uid=admin,ou=Personnel,dc=example,dc=com" \
     -W -b "ou=Groups,dc=example,dc=com" \
     "(&(cn=*)(objectClass=groupOfNames))"
   ```
3. Verify user's DN is in group member list
4. Adjust group member attribute if needed (e.g., `memberUid` vs `member`)

### Performance Issues

**Problem:** LDAP queries are slow

**Causes:**
- LDAP server is slow or overloaded
- Overly broad search filters
- Network latency to LDAP server
- Large number of groups

**Solutions:**
1. Optimize search filters to be more specific
2. Add indexes on frequently searched attributes in LDAP server
3. Check network connectivity and latency
4. Consider LDAP connection pooling (configured automatically)

## Advanced Configuration

### POSIX Groups

If using POSIX groups (cn + memberUid), configure:

```properties
ldap_group_member_attribute=memberUid
ldap_use_posix_groups=true
```

### Multiple Base DNs

To search multiple base DNs, use the search filter:

```properties
ldap_user_search_filter=(&(uid=*)(|(ou=Personnel)(ou=Contractors))(objectClass=person))
```

### Anonymous Binding

If your LDAP server allows anonymous searches (not recommended):

```properties
ldap_allow_anonymous_login=true
```

However, a manager account is still recommended for production use.

### SSL/TLS Configuration

For LDAPS connections, ensure:

1. Java truststore contains LDAP server certificate chain
2. LDAP URL uses `ldaps://` protocol
3. LDAP port is 636 (default for LDAPS)

## See Also

- [CAM Setup Guide](CAM_SETUP.md) - For CAM authentication with LDAP
- [README.md](README.md) - General CWS setup and configuration
- [Camunda Identity Management](https://docs.camunda.org/manual/latest/user-guide/identity-service/) - Camunda LDAP plugin docs
