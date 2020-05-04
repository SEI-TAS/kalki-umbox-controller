#!/usr/bin/env bash

# Set env variables.
source config.sh
export HOST_TZ=$(cat /etc/timezone)

# Clear running umboxes, if any.
bash clear_umboxes.sh

# Set up bridge.
cd ./ovs-scripts && source setup_bridge.sh && cd ..

# Start service and control network.
docker-compose up -d
docker-compose logs -f
