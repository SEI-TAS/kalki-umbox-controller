#!/usr/bin/env bash

# For macOS only, this is needed to enable the HTTP REST API. This is using port 1234.
docker run -d -v /var/run/docker.sock:/var/run/docker.sock -p 127.0.0.1:1234:1234 bobrik/socat TCP-LISTEN:1234,fork UNIX-CONNECT:/var/run/docker.sock

# This needs to be executed on the shell where this will be run, or the variable set in an IntelliJ config.
export DOCKER_HOST=tcp://localhost:1234
