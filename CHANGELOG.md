# Changelog

All notable changes to this project will be documented in this file. 

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [X.Y.Z](https://github.com/NASA-AMMOS/<repo_name>/releases/tag/X.Y.Z) - 2023-MM-DD

## [Unreleased]
### Added
- **CAM (Central Authentication Manager) Integration** - Optional SSO authentication support
  - Conditionally compiled via `cam-support` Maven profile
  - Requires optional `css-java` dependency (not in Maven Central)
  - Works in conjunction with LDAP for user/group information
  - See [CAM_SETUP.md](CAM_SETUP.md) for setup instructions
  - Includes `jr-cam.sh` development script for quick CAM setup
  - Added CAM configuration properties to installerPresets.properties
- **LDAP Setup Documentation** - New [LDAP_SETUP.md](LDAP_SETUP.md) guide
  - Detailed LDAP configuration options
  - Troubleshooting guide for common LDAP issues
  - LDAP authentication flow documentation
  - `jr-ldap.sh` development script reference
- **Authentication Modes Documentation** - Updated README with three authentication options

### Changed
- Updated README to document three authentication modes (Camunda, LDAP, CAM)
- Build system now supports conditional compilation of CAM code via Maven profile
- Spring applicationContext.xml files now include optional CAM security service bean

### Deprecated

### Removed

### Fixed

### Security
- CAM code uses Jakarta servlet APIs (jakarta.servlet) for Tomcat 10.x compatibility
- LDAP manager password should be set via environment variable for security

## [2.8.0]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- Converted ActiveMQ 5.x to Artemis ActiveMQ broker
  - broker.xml has security-enabled as false.  For production use this should be setup with security enabled.
### Breaking Changes
- Need to add the Camunda License file to ~/.camunda/license.txt since we moved to use Camunda Enterprise Edition
- Update Database Schema from Camunda 7.20 to Camunda 7.23.0-ee:
  - Go to the <root-dir>/sql/upgrade/ dir and use the upgrade scripts (in order): 
    - mysql_engine_7.20_to_7.21.sql
    - mysql_engine_7.21_to_7.22.sql
    - mysql_engine_7.22_to_7.23.sql
- Second Database Schema Update
  - Change Details here: https://github.com/NASA-AMMOS/common-workflow-service/commit/5ee0cc7c22eb740a97230f19b814bd52c3804546
- Update Database Table cws_sched_worker_proc_inst Column proc_variables from FSTObjectOutput (Binary Type blob) to json string (Blob UTF-8 string)
  - Read field as de.ruedigermoeller.serialization.FSTObjectInput and convert to com.fasterxml.jackson.databind.ObjectMapper UTF-8 string
