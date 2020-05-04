#!/usr/bin/env bash

#export CONTROL_NIC=enp0s31f6
export CONTROL_NIC=br-control
export IOT_NIC=enp2s0f1
export EXT_NIC=enp2s0f0

export CONTROL_NET_PREFIX=10.27.153
export IP_ON_CONTROL_NIC=${CONTROL_NET_PREFIX}.2
export CONTROL_IP_NET=${CONTROL_NET_PREFIX}.0/24
export CONTROL_GATEWAY=${CONTROL_NET_PREFIX}.1
export UMBOX_IP_RANGE=${CONTROL_NET_PREFIX}.128/25

export HOST_TZ=$(cat /etc/timezone)
