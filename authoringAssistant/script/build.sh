#!/bin/bash
# Build the project package with maven.
set -xe

# Ensure JAVA_HOME is set correctly
export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
mvn --version

mvn --batch-mode clean package
