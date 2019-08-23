#!/usr/bin/env bash

# DHCP server sometimes does not start properly along with machine.
sudo service isc-dhcp-server restart

export PIPENV_VENV_IN_PROJECT="enabled"
python -m pipenv install
python -m pipenv run python clone_image_api.py