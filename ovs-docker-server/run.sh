#!/usr/bin/env bash

# Clean up any leftover containers.
echo "Removing all running umboxes."
docker ps --format '{{.Names}}' | grep "^umbox-" | awk '{print $1}' | xargs -I {} docker stop {}

python docker_api.py
