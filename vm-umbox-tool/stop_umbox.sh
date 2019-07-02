#!/usr/bin/env bash

DATA_NODE_IP=192.168.58.102

IOT_DEVICE_IP=192.168.56.103

UMBOX_NAME=$1

python umbox.py -c stop -s $DATA_NODE_IP -u $UMBOX_NAME

bash clear_redirection.sh $DATA_NODE_IP $IOT_DEVICE_IP