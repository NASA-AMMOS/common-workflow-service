#!/bin/sh

# Run testing environment setup first
>&2 echo "Running testing environment setup..."
./setup_testing_environment.sh
SETUP_EXIT_CODE=$?

if [ ${SETUP_EXIT_CODE} -ne 0 ]; then
  >&2 echo "ERROR: Testing environment setup failed with exit code ${SETUP_EXIT_CODE}. Exiting."
  exit ${SETUP_EXIT_CODE}
fi

>&2 echo "Testing environment setup completed. Starting main application..."

# Run the regular wait script
exec ./wait_for_db_es_console.sh "$@"