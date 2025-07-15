FROM oraclelinux:8

RUN yum update -y && \
    yum install -y mysql java-17-openjdk java-17-openjdk-devel rsync which && \
    yum clean all

ENV JAVA_HOME /usr/lib/jvm/java-openjdk

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /home/cws_user

ADD cws_server.tar.gz .
ADD startup.sh .
ADD wait_for_db_es_console.sh .

# For time check
ADD getTime.java .
ADD joda-time-2.1.jar .

ENTRYPOINT [ "./wait_for_db_es_console.sh" ]
