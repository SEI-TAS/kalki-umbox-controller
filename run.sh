#!/usr/bin/env bash
HOST_TZ=$(cat /etc/timezone)
docker run -p 6060:6060 --rm --net=host -e TZ=${HOST_TZ} -v ~/kalki/kalki-umbox-controller/tests:/app/kalki-umbox-controller-1.0-SNAPSHOT/tests --name kalki-uc $1 $2 $3
