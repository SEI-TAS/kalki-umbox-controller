#!/usr/bin/env bash

source config.sh

DOCKER_CONTROL_NET=control-net
GATEWAY=${CONTROL_NET_PREFIX}.1

docker network create -d macvlan --subnet=${CONTROL_NET} \
                                 --gateway=${GATEWAY} \
                                 --ip-range=${UMBOX_IP_RANGE} \
                                 -o parent=${CONTROL_NIC} \
                                 ${DOCKER_CONTROL_NET}
