#!/usr/bin/env bash

DIST_PATH=$1

if [ -z "${DIST_PATH}" ]; then
  echo "Destination dist path argument required"
  exit 1
fi

# Copy startup script, and ovs bridge scripts.
cp clear_umboxes.sh ${DIST_PATH}
cp config.sh ${DIST_PATH}
cp install_packages.sh ${DIST_PATH}
cp prepare_env.sh ${DIST_PATH}
cp teardown_env.sh ${DIST_PATH}

mkdir -p ${DIST_PATH}/ovs-scripts/
cp ovs-scripts/*.sh ${DIST_PATH}/ovs-scripts/
