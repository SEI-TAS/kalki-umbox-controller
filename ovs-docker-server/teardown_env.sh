#!/usr/bin/env bash

# Stop container, clear umboxes, clear bridge.
bash clear_umboxes.sh
source config.sh
(cd ./ovs-scripts && bash remove_bridge.sh)
