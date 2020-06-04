#!/usr/bin/env bash

# Stop container, clear umboxes, clear bridge.
source config.sh
bash clear_umboxes.sh
(cd ./ovs-scripts && bash remove_bridge.sh)
