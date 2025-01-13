#!/bin/bash
# Build the project package with maven.
set -xe

mvn --batch-mode clean package
