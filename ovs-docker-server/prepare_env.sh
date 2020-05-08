#!/usr/bin/env bash

# Clear running umboxes, if any.
bash clear_umboxes.sh

# Set up bridge.
(cd ./ovs-scripts && bash setup_bridge.sh)

# Set up env vars.
source config.sh
export HOST_TZ=$(cat /etc/timezone)
