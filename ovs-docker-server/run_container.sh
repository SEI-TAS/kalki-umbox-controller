#!/usr/bin/env bash
bash stop.sh
bash ./ovs-scripts/setup_bridge.sh
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v /var/run/openvswitch/db.sock:/var/run/openvswitch/db.sock \
                -v /proc:/host_proc \
                --network=host \
                --privileged \
                --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
