#!/usr/bin/env bash
HOST_TZ=$(cat /etc/timezone)
docker run --rm -e TZ=${HOST_TZ} \
           --network=host \
           -v ~/kalki/kalki-umbox-controller/tests:/app/kalki-umbox-controller-1.4/tests \
           --name kalki-uc $1 $2 $3
