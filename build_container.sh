#!/usr/bin/env bash
cp Dockerfile Dockerfile.tmp
./gradlew docker -i -Pcontainer_name=kalki/kalki-uc
