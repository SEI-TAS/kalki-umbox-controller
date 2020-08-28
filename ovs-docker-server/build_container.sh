#!/usr/bin/env bash

SKIP_TESTS_ARG=""
if [ "$1" == "--skip_tests" ]; then
  SKIP_TESTS_ARG=" -x test "
fi

docker build --build-arg SKIP_TESTS="${SKIP_TESTS_ARG}" -t kalki/kalki-ovs-docker-server .
docker pull busybox:latest
