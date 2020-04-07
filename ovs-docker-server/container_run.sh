#!/usr/bin/env bash
docker run -p 5500:5500 --rm -v /var/run/docker.sock:/var/run/docker.sock --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
