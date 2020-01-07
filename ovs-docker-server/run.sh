#!/usr/bin/env bash

# DHCP server sometimes does not start properly along with machine.
sudo service isc-dhcp-server restart

# Clean up any leftover containers.
echo "Removing all running umboxes."
# TODO

python -m pipenv run python docker_api.py
