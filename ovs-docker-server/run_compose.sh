#!/usr/bin/env bash

# Clear running umboxes, if any.
bash clear_umboxes.sh

# Set up bridge.
cd ./ovs-scripts && bash setup_bridge.sh && cd ..

# Start service and control network.
source config.sh
docker-compose up -d
bash compose_logs.sh
