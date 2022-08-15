## Deploy OpenLDAP

- **Start OpenLDAP Container**: While inside this folder run docker-compose
    - `docker-compose up`
- **Add LDAP User to OpenLDAP Server**: Enter openldap Docker container and add ldap user
  - User can edit "user.ldif" file to fit the respective admin username
  - Once `user.ldif` file is ready, follow these commands to add user to the server
     - `docker cp user.ldif openldap:/`
     - `docker exec -it openldap bash`
     - `ldapadd -c -x -D "cn=admin,dc=openldap,dc=com" -W -f ./user.ldif`
     - Requested Password: `123`
