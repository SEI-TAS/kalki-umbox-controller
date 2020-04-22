#!/usr/bin/env bash
# Stop container, clear umboxes, clear bridge.
docker container stop kalki-ovs-docker-server
bash clear_umboxes.sh
cd ./ovs-scripts
bash remove_bridge.sh
cd ..
