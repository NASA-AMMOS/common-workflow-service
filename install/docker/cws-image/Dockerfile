FROM oraclelinux:8

RUN yum update -y && \
    yum install -y mysql java-17-openjdk java-17-openjdk-devel rsync which && \
    yum clean all

ENV JAVA_HOME /usr/lib/jvm/java-openjdk
ENV HOME /root

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /home/cws_user

ADD cws_server.tar.gz .
ADD startup.sh .
ADD wait_for_db_es_console.sh .

RUN mkdir -p /home/cws_user/.cws && echo "changeit" > /home/cws_user/.cws/creds && \
    mkdir -p /root/.cws && echo "changeit" > /root/.cws/creds

# For time check
ADD getTime.java .
ADD joda-time-2.1.jar .

ENTRYPOINT [ "./wait_for_db_es_console.sh" ]
