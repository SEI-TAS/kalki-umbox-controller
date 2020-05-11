#!/usr/bin/env bash

DIST_PATH=$1

if [ -z "${DIST_PATH}" ]; then
  echo "Destination dist path argument required"
  exit 1
fi

# Copy startup script, and ovs bridge scripts.
cp *.sh ${DIST_PATH}
mkdir -p ${DIST_PATH}/ovs-scripts/
cp ovs-scripts/*.sh ${DIST_PATH}/ovs-scripts/
