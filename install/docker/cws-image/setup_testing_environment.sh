#!/bin/sh

# --- Generate Certificates ---
CERT_SCRIPT="/opt/cws-certs/generate-certs-testing.sh"
if [ -f "${CERT_SCRIPT}" ]; then
  >&2 echo "Running certificate generation script: ${CERT_SCRIPT}"
  "${CERT_SCRIPT}"
  CERT_GEN_EXIT_CODE=$?
  if [ ${CERT_GEN_EXIT_CODE} -ne 0 ]; then
    >&2 echo "ERROR: Certificate generation script failed with exit code ${CERT_GEN_EXIT_CODE}."
    exit ${CERT_GEN_EXIT_CODE}
  fi
  >&2 echo "Certificate generation script completed successfully."
else
  >&2 echo "ERROR: Certificate generation script ${CERT_SCRIPT} not found."
  exit 1
fi
# --- End Certificate Generation ---

# --- Setup Credentials ---
mkdir -p ~/.cws
echo "changeit" >> ~/.cws/creds
chmod 700 ~/.cws/
chmod 600 ~/.cws/creds
>&2 echo "Testing credentials setup completed successfully."
# --- End Credentials Setup ---