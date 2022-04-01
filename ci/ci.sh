#!/bin/bash

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Create a file that stores local hostname
echo "$THIS_HOSTNAME" > ${ROOT}/../cws-test/src/test/resources/hostname.txt

USER=${2}
DB_TYPE=${3}
DB_HOST=${4}
DB_PORT=${5}
DB_NAME=${6}
DB_USER=${7}
DB_PASS=${8}

ES_HOST=${9}
ES_PORT=${10}
ES_USE_AUTH=${11}
ES_USERNAME=${12}
ES_PASSWORD=${13}

CLOUD=${14}
SECURITY=${15}
HOSTNAME=${16}
EMAIL_LIST=${17}
ADMIN_FIRST=${18}
ADMIN_LAST=${19}
ADMIN_EMAIL=${20}
NUM_WORKERS=${21}
WORKER_ABANDONED_DAYS=${22}


source ${ROOT}/../utils.sh


cat > ci_console.conf.template <<- EOF
cam_server_url=NA
cws_ldap_url_default=ldaps://localhost:636
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
elasticsearch_host=${ES_HOST}
elasticsearch_use_auth=${ES_USE_AUTH}
elasticsearch_port=${ES_PORT}
elasticsearch_username=${ES_USERNAME}
elasticsearch_password=${ES_PASSWORD}
smtp_hostname=smtp.localhost
default_smtp_hostname=smtp.localhost
default_cws_ldap_url=ldaps://localhost:636
cws_ldap_url=ldaps://localhost:636
default_elasticsearch_use_auth=n
aws_cloudwatch_endpoint=monitoring.us-west-1.amazonaws.com
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
cws_ldap_url_default=ldaps://localhost:636
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
cws_web_port=37080
cws_ssl_port=37443
cws_ajp_port=37009
cws_shutdown_port=37005
cws_console_host=localhost
amq_host=localhost
cws_console_ssl_port=38443
amq_port=31616
cws_amq_jmx_port=37099
cws_jmx_port=31098
identity_plugin_type=${SECURITY}
notify_users_email=y
email_subject=[CWS] You have been assigned a task (CWS_TASK_NAME)
email_body=fn:CWS_USER_FIRSTNAME<br/>ln:CWS_USER_LASTNAME,<br/>tn:(CWS_TASK_NAME), em:CWS_USER_EMAIL
elasticsearch_host=${ES_HOST}
elasticsearch_use_auth=${ES_USE_AUTH}
elasticsearch_port=${ES_PORT}
elasticsearch_username=${ES_USERNAME}
elasticsearch_password=${ES_PASSWORD}
smtp_hostname=smtp.localhost
default_smtp_hostname=smtp.localhost
default_cws_ldap_url=ldaps://localhost:636
cws_ldap_url=ldaps://localhost:636
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
${DIST}/console-only/cws/start_cws.sh -d 8000

# AMQ must be started before configuring workers
print "Waiting for console startup..."
sleep 120

# -------------------
# CONFIGURE WORKER 1
# -------------------
print "Preparing worker 1 installation files..."
mkdir ${DIST}/worker1
tar --directory=${DIST}/worker1 -zxf ${DIST}/${SERVER_DIST}

echo "Configuring worker 1 installation..."
${DIST}/worker1/cws/configure.sh ${ROOT}/ci_worker.conf Y

if [[ $? -gt 0 ]]; then
	echo "FAILED CONFIGURATION OF WORKER 1!"
	exit 1
fi

# ---------------
# START WORKER 1
# ---------------
${DIST}/worker1/cws/start_cws.sh -d 8001

print "Finished."
