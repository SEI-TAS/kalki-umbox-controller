#!/usr/bin/env bash

source prepare_env.sh
docker-compose up -d
bash compose_logs.sh
