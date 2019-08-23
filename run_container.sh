#!/usr/bin/env bash

# DHCP server sometimes does not start properly along with machine.
sudo service isc-dhcp-server restart

docker run -p 6060:6060 --rm --net=host --name kalki-uc kalki/kalki-uc
