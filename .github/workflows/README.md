# CWS Continuous Integration (CI) Pipeline

The following workflows establish continuous integration and continuous delivery (CI/CD) pipelines for this repository containing the open-source release of the [NASA-AMMOS](https://ammos.nasa.gov/) Common Workflow Service (CWS).

Utilizing GitHub Actions, the workflows build, test, and deliver CWS by configuring steps required to accomplish these tasks, making use of Marketplace Actions in the process. There are currently two CI pipelines: [CWS CI Camunda](https://github.com/NASA-AMMOS/common-workflow-service/actions/workflows/maven.yml) and [CWS CI LDAP](https://github.com/NASA-AMMOS/common-workflow-service/actions/workflows/ldap.yml).

## CWS CI Camunda Workflow

The CWS CI Camunda workflow is triggered upon any push to the repository, including commits, pull requests, and merges. The workflow is also scheduled to run every day at 5 AM PST / 12 PM UTC. It is composed of two jobs: `build-and-test-cws` and `publish-cws-image`. Both jobs run on separate GitHub runners with the latest version of Ubuntu.

The `build-and-test-cws` job performs all the necessary preliminary steps required to configure and build CWS with Camunda security. Once these steps are completed, a fully-functioning instance of CWS is built. Following the build, unit and integration tests are run, with resulting artifacts being saved to the workflow run. Afterwards, data from the workflow is sent to the development team.

The `publish-cws-image` job is the CD component of the workflow, triggered upon a commit message containing the word "version," and dependant upon successful completion of the `build-and-test-cws job`.

### build-and-test-cws Job

- [**checkout**](https://github.com/marketplace/actions/checkout): This action checks out the repository under `$GITHUB_WORKSPACE`, so the workflow can access it.
- Set up JDK 8:
  - [**setup-java**](https://github.com/marketplace/actions/setup-java-jdk): This action downloads and sets up a requested version of Java
  - Current configuration:
      - Java-version: 8
      - Distribution: Temurin
      - Cache: Maven
- **Create open-source certs**:
    - Runs the `generate-certs.sh` bash script
    - Creates and stores keystore and truststore required to access CWS UI
- **Download Logstash**:
  - [**download-file-action**](https://github.com/marketplace/actions/download-file-to-workspace): This action downloads a file from the internet into the workspace
    - Downloads Logstash using a URL
    - Renames the file as `logstash-7.16.2.zip`
    - Stores Logstash in appropriate directory
- **Check for Logstash**:
  - List files in the directory where Logstash is expected
  - Can be used to verify successful download of Logstash
- **Set up Elasticsearch**:
  - Creates and starts a Docker container with Elasticsearch
  - Runs in the background (detached mode)
- **Set up CWS database using Docker**:
  - Creates and starts a Docker container with MariaDB database instance for CWS
  - Runs in the background (detached mode)
  - Uses port 3306:3306, with name `cws_dev`
- **Show Docker containers**: Shows all Docker containers and their statuses
- **Build CWS**:
  - Builds and runs CWS
  - Begins by running the first bash script in the build process: run_ci.sh
  - The bash script is passed the `SECURITY` environmental variable to run CWS in a specific security mode
  - The CWS security mode for this workflow is `CAMUNDA`
- **Show CWS Log**:
  - List files in the directory where `cws.log` is expected
  - The presence of the file verifies successful deployment of the Apache Tomcat server
- **Set up Google Chrome**
  - Installs or updates the latest stable version of Google Chrome
  - Required for Selenium-based integration tests
- **Run Unit Tests**: Runs the JUnit-based unit tests with Jacoco code coverage
- **Run Integration Tests**: Runs the Selenium-based integration tests with Jacoco code coverage
- **Upload Jacoco report**:
  - [**upload-artifact**](https://github.com/marketplace/actions/upload-a-build-artifact): This action uploads artifacts from the workflow
  - Uploads the Jacoco code coverage reports as artifacts to the workflow run
- **Upload test screenshots**: Uploads the test screenshots produced during the integration tests as artifacts to the workflow run
- **Send custom JSON data to Slack workflow**:
  - [**slack-send**](https://github.com/marketplace/actions/slack-send): This action sends data into a Slack channel
    - Utilizes Technique 1 - Slack Workflow Builder
    - Sends GitHub Actions workflow data to a Slack channel via a webhook URL
    - Requires a Slack workflow using webhooks to be created

### publish-cws-image Job
- **Check out the repo**:
  - Utilizes the same `checkout` action to check out the repository again
  - This is done in a new GitHub runner
- **Set up JDK 8**
  - Utilizes the same `setup-java` action to set up Java
- **Log in to Docker Hub**:
  - [**Docker Login**](https://github.com/marketplace/actions/docker-login): This action is used to log in against a Docker registry
- **Generate CWS Docker image**
  - Runs the `./build.sh` bash script
  - Generates and tags a CWS Docker image
- **Re-Tag CWS Docker image for open source**
  - Re-tags the CWS Docker image created from previous step
  - Necessary to publish image on an open source Docker repository
- **Push CWS Docker image**: Publishes CWS Docker image to open source Docker repository

## CWS CI LDAP Workflow

The CWS CI LDAP workflow is triggered upon any push to the repository, including commits, pull requests, and merges. The workflow is also scheduled to run every day at 5 AM PST / 12 PM UTC. It is composed of one job, `build-and-test-cws`, and runs on a GitHub runner with the latest version of Ubuntu.

The `build-and-test-cws` job performs all the necessary preliminary steps required to configure and build CWS with LDAP security. Once these steps are completed, a fully-functioning instance of CWS is built. Following the build, LDAP-specific integration tests are run. Afterwards, data from the workflow is sent to the development team.

The following are key differences in the steps of the `build-and-test-cws` job between the `CWS CI Camunda` and `CWS CI LDAP` workflows.

### build-and-test-cws Job
- **Set up CWS LDAP Server**:
  - Creates and starts a Docker container with an LDAP server
  - Runs in the background (detached mode)
- **Build CWS**:
  - Builds and runs CWS
  - Begins by running the first bash script in the build process: run_ci.sh
  - The bash script is passed the `SECURITY` environmental variable to run CWS in a specific security mode
  - The CWS security mode for this workflow is `LDAP`
- **Run LDAP Integration Tests**:
  - Runs LDAP-specific, Selenium-based integration tests
  - Uses a different naming scheme to prevent the tests from running with the standard unit and integration tests
