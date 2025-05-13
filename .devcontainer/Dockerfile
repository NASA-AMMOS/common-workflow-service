FROM ubuntu:latest
ARG NODE_VERSION=20

# Install required dependencies for development
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    curl \
    unzip \
    zip \
    git \
    netcat-openbsd \
    rsync \
    mariadb-client \
    iproute2 \
    dnsutils \
    iputils-ping \
    expect \
    traceroute \
    chrony \
    sudo \
    vim \
    wget \
    # Additional dev tools
    bash-completion \
    less \
    lsof \
    htop \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Set Java environment
RUN echo "JAVA_HOME=/usr/lib/jvm/java-17-openjdk-$(dpkg --print-architecture)" >> /root/.bashrc
ENV PATH=${JAVA_HOME}/bin:$PATH

# install nvm
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash

# set env
ENV NVM_DIR=/root/.nvm

# install node
RUN bash -c "source $NVM_DIR/nvm.sh && nvm install $NODE_VERSION"

# Setup Maven with settings.xml
RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/
ENV MAVEN_CONFIG="/root/.m2"

WORKDIR /workspaces/common-workflow-service

# Setup environment variables (may be overridden by docker-compose)
ENV DB_TYPE=mariadb \
    DB_NAME=cws \
    NUM_WORKERS=1 \
    WORKER_MAX_NUM_RUNNING_PROCS=16 \
    WORKER_ABANDONED_DAYS=1 \
    HOSTNAME=localhost \
    KEYSTORE_PASSWORD=changeit \
    TZ=America/Los_Angeles

# set ENTRYPOINT for reloading nvm-environment
ENTRYPOINT ["bash", "-c", "source $NVM_DIR/nvm.sh && exec \"$@\"", "--"]

# set cmd to bash
CMD ["/bin/bash"]