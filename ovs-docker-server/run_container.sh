#!/usr/bin/env bash
bash stop.sh
cd ./ovs-scripts
bash setup_bridge.sh
cd ..
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v /var/run/openvswitch/db.sock:/var/run/openvswitch/db.sock \
                -v /proc:/host_proc \
                --network=host \
                --privileged \
                --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
