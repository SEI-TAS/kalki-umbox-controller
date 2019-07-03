#!/usr/bin/env bash

export PIPENV_VENV_IN_PROJECT="enabled"
python -m pipenv install
python -m pipenv run python clone_image_api.py