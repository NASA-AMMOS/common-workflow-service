FROM oraclelinux:8

RUN yum update -y && \
    yum install -y mysql java-17-openjdk java-17-openjdk-devel rsync which curl && \
    yum clean all

ENV JAVA_HOME /usr/lib/jvm/java-openjdk

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /home/cws_user

ADD cws_server.tar.gz .
ADD startup.sh .
ADD wait_for_db_es_console.sh .
ADD setup_testing_environment.sh .
ADD testing_entrypoint.sh .

# For time check
ADD getTime.java .
ADD joda-time-2.1.jar .

# Copy certificate generation scripts from the build context
COPY cws-certs /opt/cws-certs

# Download logstash for CWS installer
RUN curl -o /home/cws_user/cws/server/logstash-8.12.0.zip https://artifacts.elastic.co/downloads/logstash/logstash-8.12.0-windows-x86_64.zip

# Ensure scripts are executable
RUN chmod +x /opt/cws-certs/generate-certs-testing.sh && \
    chmod +x setup_testing_environment.sh && \
    chmod +x testing_entrypoint.sh && \
    chmod +x wait_for_db_es_console.sh

ENTRYPOINT [ "./wait_for_db_es_console.sh" ]
