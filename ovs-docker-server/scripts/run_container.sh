#!/usr/bin/env bash
# Stop container, if running.
bash stop_container.sh

# Reset bridge.
(cd ./ovs-scripts && bash setup_bridge.sh || exit 1)

# Start container.
HOST_TZ=$(cat /etc/timezone)
docker run --rm -e TZ=${HOST_TZ} \
                --network=host \
                --privileged \
                -v /var/run/docker.sock:/var/run/docker.sock \
                -v /var/run/openvswitch/db.sock:/var/run/openvswitch/db.sock \
                -v /proc:/host_proc \
                --name kalki-ovs-docker-server kalki/kalki-ovs-docker-server
