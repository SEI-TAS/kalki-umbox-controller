#!/usr/bin/env bash

# DHCP server sometimes does not start properly along with machine.
sudo service isc-dhcp-server restart

# Clean up any leftover containers.
echo "Removing all running umboxes."
docker ps --format '{{.Names}}' | grep "^umbox-" | awk '{print $1}' | xargs -I {} docker stop {}

python -m pipenv run python docker_api.py
