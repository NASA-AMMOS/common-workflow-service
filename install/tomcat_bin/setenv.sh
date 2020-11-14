#
# DEV CONFIGURATION
#
#export CATALINA_OPTS="-Xmx8g -server
#-XX:+HeapDumpOnOutOfMemoryError
#-javaagent:__CWS_TOMCAT_ROOT__/lib/org.jacoco.agent-0.8.2-runtime.jar=destfile=__CWS_TOMCAT_ROOT__/jacoco/jacoco.exec,append=false
#-Dcom.sun.management.jmxremote
#-Dcom.sun.management.jmxremote.port=__CWS_JMX_PORT__
#-Dcom.sun.management.jmxremote.authenticate=false
#-Dcom.sun.management.jmxremote.ssl=false
#-Djava.rmi.server.hostname=localhost
#-Djava.net.preferIPv4Stack=true
#-Djavax.net.ssl.trustStore=__CWS_TOMCAT_ROOT__/lib/cws_truststore.jks"

#
# RELEASE CONFIGURATION
#
#export CATALINA_OPTS="-Xmx8g -server
#-XX:+HeapDumpOnOutOfMemoryError
#-Dcom.sun.management.jmxremote
#-Dcom.sun.management.jmxremote.port=__CWS_JMX_PORT__
#-Dcom.sun.management.jmxremote.authenticate=false
#-Dcom.sun.management.jmxremote.ssl=false
#-Djava.rmi.server.hostname=localhost
#-Djava.net.preferIPv4Stack=true
#-Djavax.net.ssl.trustStore=__CWS_TOMCAT_ROOT__/lib/cws_truststore.jks"


export CATALINA_OPTS="-Xmx8g -server -XX:+HeapDumpOnOutOfMemoryError -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=__CWS_JMX_PORT__ -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost -Djava.net.preferIPv4Stack=true -Djavax.net.ssl.trustStore=__CWS_TOMCAT_ROOT__/lib/cws_truststore.jks"

