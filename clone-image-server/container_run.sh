#!/usr/bin/env bash
IMAGE_PATH=$1
docker run  -p 5000:5000 --rm -v $IMAGE_PATH:/app/images --name kalki-clone-image-server kalki/kalki-clone-image-server