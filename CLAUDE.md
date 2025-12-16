# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NASA-AMMOS Common Workflow Service (CWS) is an enterprise workflow management platform built on top of Camunda BPMN Engine. It provides an intuitive user interface, auditable logging, extensibility with code snippets, adaptation layers, and a powerful external task engine for mission-critical workflow automation.

## Development Commands

### Prerequisites
- Java 17 JDK (required - cannot use JRE)
- Maven 3.9.6+
- Docker with recommended resources: 5 CPUs, 14GB Memory, 1GB Swap, 64GB Disk
- MariaDB/MySQL database
- Elasticsearch 8.12.0+ cluster
- ITerm2 (for development scripts)

### Setup Commands

#### Database Setup (MariaDB via Docker)
```bash
docker run -d -p 3306:3306 -e MYSQL_DATABASE=cws_dev -e MYSQL_ROOT_PASSWORD=YOUR_PASSWORD -e TZ=America/Los_Angeles --name mdb106 mariadb:10.6
```

#### Elasticsearch Setup
```bash
cd install/docker/es-only
docker-compose up
```

### Build and Development Commands

#### Full Build
```bash
./build.sh
```
This command:
- Cleans module library directories
- Runs `mvn -DskipTests -Dskip.integration.tests clean install -P core`
- Creates server distribution via `create_server_dist.sh`

#### Development Setup
Create a personal development script (e.g., `jsmith.sh`) that calls `dev.sh` with configuration parameters:
```bash
./dev.sh ROOT_DIR USERNAME DB_TYPE DB_HOST DB_PORT DB_NAME DB_USER DB_PASS ES_PROTOCOL ES_HOST ES_INDEX_PREFIX ES_PORT ES_USE_AUTH ES_USERNAME ES_PASSWORD CLOUD SECURITY HOSTNAME EMAIL_LIST ADMIN_FIRST ADMIN_LAST ADMIN_EMAIL NUM_WORKERS WORKER_MAX_PROCS WORKER_ABANDONED_DAYS
```

#### Testing
```bash
./test.sh  # Runs unit tests and integration tests with JaCoCo coverage
```

#### Stop Development Environment
```bash
./stop_dev.sh
```

#### Security Vulnerability Check
```bash
mvn dependency-check:check
# or
mvn dependency-check:aggregate
```

### Single Test Execution
To run individual tests, use Maven directly:
```bash
mvn test -Dtest=ClassName#methodName
mvn integration-test -Dit.test=IntegrationTestClass
```

## Architecture Overview

### Multi-Module Structure
CWS follows a layered service-oriented architecture with these key modules:

- **cws-core**: Foundation layer with configuration, database services, logging, security
- **cws-tasks**: BPMN task implementations (Email, CommandLine, REST, File operations)
- **cws-service**: Business logic layer with process initiators, scheduling, REST APIs
- **cws-engine-service**: Engine-specific services and external task handling
- **cws-engine**: Camunda engine integration (WAR)
- **cws-adaptation**: Project-specific customizations
- **cws-ui**: Web-based user interface (WAR)
- **cws-installer**: Installation and configuration utilities
- **cws-test**: Integration and system testing framework

### Key Technologies
- **Spring Framework 6.2.4**: Dependency injection, web services, transactions
- **Camunda BPM 7.23.0-ee**: Workflow engine and BPMN execution
- **Apache Artemis**: Message queue for external task communication
- **MyBatis**: Database ORM and query mapping
- **Apache Tomcat 10.1.36**: Application server

### Module Dependencies
```
cws-core (foundation)
├── cws-tasks (depends on cws-core)
├── cws-service (depends on cws-core)
├── cws-installer (depends on cws-core)
└── cws-engine-service (depends on cws-core, cws-tasks)
    ├── cws-adaptation-engine (depends on cws-engine-service)
    └── cws-engine [WAR] (depends on cws-adaptation-engine)
        └── cws-adaptation (depends on cws-service, cws-core)
            └── cws-ui [WAR] (depends on cws-adaptation)
```

## Key Configuration Files

- **pom.xml**: Root Maven configuration with dependency management
- **utils.sh**: Shell utility functions and environment settings (CWS_VER, CAMUNDA_VER, TOMCAT_VER)
- **install/installerPresets.properties**: Default configuration properties
- **install/example-cws-configuration.properties**: Example configuration
- **owasp-suppressions.xml**: OWASP dependency check suppressions
- **version-rules.xml**: Maven version management rules

## Important Development Notes

### Version Information
- CWS Version: 2.7.0 (update in pom.xml and utils.sh for releases)
- Camunda Version: 7.23.0-ee
- Tomcat Version: 10.1.36
- Java Version: 17 (enforced by Maven)

### Security Requirements
- SSL certificates required in `install/.keystore` and `install/tomcat_lib/cws_truststore.jks`
- Keystore password stored in `~/.cws/creds` with 400 permissions
- LDAP integration for enterprise authentication
- Multiple security filters (LDAP, Camunda, general web security)

### Database Integration
- Supports MariaDB and MySQL
- Uses MyBatis for ORM and database access
- Database setup and validation handled by cws-installer module
- All database operations centralized through cws-core's DbService

### External Task Processing
- Uses Apache Artemis (ActiveMQ) for message queuing
- Workers can run as separate JVM processes
- External task service in cws-engine-service handles task distribution
- Support for multiple worker types: run_all, run_models_only, run_external_tasks_only

### Logging and Monitoring
- Centralized logging through cws-core logging framework
- Integration with Elasticsearch for log aggregation
- JaCoCo for code coverage reporting
- Swagger/OpenAPI for API documentation

### Custom Task Development
- Extend `CwsTask` base class in cws-tasks module
- Follow template method pattern for task implementations
- Built-in tasks include: Email, CommandLine, REST operations, File operations, Sleep

## Branch Strategy

- **main**: Release branch
- **develop**: Default development branch
- Current working branch: **upgrade-camunda-7.23.00-ee**

## CI/CD Integration

- GitHub Actions workflows for Camunda and LDAP testing
- Docker-based deployment support
- OWASP dependency checking integrated into build process
- Maven enforcer plugin ensures Java 17 and Maven 3.9.6+ requirements

Always reference this architecture when making changes to ensure consistency with the layered service-oriented design and proper module separation.