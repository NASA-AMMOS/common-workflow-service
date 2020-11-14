#!/bin/bash
# ------------
# stop_cws.sh
# ------------
# Script for stopping a CWS server (console or worker) on Linux or MacOSX.
# This script is template-based, and is filled in with the CWS host/port during a CWS install.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

# ======================
# DISPLAY USAGE FUNCTION
# ======================
function usage {
cat << EOF

------------------------------------------------
DESCRIPTION:

  Stops a running CWS server.

USAGE:

  ${0##*/}  [OPTION]...

OPTIONS:

  -h, --help
     display this help and exit

  --shutdown-all
     Stops all running CWS servers. Requires valid user authentication.

EOF

}

# ===============
# SETUP VARIABLES
# ===============
CWS_TOMCAT_HOME=${ROOT}/server/apache-tomcat-${TOMCAT_VER}
export CATALINA_HOME=${CWS_TOMCAT_HOME}
CWS_TOMCAT_SHUTDOWN_CMD=${CWS_TOMCAT_HOME}/bin/shutdown.sh

# ---------------------------
# PARSE THE INPUT ARGUMENTS
while :
do
    case "$1" in
      -h | --help)
        usage  # display usage information
        # no shifting needed here, we're done.
        exit 0
      ;;
      --shutdown-all)
        SHUTDOWN_ALL="true"
        shift 1
      ;;
      --) # End of all options
        shift
      ;;
      -*)
        echo "Error: Unknown option: $1" >&2
        usage
        exit 1
      ;;
      *)  # No more options
      if [[ -z "$1" ]]; then
        break
      else
        echo "Error: Unknown argument: $1" >&2
        usage
        exit 1
      fi
      ;;
    esac
done

# =========================
# PROCESS COMMAND LINE ARGS
# =========================
if [[ "${SHUTDOWN_ALL}" = "true" ]]; then
   CWS_CONSOLE_HOST='__CWS_CONSOLE_HOST__'
   CWS_CONSOLE_SSL_PORT='__CWS_CONSOLE_SSL_PORT__'

   print "Shutting down console and all workers..."
   print "Must login to do full shutdown"

   ${ROOT}/refresh_cws_token.sh > /dev/null
   curl -1 -k https://${CWS_CONSOLE_HOST}:${CWS_CONSOLE_SSL_PORT}/cws-ui/rest/system/shutdown -b cookies.txt
   RESULT=$?
   rm cookies.txt
   exit ${RESULT}
fi

# =======================
# KILL LOGSTASH PROCESS
# =======================
LS_KILL_PATTERN=${ROOT}/server/logstash-${LOGSTASH_VER}

kill_proc "Logstash" ${LS_KILL_PATTERN}

# ===========================
# KILL ELASTICSEARCH PROCESS
# ===========================
ES_KILL_PATTERN=${ROOT}/server/elasticsearch-${ELASTICSEARCH_VER}

kill_proc "Elasticsearch" ${ES_KILL_PATTERN}

# ===================
# TELL TOMCAT TO STOP
# ===================
print "Stopping CWS application server..."
${CWS_TOMCAT_SHUTDOWN_CMD} 2> ${ROOT}/logs/cws_tomcat_shutdown.log
TOMCAT_KILL_PATTERN=${CWS_TOMCAT_HOME}/bin/bootstrap.jar

# =============================================
# WAIT FOR TOMCAT TO STOP (15 seconds at most)
# =============================================
ITER="0"
TOMCAT_PROC_COUNT=$(remaining_procs ${TOMCAT_KILL_PATTERN})

while [[ ${TOMCAT_PROC_COUNT} -gt 0 ]]
do
	ITER=$[$ITER + 1]

	if (("$ITER" > "15")); then
		print "WARNING: NOT WAITING FOR TOMCAT TO DIE ON ITS OWN ANYMORE."
		break
	fi

	print "Waiting for Tomcat process (${CWS_TOMCAT_HOME}) to stop... (check #${ITER}/15)"
	sleep 1

	TOMCAT_PROC_COUNT=$(remaining_procs ${TOMCAT_KILL_PATTERN})
done

# ====================================
# IF NECESSARY, FORCEFULLY KILL TOMCAT
# ====================================
if [[ ${TOMCAT_PROC_COUNT} -gt 0 ]]; then
	kill_proc "Tomcat" "${CWS_TOMCAT_HOME}/bin/bootstrap.jar"
fi

# =======================
# DISPLAY PROCESS STATS
# =======================
TOMCAT_PROC_COUNT=$(remaining_procs ${TOMCAT_KILL_PATTERN})
LS_PROC_COUNT=$(remaining_procs ${LS_KILL_PATTERN})
ES_PROC_COUNT=$(remaining_procs ${ES_KILL_PATTERN})
print "---------------------------------------------------------------------------"
print "${TOMCAT_PROC_COUNT} REMAINING TOMCAT PROCS        (${CWS_TOMCAT_HOME})"
print "${LS_PROC_COUNT} REMAINING LOGSTASH PROCS      (${ROOT}/server/logstash-${LOGSTASH_VER})"
print "${ES_PROC_COUNT} REMAINING ELASTICSEARCH PROCS (${ROOT}/server/elasticsearch-${ELASTICSEARCH_VER})"
print "If any processes still remain, they should be manually terminated."
print "---------------------------------------------------------------------------"

print "Finished"