FROM centos:7
USER root
RUN yum update -y && \
yum install -y sudo which mariadb-server mariadb-client java-1.8.0-openjdk java-1.8.0-openjdk-devel ntp && \
yum clean all

#TODO Update this because it might break later
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0

ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN ntpdate -s time.nist.gov

RUN useradd -ms /bin/bash cws_user && echo "cws_user:docker" | chpasswd && usermod -aG wheel cws_user

RUN echo "root:docker" | chpasswd

WORKDIR /home/cws_user

ADD cws_server.tar.gz .
ADD startup.sh .
ADD wait_for_mariadb.sh .

# For time check
ADD getTime.java .
ADD joda-time-2.1.jar .

RUN chown -R cws_user:cws_user cws

# NTP Port
EXPOSE 123/udp

USER cws_user

ENTRYPOINT [ "./wait_for_mariadb.sh" ]
