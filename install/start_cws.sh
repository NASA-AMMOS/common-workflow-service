#!/bin/bash
# ------------
# start_cws.sh
# ------------
# Script for starting a CWS server (console or worker) on Linux or MacOSX.

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source ${ROOT}/utils.sh

# ======================
# DISPLAY USAGE FUNCTION
# ======================
function usage {
cat << EOF

------------------------------------------------
DESCRIPTION:

  Starts a CWS server.

USAGE:

  ${0##*/}  [OPTION]...

OPTIONS:

  -h, --help
     display this help and exit

  -d, --debug DEBUG_PORT
     Optional.
     If specified, starts tomcat in debug mode with JPDA_ADDRESS.
     See: http://wiki.apache.org/tomcat/FAQ/Developing#Debugging
     See: https://github.com/NASA-AMMOS/common-workflow-service/wiki
EOF

if [[ "$INSTALL_TYPE" = "1" || "$INSTALL_TYPE" = "2" ]]; then
cat << EOF

  -k, --keep-pending
     Optional.
     If specified, then currently pending (scheduled for execution) process rows will remain in the "pending" state.
     If this flag is not specified, then the default behavior is to transition all "pending" rows to a "disabled"
     state.  Once in a "disabled" state, they must be reviewed and manually transitioned to "pending" after CWS startup.
EOF
fi

cat << EOF

EXAMPLES:

(starting CWS in normal mode):
  ${0##*/}

(starting CWS in debug mode, using port 8000 as the JPDA port):
  ${0##*/} -d 8000
       or...
  ${0##*/} --debug 8000
---------------------------------------------------

EOF
}

# ---------------------------
# PARSE THE INPUT ARGUMENTS
#
# Keeping options in alphabetical order makes it easy to add more.
while :
do
    case "$1" in
      -d | --debug)
        DEBUG_PORT="$2"   # TODO: may want to check that this is an integer above a certain value
        shift 2
      ;;
      -k | --keep-scheduled)
        if [[ "$INSTALL_TYPE" = "1" || "$INSTALL_TYPE" = "2" ]]; then
          KEEP_PENDING_ROWS='true'
        else
          print "----------------------------------------------------------------------------------------------------"
          print "WARNING: Ignoring the --keep-scheduled flag, since that is only applicable to console installations!"
          print "----------------------------------------------------------------------------------------------------"
        fi
        shift 1
      ;;
      -h | --help)
        usage  # display usage information
        # no shifting needed here, we're done.
        exit 0
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

print "Starting CWS..."

# -----------------------
# CHECK JAVA REQUIREMENTS
#  JAVA_HOME must be set
#  Java must be 1.8x
# -----------------------
check_java_requirements

# ===============
# SETUP VARIABLES
# ===============
export CWS_HOME=${ROOT}

CWS_TOMCAT_HOME=${ROOT}/server/apache-tomcat-${TOMCAT_VER}
export CATALINA_HOME=${CWS_TOMCAT_HOME}

# CLEAR JAVA_OPTS FOR THIS SESSION,
# so any existing setting won't interfere
export JAVA_OPTS=

KEEP_PENDING_ROWS='false' # default behavior that we want to transition pending rows to disabled
CWS_INSTALLER_CONFIG_FILE=${ROOT}/configuration.properties

if [[ ! -f ${CWS_INSTALLER_CONFIG_FILE} ]]; then
    print "ERROR: Could not find the installation configuration file ${CWS_INSTALLER_CONFIG_FILE}"
    print "Please ensure configure.sh has been run prior to this script."
    exit 1
fi

INSTALL_TYPE=`grep install_type ${CWS_INSTALLER_CONFIG_FILE} | grep -v "#" | cut -d"=" -f 2`

print "CWS install type is ${INSTALL_TYPE}"

# ============================
# CHECK FOR EXISTING PROCESSES
# ============================
TOMCAT_PROC_COUNT=$(remaining_procs ${CWS_TOMCAT_HOME}/bin/bootstrap.jar)
LS_PROC_COUNT=$(remaining_procs ${ROOT}/server/logstash-${LOGSTASH_VER})
ES_PROC_COUNT=$(remaining_procs ${ROOT}/server/elasticsearch-${ELASTICSEARCH_VER})

if [[ ${TOMCAT_PROC_COUNT} -gt 0 ]] || [[ ${LS_PROC_COUNT} -gt 0 ]] || [[ ${ES_PROC_COUNT} -gt 0 ]]
then
	print "---------------------------------------------------------------------------"
	print "${TOMCAT_PROC_COUNT} ACTIVE TOMCAT PROCS    (${CWS_TOMCAT_HOME})"
	print "${LS_PROC_COUNT} ACTIVE LOGSTASH PROCS      (${ROOT}/server/logstash-${LOGSTASH_VER})"
	print "${ES_PROC_COUNT} ACTIVE ELASTICSEARCH PROCS (${ROOT}/server/elasticsearch-${ELASTICSEARCH_VER})"
	print
	print "+--------------------------------------------------------------+"
	print "| It appears that some CWS process(es) are still running.      |"
	print "| These must be stopped (run stop_cws.sh) before starting CWS. |"
	print "+--------------------------------------------------------------+"
	exit 1
fi

# =======================================================================
# Set CWS_KEEP_PENDING_ROWS_ON_STARTUP environment variable, so that CWS
# Console can use this information to appropriately modify rows in
# the database, when it starts up.
# =======================================================================
export CWS_KEEP_PENDING_ROWS_ON_STARTUP=${KEEP_PENDING_ROWS}

# FOR CONSOLE-ONLY or CONSOLE-AND-WORKER INSTALLATIONS,
if [[ "${INSTALL_TYPE}" = "1" ]] || [[ "${INSTALL_TYPE}" = "2" ]]; then
	# RUN SQL TO CREATE DATABASE IF NECESSARY.
	DB_HOST=`grep database_host ${CWS_INSTALLER_CONFIG_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_NAME=`grep database_name ${CWS_INSTALLER_CONFIG_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_USER=`grep database_user ${CWS_INSTALLER_CONFIG_FILE} | grep -v "^#" | cut -d"=" -f 2`
	DB_PASS=`grep database_password ${CWS_INSTALLER_CONFIG_FILE} | grep -v "^#" | cut -d"=" -f 2`

	echo "[mysql]" > ${ROOT}/config/my.cnf
	echo "host=${DB_HOST}" >> ${ROOT}/config/my.cnf
	echo "user=\"${DB_USER}\"" >> ${ROOT}/config/my.cnf
	echo "password=\"${DB_PASS}\"" >> ${ROOT}/config/my.cnf
	chmod 644 ${ROOT}/config/my.cnf

	print "Your database configuration is:"
	print "  DB HOST:   ${DB_HOST}"
	print "  DB NAME:   ${DB_NAME}"
	print "  DB USER:   ${DB_USER}"

    print "Checking whether database ${DB_NAME} already exists..."
    RES=`mysql --defaults-file=${ROOT}/config/my.cnf -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}'"`

    if [[ $? -gt 0 ]]; then
        print "ERROR: Problem checking for database."
        print "  Please check your database configuration, and try again."
        rm -rf ${ROOT}/config/my.cnf
        exit 1
    fi

    FOUND=`echo ${RES} | grep ${DB_NAME} | wc -l`
    if [[ ${FOUND} -eq 1 ]]; then
        print "  Database already exists."
    else
        print "ERROR: Database ${DB_NAME} could not be found, unable to start CWS."
        print "  Please run configure.sh to create the database and necessary tables."
        rm -rf ${ROOT}/config/my.cnf
        exit 1
    fi
fi

# =================
# AWS CONFIGURATION
# =================
if [[ -z "${AWS_PROFILE}" ]]; then
  AWS_PROFILE="Not set, using \"default\""
else
  AWS_PROFILE="${AWS_PROFILE}"
fi
print "Your AWS configuration is:"
print "  AWS PROFILE:   ${AWS_PROFILE}"

# =========
# START CWS
# =========
if [[ -z "$DEBUG_PORT" ]]; then
	print "Starting CWS application server in normal mode..."
	nohup ${CWS_TOMCAT_HOME}/bin/startup.sh
else
	export JPDA_ADDRESS=${DEBUG_PORT}
	export JPDA_TRANSPORT=dt_socket
	print "Starting CWS application server in debug mode (JPDA_ADRESS = ${JPDA_ADDRESS})..."
	nohup ${CWS_TOMCAT_HOME}/bin/catalina.sh jpda start
fi

# ===================
# START ELASTICSEARCH
# ===================
# Start ES only for console installations where user is not providing their own Elasticsearch
if [[ "${INSTALL_TYPE}" = "1" ]] || [[ "${INSTALL_TYPE}" = "2" ]]; then
    USER_ES=`grep ^user_provided_elasticsearch ${CWS_INSTALLER_CONFIG_FILE} | grep -v "#" | cut -d"=" -f 2`

    if [[ ${USER_ES} =~ $(echo "^(n|N)$") ]]; then
		nohup env -i ${ROOT}/launch_es.sh ${ELASTICSEARCH_VER} ${JAVA_HOME} > ${ROOT}/logs/cws_launch_es.log 2>&1 &

        if [[ $? -ne 0 ]]; then
            print "ERROR: Problem launching Elasticsearch. Please check the log under ${ROOT}/logs/cws_launch_es.log."
            exit 1
        fi
	else
	    print "Using user provided Elasticsearch..."
	fi
fi

# ==============
# START LOGSTASH
# ==============
# Start LS only for installations where user is not providing their own Logstash install
USER_LS=`grep ^user_provided_logstash ${CWS_INSTALLER_CONFIG_FILE} | grep -v "#" | cut -d"=" -f 2`

if [[ ${USER_LS} =~ $(echo "^(n|N)$") ]]; then
	nohup env -i ${ROOT}/launch_ls.sh ${LOGSTASH_VER} ${JAVA_HOME} > ${ROOT}/logs/cws_launch_ls.log 2>&1 &

	if [[ $? -ne 0 ]]; then
	    print "ERROR: Problem launching Logstash. Please check the log under ${ROOT}/logs/cws_launch_ls.log."
	    exit 1
	fi
else
	echo "Using user provided Logstash..."
fi

# ==============================
# WAIT FOR CWS SERVER TO COME UP
# ==============================
print "Waiting for CWS Server to startup..."
#print "  . = normal progress"
#print "  W = WARN"
#print "  E = ERROR"
#print "  e = Error"
#print "  x = Exception"
tail -f ${CWS_TOMCAT_HOME}/logs/catalina.out | while read LOGLINE
do
   printf "."
   #if [[ "${LOGLINE}" == *"WARN"* ]]; then
   #   printf "W"
   #fi
   #if [[ "${LOGLINE}" == *"ERROR"* ]]; then
   #   printf "E"
   #fi
   #if [[ "${LOGLINE}" == *"Error"* ]]; then
   #   printf "e"
   #fi
   #if [[ "${LOGLINE}" == *"Exception"* ]]; then
   #   printf "x"
   #fi
   [[ "${LOGLINE}" == *"org.apache.catalina.startup.Catalina.start Server startup in"* ]] && pkill -P $$ tail
done

echo ""
print "CWS Server Started."

# ==================================
# PROCESS TRIGGERS FOR STATUS TABLES
# ==================================
if [ "${INSTALL_TYPE}" = "1" ] || [ "${INSTALL_TYPE}" = "2" ]; then

    echo "Creating triggers for status tables..."
    mysql --defaults-file=${ROOT}/config/my.cnf ${DB_NAME} < ${ROOT}/sql/cws/core.afterstartup.sql
    while [[ $? -gt 0 ]]; do
        echo "Problem creating triggers for status tables. Please check your database configuration, and try again."
        rm -rf ${ROOT}/config/my.cnf
        exit 1
    done
    echo "  Done."

    rm -rf ${ROOT}/config/my.cnf
fi

# =======================
# REMOVE UNUSED LOG FILES
# =======================
rm `ls ${CWS_TOMCAT_HOME}/logs/*manager*`

if [[ "${INSTALL_TYPE}" = "3" ]]; then
	rm ${CWS_TOMCAT_HOME}/logs/localhost*
fi

print "Finished"
