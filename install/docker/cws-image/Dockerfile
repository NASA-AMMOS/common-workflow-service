FROM centos:7

RUN yum update -y && \
    yum install -y mariadb-server mariadb-client java-1.8.0-openjdk java-1.8.0-openjdk-devel && \
    yum clean all

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN useradd -ms /bin/bash cws_user && usermod -aG wheel cws_user

WORKDIR /home/cws_user

ADD cws_server.tar.gz .
ADD startup.sh .
ADD wait_for_mariadb.sh .

# For time check
ADD getTime.java .
ADD joda-time-2.1.jar .

RUN chown -R cws_user:cws_user cws

USER cws_user

ENTRYPOINT [ "./wait_for_mariadb.sh" ]
