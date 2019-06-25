#!/usr/bin/env bash

export PIPENV_VENV_IN_PROJECT="enabled"
pipenv install
pipenv run python clone_image_api.py