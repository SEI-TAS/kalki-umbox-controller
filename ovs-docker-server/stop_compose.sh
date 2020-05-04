#!/usr/bin/env bash
# Stop container, clear umboxes, clear bridge.
bash clear_umboxes.sh
source config.sh
docker-compose down
(cd ./ovs-scripts && bash remove_bridge.sh)
