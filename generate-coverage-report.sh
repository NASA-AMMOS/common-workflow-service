#!/bin/bash

# Script to generate JaCoCo code coverage report for the entire CWS codebase

echo "============================================"
echo "Generating JaCoCo Code Coverage Report"
echo "============================================"

# Clean previous builds and reports
echo "Cleaning previous builds..."
mvn clean

# Run tests with JaCoCo agent
echo "Running tests with JaCoCo coverage..."
mvn test

# Generate aggregate coverage report
echo "Generating aggregate coverage report..."
mvn verify -DskipTests

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