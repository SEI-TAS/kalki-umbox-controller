#!/usr/bin/env bash
# Setup pipenv env in case this is the first time this is called.
export PIPENV_VENV_IN_PROJECT="enabled"
python -m pipenv install
