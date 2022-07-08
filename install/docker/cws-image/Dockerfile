FROM oraclelinux:8
#FROM arm64v8/oraclelinux:8    # Use this image for Mac M1/M2 arm64

RUN yum update -y && \
    yum install -y mysql java-1.8.0-openjdk java-1.8.0-openjdk-devel rsync which && \
    yum clean all

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0

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
