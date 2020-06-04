#!/usr/bin/env bash

source config.sh
DOCKER_CONTROL_NET=ovsdockerserver_control-net

docker network create -d macvlan --subnet=${CONTROL_IP_NET} \
                                 --gateway=${CONTROL_GATEWAY} \
                                 --ip-range=${UMBOX_IP_RANGE} \
                                 -o parent=${CONTROL_NIC} \
                                 ${DOCKER_CONTROL_NET}
