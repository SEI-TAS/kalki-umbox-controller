#!/usr/bin/env bash
docker container stop kalki-ovs-docker-server
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v /var/run/openvswitch/db.sock:/var/run/openvswitch/db.sock \
                -v /var/run/netns/:/var/run/netns/ \
                --cap-add=NET_ADMIN --network=host \
                --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
