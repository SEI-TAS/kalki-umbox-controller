#!/usr/bin/env bash

# Set up env vars.
source config.sh

# Clear running umboxes, if any.
bash clear_umboxes.sh

# Set up bridge.
(cd ./ovs-scripts && bash setup_bridge_tiered.sh || exit 1)
