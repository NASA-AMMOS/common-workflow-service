# Based on the public GIT repository by rackerlabs
# Reference: https://github.com/rackerlabs/dockerstack/tree/master/keystone/openldap
FROM  ubuntu:latest

RUN apt-get update
RUN apt-get install -q -y vim ldapvi
RUN echo 'slapd/root_password password password' | debconf-set-selections
RUN echo 'slapd/root_password_again password password' | debconf-set-selections
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y slapd ldap-utils
RUN rm -rf /var/lib/apt/lists/*

ADD access.ldif user.ldif /

RUN service slapd start ;\
ldapadd -Y EXTERNAL -H ldapi:/// -f access.ldif &&\
ldapadd -x -D cn=admin,dc=example,dc=com -w password -c -f user.ldif

EXPOSE 389
CMD slapd -h 'ldap:/// ldapi:///' -g openldap -u openldap -F /etc/ldap/slapd.d -d stats
