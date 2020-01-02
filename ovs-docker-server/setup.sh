#!/usr/bin/env bash

DOCKER_CONTROL_NET=control-net
CONTROL_BRIDGE=br-control
CONTROL_NET=10.27.153.0/24
GATEWAY=10.27.153.1
CONTROL_NODE_IP=10.27.153.3
DATA_NODE_IP=10.27.153.2

docker network create -d macvlan --subnet=${CONTROL_NET} --gateway=${GATEWAY} --aux-address="control=${CONTROL_NODE_IP}" --aux-address="data=${DATA_NODE_IP}" -o parent=${CONTROL_BRIDGE} ${DOCKER_CONTROL_NET}

# How to connect main NIC and 3 OVS NICs.
#docker run --rm -dit --network control-net --name umbox-id alpine:latest ash
#sudo ovs-docker add-port ovs-br eth1 umbox-id
#sudo ovs-docker add-port ovs-br eth2 umbox-id
#sudo ovs-docker add-port ovs-br eth3 umbox-id

# Cleanup
#sudo ovs-docker del-ports ovs-br umbox-id
#docker container stop umbox-id

# To get each port name:
#sudo ovs-vsctl --data=bare --no-heading --columns=name find interface external_ids:container_id="umbox-test2" external_ids:container_iface="eth1"
