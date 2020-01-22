#!/usr/bin/env bash
# $1: IP address of data node.
# $2: IP address of device to redirect traffic from.
python ovs.py -c del_rule -s $1 -di $2
