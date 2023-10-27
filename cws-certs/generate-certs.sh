#! /bin/bash

# the following bash script creates open-source certs required to access CWS

# create private key and self-signed certificate within a keystore
keytool -genkey -keyalg RSA -dname "cn=cws, ou=cws, o=cws, l=cws, s=FL, c=US" -alias cws -keypass changeit -keystore .keystore -storepass changeit -storetype JKS -validity 360 -keysize 2048

# extract self-signed certificate from keystore
keytool -export -alias cws -file cws.crt -keystore .keystore -storepass changeit

# insert self-signed certificate into truststore
keytool -import -alias cws -file cws.crt -keypass changeit -noprompt -keystore cws_truststore.jks -storepass changeit -storetype JKS

# place open-source certs in appropriate directories
cp .keystore ../install
cp cws_truststore.jks ../install/tomcat_lib