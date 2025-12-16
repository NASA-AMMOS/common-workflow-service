#!/usr/bin/env bash
set -euo pipefail

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source ${ROOT}/../utils.sh
cd $ROOT

# Get Camunda version from command line argument (required)
if [ $# -eq 0 ]; then
    print "Usage: $0 <camunda-version>"
    print "Example: $0 7.23.5-ee"
    exit 1
fi

CAMUNDA_VERSION="$1"
print "Using Camunda version: $CAMUNDA_VERSION"

# Check if CWS Camunda zip files already exist
if [ -f "camunda-distro-zips/cws_camunda-bpm-ee-tomcat-${CAMUNDA_VERSION}.zip" ] && [ -f "camunda-distro-zips/cws_camunda-bpm-ee-tomcat-${CAMUNDA_VERSION}-lib.zip" ]; then
    print "CWS Camunda zip files already exist in camunda-distro-zips directory..."
    exit 0
fi

mkdir -p camunda-distro-zips
cd camunda-distro-zips
print "Downloading Camunda version $CAMUNDA_VERSION..."

# Extract major.minor.patch version for URL construction (e.g., 7.23.5-ee -> 7.23.5)
MAJOR_MINOR_PATCH=$(echo "$CAMUNDA_VERSION" | sed 's/-.*$//')
# Extract major.minor for subdirectory (e.g., 7.23.5 -> 7.23)
MAJOR_MINOR=$(echo "$MAJOR_MINOR_PATCH" | sed 's/\.[0-9]*$//')
DOWNLOAD_URL="https://downloads.camunda.cloud/enterprise-release/camunda-bpm/tomcat/${MAJOR_MINOR}/${MAJOR_MINOR_PATCH}/camunda-bpm-ee-tomcat-${CAMUNDA_VERSION}.zip"

print "Download URL: $DOWNLOAD_URL"
curl -s -S -n -L -O "$DOWNLOAD_URL"

ZIP_PATH="camunda-bpm-ee-tomcat-${CAMUNDA_VERSION}.zip"

# Verify the zip file exists
if [ ! -f "$ZIP_PATH" ]; then
    print "Error: ZIP file '$ZIP_PATH' not found."
    exit 1
fi

# Prepare names
ZIP_FILE="$(basename "$ZIP_PATH")"
BASE_NAME="${ZIP_FILE%.zip}"

# Prepare temp directory
TEMP_DIR="tmp"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR/$BASE_NAME"

# Extract into temp/<basename>
print "Extracting $ZIP_PATH to $TEMP_DIR/$BASE_NAME..."
unzip -q "$ZIP_PATH" -d "$TEMP_DIR/$BASE_NAME"

# Move lib folder to temp/lib
if [ -d "$TEMP_DIR/$BASE_NAME/lib" ]; then
    mv "$TEMP_DIR/$BASE_NAME/lib" "$TEMP_DIR/"
else
    print "No lib directory found in $TEMP_DIR/$BASE_NAME"
    exit 1
fi

# Find the Tomcat directory dynamically
TOMCAT_DIR=$(find "$TEMP_DIR/$BASE_NAME/server" -name "apache-tomcat-*" -type d | head -1)
if [ -z "$TOMCAT_DIR" ]; then
    print "Error: Could not find apache-tomcat directory in $TEMP_DIR/$BASE_NAME/server"
    print "Available directories:"
    ls -la "$TEMP_DIR/$BASE_NAME/server" 2>/dev/null || print "  Server dir not found"
    exit 1
fi

print "Found Tomcat directory: $TOMCAT_DIR"

# Remove unwanted directories from webapps
WEBAPPS_DIR="$TOMCAT_DIR/webapps"
print "Looking for webapps directory at: $WEBAPPS_DIR"
print "Current working directory: $(pwd)"
print "Temp directory contents:"
ls -la "$TEMP_DIR" 2>/dev/null || print "  Temp dir not found"
print "Base name directory contents:"
ls -la "$TEMP_DIR/$BASE_NAME" 2>/dev/null || print "  Base name dir not found"

if [ -d "$WEBAPPS_DIR" ]; then
    print "Found webapps directory!"
    print "Webapps directory contents:"
    ls -la "$WEBAPPS_DIR"
    print "Removing unwanted directories from webapps..."
    for dir in "camunda-invoice" "camunda-welcome" "examples"; do
        print "Checking for directory: $WEBAPPS_DIR/$dir"
        if [ -d "$WEBAPPS_DIR/$dir" ]; then
            print "  Removing $dir"
            rm -rf "$WEBAPPS_DIR/$dir"
            print "After removal, checking if $dir still exists:"
            if [ -d "$WEBAPPS_DIR/$dir" ]; then
                print "  ERROR: $dir still exists after removal!"
            else
                print "  SUCCESS: $dir was removed"
            fi
        else
            print "  Directory $dir not found, skipping"
        fi
    done
    print "Final webapps directory contents:"
    ls -la "$WEBAPPS_DIR"
else
    print "Warning: webapps directory not found at $WEBAPPS_DIR"
    print "Let's search for webapps directories:"
    find "$TEMP_DIR" -name "webapps" -type d 2>/dev/null || print "  No webapps directories found"
fi

# Zip remaining files (without lib)
print "Creating cws_${BASE_NAME}.zip..."
# Remove existing zip file if it exists
rm -f "cws_${BASE_NAME}.zip"
(cd "$TEMP_DIR/$BASE_NAME" && zip -rq "../../cws_${BASE_NAME}.zip" .)

# Zip lib folder separately
print "Creating cws_${BASE_NAME}-lib.zip..."
# Remove existing zip file if it exists
rm -f "cws_${BASE_NAME}-lib.zip"
(cd "$TEMP_DIR/lib" && zip -rq "../../cws_${BASE_NAME}-lib.zip" .)

# Cleanup
rm -rf "$TEMP_DIR"
print "Done."
