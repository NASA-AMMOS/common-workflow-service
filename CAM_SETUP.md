# CAM (Central Authentication Manager) Setup Guide

## Overview

CWS supports optional integration with CAM (Central Authentication Manager) for SSO token-based authentication. The CAM integration requires the `css-java` dependency, which is hosted in a private repository and not publicly available.

**CWS can be built and run with or without CAM support.** Users without access to the `css-java` dependency can build CWS successfully - CAM authentication features will simply be excluded from the build.

## Building Without CAM (Default)

If you don't have access to the `css-java` dependency or don't need CAM authentication, simply build CWS normally:

```bash
mvn clean install
```

This will build CWS successfully, excluding CAM-specific features.

## Building With CAM Support

If you have access to the `css-java` dependency and want CAM authentication support, follow these steps:

### Step 1: Install the css-java Dependency

You need the `css-java` JAR file installed in your local Maven repository. The current required version is defined in `pom.xml` as `${cam.version}` (currently 5.6.0).

#### Where to Obtain css-java

The `css-java` JAR can be obtained from:

1. **From an existing CWS installation**: Look in `$CATALINA_HOME/lib/css-java-*.jar`
2. **From JPL Artifactory**: If you have network access and credentials (see Option B below)
3. **From a colleague**: Request the JAR file from someone who has already obtained it
4. **From JPL software distribution**: Contact your CAM administrator

#### Option A: Manual Installation from Local JAR (Recommended)

If you have the JAR file locally, install it to your Maven repository:

```bash
mvn install:install-file \
  -Dfile=/path/to/css-java-5.6.0.jar \
  -DgroupId=gov.nasa.jpl.ammos.seo.asec.cam.client \
  -DartifactId=css-java \
  -Dversion=5.6.0 \
  -Dpackaging=jar
```

Replace `/path/to/css-java-5.6.0.jar` with the actual path to your JAR file.

This installs the JAR to `~/.m2/repository/gov/nasa/jpl/ammos/seo/asec/cam/client/css-java/5.6.0/` and Maven will use it for builds without requiring network access.

#### Option B: Configure JPL Artifactory Access

If you have access to JPL's Artifactory (`artifactory.jpl.nasa.gov`), you can configure Maven to download `css-java` automatically.

**Prerequisites:**
1. JPL network access (on-site or VPN)
2. Artifactory credentials
3. SSL certificates for `artifactory.jpl.nasa.gov` in Java's truststore

**Installing SSL Certificates:**

JPL Artifactory uses certificates signed by Sectigo/Entrust. You may need to import the intermediate CA into Java's cacerts:

```bash
# Download the intermediate CA certificate
curl -s "http://crt.sectigo.com/EntrustOVTLSIssuingRSACA2.crt" -o /tmp/entrust-intermediate.crt

# Convert to PEM format
openssl x509 -in /tmp/entrust-intermediate.crt -inform DER -out /tmp/entrust-intermediate.pem

# Find your Java cacerts location
JAVA_HOME_CACERTS="$(dirname $(dirname $(readlink -f $(which java))))/lib/security/cacerts"
# On macOS with Homebrew: /opt/homebrew/Cellar/openjdk@17/*/libexec/openjdk.jdk/Contents/Home/lib/security/cacerts

# Import the certificate
sudo keytool -importcert -alias entrust-ov-tls-issuing-rsa-ca-2 \
  -file /tmp/entrust-intermediate.pem \
  -keystore "$JAVA_HOME_CACERTS" \
  -storepass changeit -noprompt
```

**Configure Maven settings.xml:**

Add to your `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>jpl-artifactory</id>
      <username>your-jpl-username</username>
      <password>your-artifactory-password</password>
    </server>
  </servers>

  <profiles>
    <profile>
      <id>jpl-repos</id>
      <repositories>
        <repository>
          <id>jpl-artifactory</id>
          <url>https://artifactory.jpl.nasa.gov/artifactory/maven-libs-release-local</url>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>jpl-repos</activeProfile>
  </activeProfiles>
</settings>
```

**Note:** Your Artifactory password may be an API key or encrypted password. Check with your JPL administrator for the correct credentials format.

### Step 2: Build with CAM Profile

Once the dependency is installed, build with the `cam-support` Maven profile:

```bash
mvn clean install -Pcam-support
```

This will include all CAM authentication features in the build.

Alternatively, use the provided development script:

```bash
./jr-cam.sh
```

This script sets `BUILD_PROFILE=cam-support` and configures all CAM-related settings for a complete development environment.

## What's Included/Excluded

### Features Available WITHOUT CAM:
- ✅ Standard LDAP authentication
- ✅ Camunda authentication
- ✅ Core workflow functionality
- ✅ Standard security filters
- ✅ All non-CAM tasks and services

### Features Available ONLY WITH CAM:
- CAM SSO token authentication
- CAM LDAP identity provider integration
- CAM security filters
- REST tasks with CAM token support (`RestGetTask`, `RestPostTask`)
- CAM authentication management

## Affected Modules

The following modules have CAM-specific code that's conditionally compiled:

### cws-core
- **Source directory**: `src/main/java-cam/`
- **Classes**:
  - `gov.nasa.ammos.security.css.AuthenticationMgr`
  - `jpl.cws.core.identity.cam.*` (entire package)
  - `jpl.cws.core.web.CwsCamSecurityFilter`
  - `jpl.cws.core.service.CamSecurityService`

### cws-tasks
- **Source directory**: `src/main/java-cam/`
- **Classes**:
  - `jpl.cws.task.RestGetTask`
  - `jpl.cws.task.RestPostTask`

### cws-test
- **Test directory**: `src/test/java-cam/`
- **Test classes**:
  - `RestGetTaskTest`
  - `RestGetTaskTest2`
  - `RestPostTaskTest`
  - `RestPostTaskTest2`

## Technical Details

The `cam-support` profile uses the `build-helper-maven-plugin` to conditionally add source directories:
- `src/main/java-cam/` - Added to main source path
- `src/test/java-cam/` - Added to test source path

When the profile is **not** active, these directories are ignored, and the code is not compiled.

## Troubleshooting

### Build fails with "Could not resolve dependencies for css-java"

This means you're building with `-Pcam-support` but the `css-java` dependency is not available. Either:
1. Install the dependency manually using Option A above, OR
2. Build without the profile: `mvn clean install`

### Build fails with "PKIX path building failed" when accessing JPL Artifactory

This SSL certificate error occurs when Java doesn't trust the certificate chain for `artifactory.jpl.nasa.gov`. The server uses certificates signed by Sectigo/Entrust which may not be in Java's default truststore.

**Solution:** Import the intermediate CA certificate into Java's cacerts (see "Installing SSL Certificates" in Option B above).

**To verify the certificate is installed:**
```bash
keytool -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit | grep -i entrust
```

### ClassNotFoundException for CAM classes at runtime

If you see errors like `ClassNotFoundException: gov.nasa.ammos.security.css.AuthenticationMgr` at runtime:
- You're trying to use CAM features but didn't build with `-Pcam-support`
- Rebuild with the CAM profile enabled

### How do I know if CAM is enabled?

Check the build output. When building with `-Pcam-support`, you should see:
```
[INFO] --- build-helper-maven-plugin:3.3.0:add-source (add-cam-source)
```

## Questions?

For questions about obtaining the `css-java` dependency, contact your organization's CAM administrator or repository maintainer.
