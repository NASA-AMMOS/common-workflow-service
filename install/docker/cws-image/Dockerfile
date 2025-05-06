FROM oraclelinux:8

RUN yum update -y && \
    yum install -y mysql java-17-openjdk java-17-openjdk-devel rsync which wget tar gzip && \
    yum clean all

ENV JAVA_HOME /usr/lib/jvm/java-openjdk

# Install Maven
ENV MAVEN_VERSION 3.9.6
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "/home/cws_user/.m2"

RUN mkdir -p /usr/share/maven && \
    wget https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -P /tmp && \
    tar -xzf /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /usr/share/maven --strip-components=1 && \
    rm -f /tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV PATH=${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}


ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /home/cws_user

ADD cws_server.tar.gz .
ADD startup.sh .
ADD wait_for_db_es_console.sh .
ADD setup_test_env.sh .

# For time check
ADD getTime.java .
ADD joda-time-2.1.jar .

# Copy certificate generation scripts from the build context
COPY cws-certs /opt/cws-certs

# Ensure the specific script is executable
RUN chmod +x /opt/cws-certs/generate-certs.sh
RUN curl -o /home/cws_user/cws/server/logstash-8.12.0.zip https://artifacts.elastic.co/downloads/logstash/logstash-8.12.0-windows-x86_64.zip

ENTRYPOINT [ "./wait_for_db_es_console.sh" ]
