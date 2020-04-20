#!/usr/bin/env bash

source config.sh

DOCKER_CONTROL_NET=control-net
CONTROL_NET=${CONTROL_NET_PREFIX}.0/24
GATEWAY=${CONTROL_NET_PREFIX}.1
IP_RANGE=${CONTROL_NET_PREFIX}.128/25

docker network create -d macvlan --subnet=${CONTROL_NET} \
                                 --gateway=${GATEWAY} \
                                 --ip-range=${IP_RANGE} \
                                 -o parent=${CONTROL_NIC} \
                                 ${DOCKER_CONTROL_NET}

ENV PIPENV_VENV_IN_PROJECT "enabled"
pipenv install
