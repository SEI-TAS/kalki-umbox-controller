#!/usr/bin/env bash
docker container stop kalki-ovs-docker-server
bash ./ovs-scripts/setup_bridge.sh
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v /var/run/openvswitch/db.sock:/var/run/openvswitch/db.sock \
                -v /proc:/host_proc \
                --cap-add=NET_ADMIN --network=host \
                --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
