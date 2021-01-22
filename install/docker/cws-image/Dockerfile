#FROM registry.access.redhat.com/rhscl/mariadb-101-rhel7:10.1-24
#FROM registry.access.redhat.com/rhel:latest
#FROM registry.access.redhat.com/rhel7
FROM centos:7
USER root
RUN yum update -y && \
yum install -y sudo which mariadb-server mariadb-client java-1.8.0-openjdk java-1.8.0-openjdk-devel ntp && \
yum clean all

#TODO Update this because it might break later
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0
#RUN export JAVA_HOME=$(cd /usr/lib/jvm/java*x86_64 && pwd)

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
#RUN cp -p /usr/share/zoneinfo/America/Los_Angeles /etc/localtime

#ADD ntpd.conf /etc/ntpd.conf

RUN ntpdate -s ntp.jpl.nasa.gov

RUN echo "* soft nproc 65536" >> /etc/security/limits.conf
RUN echo "* hard nproc 65536" >> /etc/security/limits.conf
RUN echo "* soft nofile 65536" >> /etc/security/limits.conf
RUN echo "* hard nofile 65536" >> /etc/security/limits.conf
RUN echo "fs.file-max = 65536" >> /etc/sysctl.conf

RUN echo "vm.max_map_count=262144" >> /etc/sysctl.conf

RUN useradd -ms /bin/bash cws_user && echo "cws_user:docker" | chpasswd && usermod -aG wheel cws_user

RUN echo "root:docker" | chpasswd

#*               soft    core            0
#*               hard    rss             10000


WORKDIR /home/cws_user

ADD cws_server.tar.gz .
#ADD config_console-and-worker.properties .
ADD startup.sh .

ADD wait_for_mariadb.sh .

ADD getTime.java .
ADD joda-time-2.1.jar .

RUN chown -R cws_user:cws_user cws

# ntp port
EXPOSE 123/udp

#EXPOSE 9200
#EXPOSE 9300

USER cws_user

ENTRYPOINT [ "./wait_for_mariadb.sh" ]
