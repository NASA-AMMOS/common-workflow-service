#! /bin/bash

# This script creates certs required by CWS when run inside the container.

# Define target directories within the container
# Ensure TOMCAT_VER matches the version used in the CWS distribution
TOMCAT_VER="9.0.75"
TOMCAT_BASE_DIR="/home/cws_user/cws/server/apache-tomcat-${TOMCAT_VER}"
TOMCAT_CONF_DIR="${TOMCAT_BASE_DIR}/conf"
TOMCAT_LIB_DIR="${TOMCAT_BASE_DIR}/lib"
KEYSTORE_FILE="${TOMCAT_CONF_DIR}/.keystore"
TRUSTSTORE_FILE="${TOMCAT_LIB_DIR}/cws_truststore.jks"
CERT_FILE="/tmp/cws.crt" # Temporary location for the exported cert
PASSWORD="changeit"     # Must match the password expected by CWS/Tomcat

echo "Generating CWS certificates..."
echo "  Keystore target: ${KEYSTORE_FILE}"
echo "  Truststore target: ${TRUSTSTORE_FILE}"

# Ensure target directories exist
mkdir -p "${TOMCAT_CONF_DIR}"
mkdir -p "${TOMCAT_LIB_DIR}"

# Create private key and self-signed certificate within the keystore at the target location
keytool -genkey -keyalg RSA \
        -dname "cn=cws-container, ou=CWS, o=NASA, l=Pasadena, s=CA, c=US" \
        -alias cws \
        -keypass "${PASSWORD}" \
        -keystore "${KEYSTORE_FILE}" \
        -storepass "${PASSWORD}" \
        -storetype JKS \
        -validity 3650 \
        -keysize 2048
if [ $? -ne 0 ]; then echo "ERROR: Failed to generate keystore."; exit 1; fi
echo "  Keystore generated."

# Extract self-signed certificate from keystore to a temporary file
keytool -export -alias cws \
        -file "${CERT_FILE}" \
        -keystore "${KEYSTORE_FILE}" \
        -storepass "${PASSWORD}"
if [ $? -ne 0 ]; then echo "ERROR: Failed to export certificate."; exit 1; fi
echo "  Certificate exported to ${CERT_FILE}."

# Import self-signed certificate into truststore at the target location
keytool -import -alias cws \
        -file "${CERT_FILE}" \
        -keypass "${PASSWORD}" \
        -noprompt \
        -keystore "${TRUSTSTORE_FILE}" \
        -storepass "${PASSWORD}" \
        -storetype JKS
if [ $? -ne 0 ]; then echo "ERROR: Failed to import certificate into truststore."; exit 1; fi
echo "  Certificate imported into truststore."

# Clean up temporary certificate file
rm -f "${CERT_FILE}"
echo "  Temporary certificate file removed."

echo "Certificate generation complete."
exit 0
