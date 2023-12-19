#!/bin/bash
# --------
# utils.sh
# --------
# Shell utility functions and environment settings used throughout CWS setup scripts

# Update versions as necessary
export CWS_VER='2.5.0'    # update this each CWS release
export CAMUNDA_VER='7.20.0'
export TOMCAT_VER='9.0.75'
export LOGSTASH_VER='8.8.0'

# Prints the provided string, tagging with the script that called it
function print () {
    echo "[`basename ${0}`] ${1}"
}

# Script from :  https://gist.github.com/bobthecow/757788
#
# Usage:
#     tab                   Opens the current directory in a new tab
#     tab [PATH]            Open PATH in a new tab
#     tab [CMD]             Open a new tab and execute CMD
#     tab [PATH] [CMD] ...  You can prob'ly guess
#
function tab () {
    local cmd=""
    local cdto="$PWD"
    local args="$@"

    if [[ -d "$1" ]]; then
        cdto=`cd "$1"; pwd`
        args="${@:2}"
    fi

    if [[ -n "$args" ]]; then
        cmd="; $args"
    fi

    if [[ $cdto == *"console"* ]]
    then
        tab_title="---CONSOLE---"
    else
        tab_title="---WORKER---"
    fi

osascript <<-EOF
  tell application "iTerm2"
    tell current window
      -- These commands return a tab
      set newTab to (create tab with default profile)
      tell newTab
        tell current session
          set name to "$tab_title"
          write text "cd \"$cdto\"$cmd"
        end tell
      end tell
    end tell
  end tell
EOF

}

# Prompts the user for Y/N to determine if script execution should continue or not
function prompt_to_continue () {
	CONTINUE=''
	PROMPT="${1}"

	while [[ ! ${CONTINUE} =~ ^(y|Y|n|N)$ ]]; do
		read -p "${PROMPT}" CONTINUE
		if [[ ${CONTINUE} =~ ^(y|Y|n|N)$ ]]
		then
			break  # Skip entire rest of loop.
		fi
		print "  ERROR: Must specify either 'Y' or 'N'.\n\n";
	done

	if [[ ${CONTINUE} =~ ^(n|N)$ ]]; then
		exit 1
	fi
}

# Returns the remaining running processes for the provided process name pattern
function remaining_procs () {
    PROC_PATTERN=${1}

    echo `ps -ef | grep ${PROC_PATTERN} | grep -v grep | wc -l | xargs`
}

# Attempts to kill a set of processes based on a pattern, first gracefully (via SIGTERM), then forcefully (via SIGKILL)
function kill_proc () {
    PROC_NAME=${1}
    PROC_KILL_PATTERN=${2}

    if [[ "$OSTYPE" == "darwin"* ]]; then
        PKILL_OPTS="-i -f ${PROC_KILL_PATTERN}"
    else
        PKILL_OPTS="-f ${PROC_KILL_PATTERN}"
    fi

    PROC_COUNT=$(remaining_procs ${PROC_KILL_PATTERN})

    if [[ ${PROC_COUNT} -gt 0 ]]; then
        print "Stopping ${PROC_NAME} gracefully (procs that match '${PROC_KILL_PATTERN}')...";
        pkill ${PKILL_OPTS}

        if [[ $? -gt 0 ]] || [[ $(remaining_procs ${PROC_KILL_PATTERN}) -gt 0 ]]; then
            print "Stopping ${PROC_NAME} forcefully (procs that match '${PROC_KILL_PATTERN}')...";
            pkill -9 ${PKILL_OPTS}

            if [[ $? -gt 0 ]] || [[ $(remaining_procs ${PROC_KILL_PATTERN}) -gt 0 ]]; then
                print "WARNING: Failed to forcefully stop ${PROC_NAME}, process should be killed manually."
            else
                print "${PROC_NAME} stopped forcefully."
            fi
        else
            print "${PROC_NAME} stopped gracefully."
        fi
    fi
}

# Dumps a listing of the currently running java applications to stdout
function dump_java_apps () {
    print "Dumping java apps..."
    ps -ef | grep java
    print "Done dumping java apps!!!"
}

