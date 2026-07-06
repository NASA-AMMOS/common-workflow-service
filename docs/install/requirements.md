# Requirements & Compatibility

## Runtime & build

| Requirement | Minimum | Notes |
| --- | --- | --- |
| **Java** | 17 (JDK) | JDK required — **not** a JRE. Compatible with 17 and 21. Required for Spring 7.x. |
| **Maven** | 3.9.6 | Required for modern plugin support. |
| **Docker** | current | Used for MariaDB and Elasticsearch containers; recommended 5 CPUs / 14 GB memory / 1 GB swap / 64 GB disk. |
| **Database** | MariaDB or MySQL | A schema plus a user with full (CRUD) access. |
| **Elasticsearch** | 8.12.0+ | An externally-configured cluster (secure with/without auth, or insecure HTTP). |

## Core framework versions

| Library | Current version | Compatible | Notes |
| --- | --- | --- | --- |
| Spring Framework | 7.0.6 | 7.0.x | Stable |
| Camunda BPM | 7.24.6-ee | 7.24.x | **Enterprise Edition — a Camunda license is required** |
| Java | 17 | 17, 21 | LTS |

!!! warning "Camunda Enterprise license"
    CWS builds against Camunda **Enterprise Edition** (`7.24.6-ee`). Resolving
    these artifacts and running the engine requires a valid Camunda license and
    access to Camunda's enterprise repository.

## Testing dependencies

| Library | Current version | Notes |
| --- | --- | --- |
| JUnit | 4.13.2 | Migration to JUnit 5 is a future consideration. |

## Checking compatibility & security

CWS uses several tools to keep dependencies healthy:

- **Maven Enforcer Plugin** — enforces the Java 17 and Maven 3.9.6+ requirements
  automatically at build time.
- **Versions Maven Plugin** — check for updates manually:

    ```bash
    mvn versions:display-property-updates
    ```

- **OWASP Dependency-Check** — scan for known CVEs manually:

    ```bash
    mvn clean dependency-check:aggregate   # aggregate report
    mvn clean dependency-check:check       # per-module reports
    ```

Next: [Prerequisites](prerequisites.md).
