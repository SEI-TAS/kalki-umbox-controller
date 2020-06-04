#!/usr/bin/env bash
source config.sh
exec python docker_api.py "${IP_ON_CONTROL_NIC}"
