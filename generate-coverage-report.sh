#!/bin/bash

# Script to generate JaCoCo code coverage report for the entire CWS codebase

echo "============================================"
echo "Generating JaCoCo Code Coverage Report"
echo "============================================"

# Clean previous builds and reports
echo "Cleaning previous builds..."
mvn clean

# Install modules to local repository first
echo "Building and installing modules to local repository..."
mvn install -DskipTests -Dmaven.javadoc.skip=true

# Run tests with JaCoCo agent (continue even if tests fail)
echo "Running tests with JaCoCo coverage..."
echo "Note: Will continue even if some tests fail to generate coverage report"
mvn test -Dmaven.test.failure.ignore=true -DfailIfNoTests=false

# Skip integration tests if they're causing issues
echo "Running unit tests only (skipping integration tests)..."
mvn test -Dmaven.test.failure.ignore=true -DfailIfNoTests=false -DskipITs=true

# Compile and package without running tests again
echo "Packaging modules..."
mvn package -DskipTests

# Generate aggregate coverage report
echo "Generating aggregate coverage report..."
mvn jacoco:report-aggregate -pl cws-coverage-aggregate

# Alternative: Generate report using verify phase
echo "Running verify phase for complete report..."
mvn verify -DskipTests -pl cws-coverage-aggregate

# The aggregate report will be generated in:
# cws-coverage-aggregate/target/site/jacoco-aggregate/index.html

echo ""
echo "============================================"
echo "Coverage report generation complete!"
echo "============================================"
echo ""
echo "The aggregated coverage report is available at:"
echo "cws-coverage-aggregate/target/site/jacoco-aggregate/index.html"
echo ""
echo "Individual module reports are available in each module's target/site/jacoco directory"
echo ""
echo "Note: Some integration tests may have failed, but coverage data was still collected."