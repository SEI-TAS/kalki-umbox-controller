#!/usr/bin/env bash
HOST_TZ=$(cat /etc/timezone)
docker run -p 6060:6060 --rm --net=host -e TZ=${HOST_TZ} --name kalki-uc kalki/kalki-uc $1