# Runs a number of checks to see if Java is properly installed and at a compatible version for CWS
function check_java_requirements () {
    print "Checking Java requirements..."
    if [[ -z ${JAVA_HOME} ]]; then
        print "  ERROR: CWS requires JAVA_HOME be set."
        print "  JAVA_HOME should be set to the JDK on your home system."
        exit 1
    else
        print "  JAVA_HOME set  [OK]"
        print "  JAVA_HOME = ${JAVA_HOME}"
    fi

    JAVA_HOME_VERSION=$("${JAVA_HOME}/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print "  JAVA_HOME Java version : ${JAVA_HOME_VERSION}"

    JAVA_PATH_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    print "  PATH      Java version : ${JAVA_PATH_VERSION}  $(which java)"

    if [[ "${JAVA_PATH_VERSION}" == "${JAVA_PATH_VERSION}" ]]; then
        print "  Java versions match      [OK]"
    else
        print "  ERROR: Java versions don't match."
        print "  Please ensure your JAVA_HOME java is on your PATH environment variable."
        exit 1
    fi

    if [[ "${JAVA_PATH_VERSION}" > "17" && "${JAVA_PATH_VERSION}" < "18" ]]; then
        print "  Java version == 17x     [OK]"
    else
        print "  ERROR: Java version is ${JAVA_PATH_VERSION}. CWS only supports Java version 12x."
        exit 1
    fi

    JAVAC_EXISTS=`stat ${JAVA_HOME}/bin/javac &> /dev/null;echo $?`

    if [[ "${JAVAC_EXISTS}" == "0" ]]; then
        print "  Java Compiler available  [OK]"
    else
        print "  ERROR: No Java compiler (javac) found."
        print "  Please make sure you are using a JAVA_HOME that is a JDK, NOT a JRE."
        print "  Make sure your path is set correctly."
        print "  For example, if running under bash:"
        print "  (in ~/.bash_profile)"
        print "     export PATH=\$JAVA_HOME\/bin:\$PATH"
        exit 1
    fi

    print "Java requirements met."
}

# Creates a complete installation configuration file for a CWS console/worker to streamline development installs.
function auto_conf_data () {
    INSTALL_TYPE=${1}

    # VALUES SET FROM dev.sh
    ROOT=${2}
    LDAP_USERNAME=${3}
    DB_TYPE=${4}
    DB_HOST=${5}
    DB_PORT=${6}
    DB_NAME=${7}
    DB_USER=${8}
    DB_PASS=${9}
    ES_PROTOCOL=${10}
    ES_HOST=${11}
    ES_PORT=${12}
    ES_USE_AUTH=${13}
    ES_USERNAME=${14}
    ES_PASSWORD=${15}
    ENABLE_CLOUD_AS=${16}
    SECURITY_SCHEME=${17}
    THIS_HOSTNAME=${18}
    NOTIFICATION_EMAILS=${19}
    ADMIN_FIRSTNAME=${20}
    ADMIN_LASTNAME=${21}
    ADMIN_EMAIL=${22}
    NUM_WORKERS=${23}
    WORKER_MAX_NUM_RUNNING_PROCS=${24}
    WORKER_ABANDONED_DAYS=${25}

    OUTPUT_FILE=${26}

    source ${ROOT}/utils.sh

    LDAP_SERVER_URL="ldap://localhost:389"

    CWS_CONSOLE_SSL_PORT=38443
    AMQ_PORT=31616

    if [[ "${INSTALL_TYPE}" == "console-and-worker" ]]; then
        INSTALL_TYPE_CODE=1
        WORKER_TYPE="run_all"
        CWS_WEB_PORT=38080
        CWS_SSL_PORT=38443
        CWS_AJP_PORT=38009
        CWS_SHUTDOWN_PORT=38005
        CWS_AMQ_JMX_PORT=37099
        CWS_JMX_PORT=31099
    elif [[ "${INSTALL_TYPE}" == "console-only" ]]; then
        INSTALL_TYPE_CODE=2
        WORKER_TYPE="run_all"
        CWS_WEB_PORT=38080
        CWS_SSL_PORT=38443
        CWS_AJP_PORT=38009
        CWS_SHUTDOWN_PORT=38005
        CWS_AMQ_JMX_PORT=37099
        CWS_JMX_PORT=31099
    elif [[ "${INSTALL_TYPE}" == "worker1" ]]; then
        INSTALL_TYPE_CODE=3
        WORKER_TYPE="run_all"
        CWS_WEB_PORT=37080
        CWS_SSL_PORT=37443
        CWS_AJP_PORT=37009
        CWS_SHUTDOWN_PORT=37005
        CWS_AMQ_JMX_PORT=37099
        CWS_JMX_PORT=31098
    elif [[ "${INSTALL_TYPE}" == "worker2" ]]; then
        INSTALL_TYPE_CODE=3
        WORKER_TYPE="run_models_only"
        CWS_WEB_PORT=36080
        CWS_SSL_PORT=36443
        CWS_AJP_PORT=36009
        CWS_SHUTDOWN_PORT=36005
        CWS_AMQ_JMX_PORT=37099
        CWS_JMX_PORT=31097
    elif [[ "${INSTALL_TYPE}" == "worker3" ]]; then
        INSTALL_TYPE_CODE=3
        WORKER_TYPE="run_external_tasks_only"
        CWS_WEB_PORT=33080
        CWS_SSL_PORT=33443
        CWS_AJP_PORT=33009
        CWS_SHUTDOWN_PORT=33005
        CWS_AMQ_JMX_PORT=37099
        CWS_JMX_PORT=31096
    else
        print "ERROR: UNEXPECTED INSTALL TYPE VALUE: '${INSTALL_TYPE}'"
        exit 1;
    fi

    print ${OUTPUT_FILE}

    cat > ${OUTPUT_FILE} <<- EOF
    hostname=${THIS_HOSTNAME}
    install_type=${INSTALL_TYPE_CODE}
    worker_type=${WORKER_TYPE}
    database_type=${DB_TYPE}
    database_host=${DB_HOST}
    database_port=${DB_PORT}
    database_name=${DB_NAME}
    database_username=${DB_USER}
    database_password=${DB_PASS}
    elasticsearch_protocol=${ES_PROTOCOL}
    elasticsearch_host=${ES_HOST}
    elasticsearch_port=${ES_PORT}
    elasticsearch_use_auth=${ES_USE_AUTH}
    elasticsearch_username=${ES_USERNAME}
    elasticsearch_password=${ES_PASSWORD}
    admin_user=${LDAP_USERNAME}
    admin_firstname=${ADMIN_FIRSTNAME}
    admin_lastname=${ADMIN_LASTNAME}
    admin_email=${ADMIN_EMAIL}
    cws_console_host=${THIS_HOSTNAME}
    cws_console_ssl_port=${CWS_CONSOLE_SSL_PORT}
    cws_web_port=${CWS_WEB_PORT}
    cws_ssl_port=${CWS_SSL_PORT}
    cws_ajp_port=${CWS_AJP_PORT}
    cws_shutdown_port=${CWS_SHUTDOWN_PORT}
    amq_host=${THIS_HOSTNAME}
    amq_port=${AMQ_PORT}
    cws_amq_jmx_port=${CWS_AMQ_JMX_PORT}
    cws_jmx_port=${CWS_JMX_PORT}
    history_days_to_live=1
    worker_max_num_running_procs=${WORKER_MAX_NUM_RUNNING_PROCS}
    worker_abandoned_days=${WORKER_ABANDONED_DAYS}
    notify_users_email=y
    email_subject=[CWS] You have been assigned a task (CWS_TASK_NAME)
    email_body=fn:CWS_USER_FIRSTNAME<br/>ln:CWS_USER_LASTNAME,<br/>tn:(CWS_TASK_NAME), em:CWS_USER_EMAIL
    brand_header=Test Console
    project_webapp_root=proj
    cws_enable_cloud_autoscaling=${ENABLE_CLOUD_AS}
    identity_plugin_type=$(echo ${SECURITY_SCHEME} | tr '[:lower:]' '[:upper:]')
    cws_ldap_url=${LDAP_SERVER_URL}
    cws_ldap_url_default=${LDAP_SERVER_URL}
    ldap_identity_plugin_class=org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin
    ldap_security_filter_class=jpl.cws.core.web.CwsLdapSecurityFilter
    camunda_security_filter_class=jpl.cws.core.web.CwsCamundaSecurityFilter
    auto_accept_config=y
    startup_autoregister_process_defs=false
    cws_notification_emails=${NOTIFICATION_EMAILS}
    cws_token_expiration_hours=240
    user_provided_logstash=n
EOF
}
