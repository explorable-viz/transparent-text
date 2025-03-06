#!/bin/bash
set -xe

#remove all the parameters that should be removed.
# Ensure JAVA_HOME is set correctly
if [[ $OSTYPE == 'darwin'* ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-22.0.2.jdk/Contents/Home"
fi
java --version
command="java --enable-preview -jar target/authoringAssistant-0.1-jar-with-dependencies.jar agent=$1"
output=$(eval "$command")
