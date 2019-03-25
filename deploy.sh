#!/usr/bin/env bash

# Perform a standard deploy for the project.
# -----------------------------------------------------------------------------
set -e

# Use moar nest to ensure all referenced modules are locally nested.
moar nest

# Initalize for a clean build
./build.sh

# Deploy via Bean Stalk
eb deploy --staged
