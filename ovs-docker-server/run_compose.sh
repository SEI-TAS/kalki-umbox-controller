#!/usr/bin/env bash

source prepare_env.sh
export HOST_TZ=$(cat /etc/timezone)
docker-compose up -d
bash compose_logs.sh
