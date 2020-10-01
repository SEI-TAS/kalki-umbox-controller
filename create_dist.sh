#!/usr/bin/env bash

DIST_COMPONENT_FOLDER=$1

# Copy test files, in case they are needed.
cp -r tests/ "${DIST_COMPONENT_FOLDER}"/
