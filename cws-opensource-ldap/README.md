# Deploy OpenLDAP Server

### LDAP Server Configuration

- The open source LDAP server, with default user configuration, can be activated using Docker.
    - LDIF files _user.ldif_ and _access.ldif_ contain the default DN(distinguished name), UserId, and ldap user details.
      - `dn: cn=cwsuser,ou=Users,dc=example,dc=com`
      - `uid: cwsuser`
      - You can configure the user directory data by editing the `user.ldif` file. You can find more LDAP and LDIF instructions, [here](https://docs.oracle.com/cd/A87860_01/doc/network.817/a86101/syntax.htm). 
      
### Start OpenLDAP Container
- While inside this folder run command:
  
```
docker-compose up -d
```

#### *Test LDAP Server*
Once `openldap_container` is up, run a ldapsearch command to verify user configuration.
- Enter Docker container:
```
docker exec -it openldap_container bash
```
- Run ldapsearch command: 
```
ldapsearch -x -LLL -H ldap://localhost:389 -b dc=example,dc=com -D "cn=<cn>,ou=Users,dc=example,dc=com" -w <userpassword> uid=<uid>
```

#### CWS LDAP Authorization Plugin

- The LDAP plugin property settings below match the default configuration for the openldap server.

```
    <property name="serverUrl"                   value="__CWS_LDAP_URL__" />
    <property name="acceptUntrustedCertificates" value="false" />
    <property name="baseDn"                      value="dc=example,dc=com" />
    <property name="userSearchBase"              value="ou=Users" />
    <property name="userSearchFilter"            value="(objectclass=inetOrgPerson)" />
    <property name="userIdAttribute"             value="uid" />
    <property name="userFirstnameAttribute"      value="givenName" />
    <property name="userLastnameAttribute"       value="sn" />
    <property name="userEmailAttribute"          value="mail" />
    <property name="userPasswordAttribute"       value="userpassword" />
    <property name="groupSearchBase"             value="ou=Users" />
    <property name="groupSearchFilter"           value="(|(cn=your.first.group)(cn=your.other.group))" />
    <property name="groupIdAttribute"            value="cn" />
    <property name="groupNameAttribute"          value="cn" />
    <property name="groupMemberAttribute"        value="uniqueMember" />
```

_Based on the public GIT repository by rackerlabs, reference: https://github.com/rackerlabs/dockerstack/tree/master/keystone/openldap_