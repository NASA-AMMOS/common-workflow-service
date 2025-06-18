# CWS Continuous Integration (CI) Pipeline

The following workflows establish continuous integration and continuous delivery (CI/CD) pipelines for this repository containing the open-source release of the [NASA-AMMOS](https://ammos.nasa.gov/) Common Workflow Service (CWS).

Utilizing GitHub Actions, the workflows build, test, and deliver CWS by configuring steps required to accomplish these tasks, making use of Marketplace Actions in the process. There are currently two CI pipelines: [CWS CI Camunda](https://github.com/NASA-AMMOS/common-workflow-service/actions/workflows/camunda.yml) and [CWS CI LDAP](https://github.com/NASA-AMMOS/common-workflow-service/actions/workflows/ldap.yml).

## CWS CI Camunda Workflow

The CWS CI Camunda workflow is triggered upon any push to the repository, including commits, pull requests, and merges. The workflow is also scheduled to run every Monday at 5 AM PST / 12 PM UTC. It is composed of three jobs: `build-and-test-cws`, `advanced-test`, and `publish-cws-image`. All jobs run on separate GitHub runners with the latest version of Ubuntu.

The `build-and-test-cws` job performs all the necessary preliminary steps required to configure and build CWS with Camunda security. Once these steps are completed, a fully-functioning instance of CWS is built. Following the build, unit and integration tests are run, with resulting artifacts being saved to the workflow run. Afterwards, data from the workflow is sent to the development team.

The `advanced-test` job is fundamentally the same for the build of CWS, except 2 workers being started instead of 1. Following the build, an advanced integration test requiring additional workers is run.

The `publish-cws-image` job is the CD component of the workflow, triggered upon a commit with a tag and dependant upon successful completion of the `build-and-test-cws` and `advanced-test` jobs.

> **Warning**
> The current GitHub runner configuration used for the build and test of CWS, as well as the advanced test, supports up to 1 console and 2 workers.
> More than 2 workers may result in test failures due to the stress on the runner.

### build-and-test-cws Job

- [**Services**](https://docs.github.com/en/actions/using-containerized-services/about-service-containers):
  - MariaDB
    - Image: mariadb:10.11
    - Ports: 3306:3306
- [**checkout**](https://github.com/marketplace/actions/checkout): This action checks out the repository under `$GITHUB_WORKSPACE`, so the workflow can access it.
- Set up JDK 17:
  - [**setup-java**](https://github.com/marketplace/actions/setup-java-jdk): This action downloads and sets up a requested version of Java
  - Current configuration:
      - Java-version: 17
      - Distribution: Temurin
      - Cache: Maven
- **Create open-source certs**:
    - Runs the `generate-certs.sh` bash script
    - Creates and stores keystore and truststore required to access CWS UI
- **Download Logstash**:
  - [**download-file-action**](https://github.com/marketplace/actions/download-file-to-workspace): This action downloads a file from the internet into the workspace
    - Downloads Logstash using a URL
    - Renames the file as `logstash-8.12.0.zip`
    - Stores Logstash in appropriate directory
- **Check for Logstash**:
  - List files in the directory where Logstash is expected
  - Can be used to verify successful download of Logstash
- **Set up Elasticsearch**:
  - Creates and starts a Docker container with Elasticsearch
  - Runs in the background (detached mode)
- **Show Docker containers**: Shows all Docker containers and their statuses
- **Build CWS**:
  - Builds and runs CWS
  - Begins by running the first bash script in the build process: run_ci.sh
    - The bash script is passed the `SECURITY` environmental variable to run CWS in a specific security mode
      - The CWS security mode for this workflow is `CAMUNDA`
    - The bash script is passed the `WORKERS` environmental variable to start the specified number of workers
      - Current configuration is set to run 1 console and 1 worker
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

### advanced-test Job
- **Build CWS**:
  - Builds and runs CWS
  - Begins by running the first bash script in the build process: run_ci.sh
    - The bash script is passed the `SECURITY` environmental variable to run CWS in a specific security mode
      - The CWS security mode for this workflow is `CAMUNDA`
    - The bash script is passed the `WORKERS` environmental variable to start the specified number of workers
      - Current configuration is set to run 1 console and 2 workers
- **Run Load Integration Test**:
  - Runs the Selenium-based LoadTestIT
  - Requires a minimum of 2 workers to be started in order to run properly

### publish-cws-image Job
- **Check out the repo**:
  - Utilizes the same `checkout` action to check out the repository again
  - This is done in a new GitHub runner
- **Set up JDK 17**
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

The CWS CI LDAP workflow is triggered upon any push to the repository, including commits, pull requests, and merges. The workflow is also scheduled to run every Monday at 5 AM PST / 12 PM UTC. It is composed of one job, `build-and-test-cws`, and runs on a GitHub runner with the latest version of Ubuntu.

The `build-and-test-cws` job performs all the necessary preliminary steps required to configure and build CWS with LDAP security. Once these steps are completed, a fully-functioning instance of CWS is built. Following the build, LDAP-specific integration tests are run. Afterwards, data from the workflow is sent to the development team.

The following are key differences in the steps of the `build-and-test-cws` job between the `CWS CI Camunda` and `CWS CI LDAP` workflows.

> **Warning**
> The current GitHub runner configuration used for the build and test of CWS supports up to 1 console and 2 workers.
> More than 2 workers may result in test failures due to the stress on the runner.

### build-and-test-cws Job
- **Set up CWS LDAP Server**:
  - Creates and starts a Docker container with an LDAP server
  - Runs in the background (detached mode)
- **Build CWS**:
  - Builds and runs CWS
  - Begins by running the first bash script in the build process: run_ci.sh
    - The bash script is passed the `SECURITY` environmental variable to run CWS in a specific security mode
      - The CWS security mode for this workflow is `LDAP`
    - The bash script is passed the `WORKERS` environmental variable to start the specified number of workers
      - Current configuration is set to run 1 console and 1 worker
- **Run LDAP Integration Tests**:
  - Runs LDAP-specific, Selenium-based integration tests
  - Uses a different naming scheme to prevent the tests from running with the standard unit and integration tests
