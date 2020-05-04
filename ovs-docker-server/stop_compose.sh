#!/usr/bin/env bash
# Stop container, clear umboxes, clear bridge.
bash clear_umboxes.sh
docker-compose down
cd ./ovs-scripts && source remove_bridge.sh && cd ..
