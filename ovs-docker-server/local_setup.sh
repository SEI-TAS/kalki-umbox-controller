#!/usr/bin/env bash

bash setup_docker_network.sh

ENV PIPENV_VENV_IN_PROJECT "enabled"
pipenv install
