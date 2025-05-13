#!/bin/bash

# Script to compile and run the PopulateFinishedJobs.java program
# This script will populate the CWS database with a configurable number of finished jobs

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
cd "$SCRIPT_DIR"

# Create lib directory if it doesn't exist
mkdir -p lib

# Check if necessary JAR dependencies exist in lib directory
if [ ! -f "./lib/mysql-connector-java.jar" ]; then
    echo "Downloading MySQL connector..."
    wget -q https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.28/mysql-connector-java-8.0.28.jar -O ./lib/mysql-connector-java.jar
fi

# Configure defaults
JOB_COUNT=30000
DB_URL="jdbc:mysql://db:3306/cws"
DB_USER="root"
DB_PASS="changeme"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --count)
            JOB_COUNT="$2"
            shift 2
            ;;
        --url)
            DB_URL="$2"
            shift 2
            ;;
        --user)
            DB_USER="$2"
            shift 2
            ;;
        --pass)
            DB_PASS="$2"
            shift 2
            ;;
        --help)
            echo "Usage: ./populate_db.sh [options]"
            echo "Options:"
            echo "  --count <number>    Number of jobs to create (default: 30000)"
            echo "  --url <url>         Database URL (default: jdbc:mysql://localhost:3306/cws)"
            echo "  --user <username>   Database username (default: admin)"
            echo "  --pass <password>   Database password (default: changeme)"
            echo "  --help              Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "Compiling PopulateFinishedJobs.java..."
javac -cp ".:./lib/*" PopulateFinishedJobs.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Running PopulateFinishedJobs with $JOB_COUNT jobs..."
java -cp ".:./lib/*" PopulateFinishedJobs --count "$JOB_COUNT" --url "$DB_URL" --user "$DB_USER" --pass "$DB_PASS"