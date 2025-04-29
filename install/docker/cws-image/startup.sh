#!/bin/bash

# Define Tomcat conf directory and keystore path/password
TOMCAT_CONF_DIR="/home/cws_user/cws/server/apache-tomcat-9.0.75/conf"
KEYSTORE_FILE="${TOMCAT_CONF_DIR}/.keystore"
KEYSTORE_PASS="changeit" # Simple password for CI

# Ensure Tomcat conf directory exists (should be created when tarball is extracted)
mkdir -p "${TOMCAT_CONF_DIR}"

# Generate self-signed keystore if it doesn't exist
if [ ! -f "${KEYSTORE_FILE}" ]; then
  echo "Generating self-signed keystore for HTTPS..."
  keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
          -keystore "${KEYSTORE_FILE}" \
          -storepass "${KEYSTORE_PASS}" \
          -keypass "${KEYSTORE_PASS}" \
          -dname "CN=localhost, OU=CWS, O=NASA, L=Pasadena, ST=CA, C=US" \
          -validity 3650
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to generate keystore."
    exit 1
  fi
  echo "Keystore generated at ${KEYSTORE_FILE}"
else
  echo "Keystore already exists at ${KEYSTORE_FILE}"
fi

# Remove creds file modification - installer should use config.properties
echo "Skipping modification of creds file."

# --- Add keystore password to config.properties file ---
CONFIG_FILE_PATH="/home/cws_user/config.properties"
KEYSTORE_CONFIG_LINE="cws_keystore_storepass=${KEYSTORE_PASS}"

if [ -f "${CONFIG_FILE_PATH}" ]; then
  # Check if the line already exists
  if grep -q "^cws_keystore_storepass=" "${CONFIG_FILE_PATH}"; then
    echo "Keystore password line already exists in ${CONFIG_FILE_PATH}"
  else
    echo "Adding keystore password line to ${CONFIG_FILE_PATH}"
    # Append the line to the config file
    echo "" >> "${CONFIG_FILE_PATH}" # Add newline for safety
    echo "${KEYSTORE_CONFIG_LINE}" >> "${CONFIG_FILE_PATH}"
  fi
else
  # This should not happen if docker-compose mount works
  echo "ERROR: Configuration file ${CONFIG_FILE_PATH} not found. Cannot add keystore password."
  exit 1 # Make this fatal, as configure will fail anyway
fi
# --- End of adding keystore password ---

javac -cp joda-time-2.1.jar getTime.java
java -cp .:joda-time-2.1.jar getTime

ls /home/cws_user/cws/server/apache-tomcat-9.0.75/logs

# Clear out any previous logs before starting (Note: Previous logs will cause CWS not to start)
rm -rf /home/cws_user/cws/server/apache-tomcat-9.0.75/logs/*

# --- Debug: Print config file contents before configure ---
echo "--- Contents of ${CONFIG_FILE_PATH} before configure.sh ---"
cat "${CONFIG_FILE_PATH}"
echo "--- End of ${CONFIG_FILE_PATH} ---"
# --- End Debug ---

cd cws

# Run configure and check exit code
./configure.sh ../config.properties Y
CONFIGURE_EXIT_CODE=$?
if [ ${CONFIGURE_EXIT_CODE} -ne 0 ]; then
  echo "ERROR: configure.sh failed with exit code ${CONFIGURE_EXIT_CODE}."
  exit 1
fi

echo
echo "Done with configure!"
echo

# Run start_cws and check exit code
./start_cws.sh
START_EXIT_CODE=$?
if [ ${START_EXIT_CODE} -ne 0 ]; then
  echo "ERROR: start_cws.sh failed with exit code ${START_EXIT_CODE}."
  exit 1
fi

echo "startup.sh completed successfully."
