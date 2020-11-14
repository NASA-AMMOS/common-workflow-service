#!/bin/sh
# ------------------
# deploy_proc_def.sh
# ------------------
# Deploys a process definition file to an existing CWS installation identified by a hostname and port.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

usage="
USAGE:
$(basename "$0") -f <PATH_TO_PROC_DEF_FILE> -h <CWS_SERVER_HOST> -p <CWS_SERVER_PORT>

example:
    $(basename "$0") -f /path/to/my/cool.bpmn -h <cws_hostname> -p 38443"

# A POSIX variable
OPTIND=1         # Reset in case getopts has been used previously in the shell.

while getopts 'f:h:p:' option; do
  case "$option" in
    f) PATH_TO_PROC_DEF_FILE=$OPTARG
       ;;
    h) CWS_HOST=$OPTARG
       ;;
    p) CWS_PORT=$OPTARG
       ;;
    :) printf "missing argument for -%s\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
   \?) printf "illegal option: -%s\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
  esac
done
shift $((OPTIND - 1))
LEFTOVERS=$@
if [[ ! -z "$LEFTOVERS" ]]; then
  echo "UNEXPECTED VALUES SPECIFIED IN COMMAND LINE: $LEFTOVERS"
  echo "$usage" >&2
  exit 1
fi

if [[ ! -f ${ROOT}/cookies.txt ]]; then
  print "cookies.txt file not found! This file is required for authentication to CWS server."
  print "Please run the refresh_cws_token.sh script first, to generate the cookies.txt file."
  exit 1
fi

API=https://${CWS_HOST}:${CWS_PORT}/camunda/api

print "Deploying process definition located at: ${PATH_TO_PROC_DEF_FILE} to CWS server: ${CWS_HOST}:${CWS_PORT}..."

if [[ ! -f ${PATH_TO_PROC_DEF_FILE} ]]; then
  print "No model file found at path: ${PATH_TO_PROC_DEF_FILE}!  Aborting script..."
  exit 1
fi

# deploy
curl --trace-ascii "deployment.trace" -w "\n" --cookie ${ROOT}/cookies.txt \
  -H "Accept: application/json" \
  -F "deployment-name=rest-test" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=false" \
  -F "process.bpmn=@${PATH_TO_PROC_DEF_FILE}" \
  ${API}/engine/engine/default/deployment/create
