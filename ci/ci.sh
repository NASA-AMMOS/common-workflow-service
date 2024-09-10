#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

USER=${2}
DB_TYPE=${3}
DB_HOST=${4}
DB_PORT=${5}
DB_NAME=${6}
DB_USER=${7}
DB_PASS=${8}

ES_PROTOCOL=${9}
ES_HOST=${10}
ES_INDEX_PREFIX=${11}
ES_PORT=${12}
ES_USE_AUTH=${13}
ES_USERNAME=${14}
ES_PASSWORD=${15}

CLOUD=${16}
SECURITY=${17}
HOSTNAME=${18}
EMAIL_LIST=${19}
ADMIN_FIRST=${20}
ADMIN_LAST=${21}
ADMIN_EMAIL=${22}
NUM_WORKERS=${23}
WORKER_ABANDONED_DAYS=${24}


source ${ROOT}/../utils.sh


# edits installation configuration file for a CWS worker
function worker_conf_data() {
  INSTALL_TYPE=${1}

  if [[ "${INSTALL_TYPE}" == "worker1" ]]; then
      sed -i '' -e 's/__WORKER_TYPE__/run_all/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_WEB_PORT__/37080/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_SSL_PORT__/37443/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_AJP_PORT__/37009/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_SHUTDOWN_PORT__/37005/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_JMX_PORT__/31098/' ${ROOT}/ci_worker.conf
  elif [[ "${INSTALL_TYPE}" == "worker2" ]]; then
      sed -i '' -e 's/__WORKER_TYPE__/run_models_only/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_WEB_PORT__/36080/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_SSL_PORT__/36443/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_AJP_PORT__/36009/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_SHUTDOWN_PORT__/36005/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_JMX_PORT__/31097/' ${ROOT}/ci_worker.conf
  elif [[ "${INSTALL_TYPE}" == "worker3" ]]; then
      sed -i '' -e 's/__WORKER_TYPE__/run_external_tasks_only/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_WEB_PORT__/33080/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_SSL_PORT__/33443/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_AJP_PORT__/33009/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_SHUTDOWN_PORT__/33005/' ${ROOT}/ci_worker.conf
      sed -i '' -e 's/__CWS_JMX_PORT__/31096/' ${ROOT}/ci_worker.conf
  else
      print "ERROR: UNEXPECTED INSTALL TYPE VALUE: '${INSTALL_TYPE}'"
      exit 1;
  fi
}

check_java_requirements

cat > ci_console.conf.template <<- EOF
cam_server_url=NA
cws_ldap_url_default=ldap://localhost:389
cam_ldap_identity_plugin_class=jpl.cws.core.identity.cam.CamLdapIdentityProviderPlugin
cam_ldap_security_filter_class=jpl.cws.core.web.CwsCamSecurityFilter
ldap_identity_plugin_class=org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin
ldap_security_filter_class=jpl.cws.core.web.CwsLdapSecurityFilter
camunda_security_filter_class=jpl.cws.core.web.CwsCamundaSecurityFilter
auto_accept_config=y
startup_autoregister_process_defs=false
cws_notification_emails=${EMAIL_LIST}
cws_token_expiration_hours=240
hostname=${HOSTNAME}
install_type=2
database_type=${DB_TYPE}
database_host=${DB_HOST}
database_port=${DB_PORT}
database_name=${DB_NAME}
database_username=${DB_USER}
database_password=${DB_PASS}
admin_user=${USER}
admin_firstname=${ADMIN_FIRST}
admin_lastname=${ADMIN_LAST}
admin_email=${ADMIN_EMAIL}
cws_web_port=38080
cws_ssl_port=38443
cws_ajp_port=38009
cws_shutdown_port=38005
amq_host=localhost
amq_port=31616
cws_amq_jmx_port=37099
cws_jmx_port=31099
identity_plugin_type=${SECURITY}
notify_users_email=y
email_subject=[CWS] You have been assigned a task (CWS_TASK_NAME)
email_body=fn:CWS_USER_FIRSTNAME<br/>ln:CWS_USER_LASTNAME,<br/>tn:(CWS_TASK_NAME), em:CWS_USER_EMAIL
brand_header=Test Console
project_webapp_root=proj
cws_enable_cloud_autoscaling=n
elasticsearch_protocol=${ES_PROTOCOL}
elasticsearch_host=${ES_HOST}
elasticsearch_index_prefix=${ES_INDEX_PREFIX}
elasticsearch_use_auth=${ES_USE_AUTH}
elasticsearch_port=${ES_PORT}
elasticsearch_username=${ES_USERNAME}
elasticsearch_password=${ES_PASSWORD}
smtp_hostname=smtp.localhost
default_smtp_hostname=smtp.localhost
default_cws_ldap_url=ldap://localhost:389
cws_ldap_url=ldap://ldapsearch:389
default_elasticsearch_use_auth=n
aws_cloudwatch_endpoint=monitoring.us-west-1.amazonaws.com
default_elasticsearch_index_prefix=cws-index
default_elasticsearch_port=9200
default_aws_cloudwatch_endpoint=monitoring.us-west-1.amazonaws.com
aws_sqs_dispatcher_msgFetchLimit=1
cws_console_host=localhost
default_cws_auth_scheme=CAMUNDA
default_history_level=full
default_shutdown_port=38005
metrics_publishing_interval=10
EOF


