#!/usr/bin/env bash
docker container stop kalki-ovs-docker-server
docker run -p 5500:5500 --rm -v /var/run/docker.sock:/var/run/docker.sock --cap-add=NET_ADMIN --network=host --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