cat > ci_worker.conf.template <<- EOF
cam_server_url=NA
cws_ldap_url_default=ldap://localhost:389
cam_ldap_identity_plugin_class=jpl.cws.core.identity.cam.CamLdapIdentityProviderPlugin
cam_ldap_security_filter_class=jpl.cws.core.web.CwsCamSecurityFilter
ldap_identity_plugin_class=org.camunda.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin
ldap_security_filter_class=jpl.cws.core.web.CwsLdapSecurityFilter
camunda_security_filter_class=jpl.cws.core.web.CwsCamundaSecurityFilter
auto_accept_config=y
startup_autoregister_process_defs=false
cws_notification_emails=${EMAIL_LIST}
cws_token_expiration_hours=240
hostname=${HOSTNAME}
install_type=3
worker_type=__WORKER_TYPE__
database_type=${DB_TYPE}
database_host=${DB_HOST}
database_port=${DB_PORT}
database_name=${DB_NAME}
database_username=${DB_USER}
database_password=${DB_PASS}
admin_user=${USER}
admin_firstname=${ADMIN_FIRST}
admin_lastname=${ADMIN_LAST}
admin_email=${ADMIN_EMAIL}
cws_web_port=__CWS_WEB_PORT__
cws_ssl_port=__CWS_SSL_PORT__
cws_ajp_port=__CWS_AJP_PORT__
cws_shutdown_port=__CWS_SHUTDOWN_PORT__
cws_console_host=localhost
amq_host=localhost
cws_console_ssl_port=38443
amq_port=31616
cws_amq_jmx_port=37099
cws_jmx_port=__CWS_JMX_PORT__
identity_plugin_type=${SECURITY}
notify_users_email=y
email_subject=[CWS] You have been assigned a task (CWS_TASK_NAME)
email_body=fn:CWS_USER_FIRSTNAME<br/>ln:CWS_USER_LASTNAME,<br/>tn:(CWS_TASK_NAME), em:CWS_USER_EMAIL
elasticsearch_protocol=${ES_PROTOCOL}
elasticsearch_host=${ES_HOST}
elasticsearch_index_prefix=${ES_INDEX_PREFIX}
elasticsearch_use_auth=${ES_USE_AUTH}
elasticsearch_port=${ES_PORT}
elasticsearch_username=${ES_USERNAME}
elasticsearch_password=${ES_PASSWORD}
smtp_hostname=smtp.localhost
default_elasticsearch_index_prefix=cws-index
default_smtp_hostname=smtp.localhost
default_cws_ldap_url=ldap://localhost:389
cws_ldap_url=ldap://ldapsearch:389
project_webapp_root=
default_elasticsearch_use_auth=n
aws_cloudwatch_endpoint=monitoring.us-west-1.amazonaws.com
default_history_level=full
default_shutdown_port=38005
metrics_publishing_interval=10
EOF


# --------------------------------------------
# BUILD CODE AND CREATE DISTRIBUTION ARCHIVE
# --------------------------------------------
${ROOT}/ci_build.sh

DIST=${ROOT}/../dist
SERVER_DIST=cws_server.tar.gz

# -------------------
# CONFIGURE CONSOLE
# -------------------
print "Preparing console installation files..."
mkdir ${DIST}/console-only
tar --directory=${DIST}/console-only -zxf ${DIST}/${SERVER_DIST}

print "Configuring console installation......"
cp ${ROOT}/ci_console.conf.template ${ROOT}/ci_console.conf
cp ${ROOT}/ci_worker.conf.template ${ROOT}/ci_worker.conf
sed -ie "s^__CONSOLE_HOST__^${THIS_HOSTNAME}^g" ${ROOT}/ci_console.conf
sed -ie "s^__WORKER_HOST__^${THIS_HOSTNAME}^g"  ${ROOT}/ci_worker.conf
sed -ie "s^__CONSOLE_HOST__^${THIS_HOSTNAME}^g" ${ROOT}/ci_worker.conf
rm -f ${ROOT}/ci_worker.confe
rm -f ${ROOT}/ci_console.confe

${DIST}/console-only/cws/configure.sh ${ROOT}/ci_console.conf Y

if [[ $? -gt 0 ]]; then
	print "FAILED CONFIGURATION OF CONSOLE!"
	exit 1
fi

print "Done configuring console installation."

# -------------
# START CONSOLE
# -------------
BASE_PORT=8000
${DIST}/console-only/cws/start_cws.sh -d $BASE_PORT

# AMQ must be started before configuring workers
print "Waiting for console startup..."
sleep 120

# -------------------------
# CONFIGURE & START WORKERS
# -------------------------
if [[ -z "$NUM_WORKERS" ]]; then
	NUM_WORKERS=1
fi

for ((WORKER_NUM=1; WORKER_NUM <= $NUM_WORKERS; WORKER_NUM++)); do
	print "Preparing worker $WORKER_NUM installation files...";
	WORKER_TAG="worker${WORKER_NUM}"
	mkdir -p ${DIST}/${WORKER_TAG}
	tar --directory=${DIST}/${WORKER_TAG} -zxf ${DIST}/${SERVER_DIST}

	echo "Configuring worker $WORKER_NUM installation..."
	worker_conf_data $WORKER_TAG
  ${DIST}/${WORKER_TAG}/cws/configure.sh ${ROOT}/ci_worker.conf Y
  cp ${ROOT}/ci_worker.conf.template ${ROOT}/ci_worker.conf

	if [[ $? -gt 0 ]]; then
  	echo "FAILED CONFIGURATION OF WORKER $WORKER_NUM!"
  	exit 1
  fi

  sleep 5

  print "Starting worker ${WORKER_NUM}..."
  WORKER_PORT=$((BASE_PORT + WORKER_NUM))
  ${DIST}/${WORKER_TAG}/cws/start_cws.sh -d $WORKER_PORT

  print "Finished."
done
