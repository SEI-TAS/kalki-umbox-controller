#!/usr/bin/env bash
cp Dockerfile.vm Dockerfile.tmp
./gradlew docker -i -Pcontainer_name='kalki/kalki-uc-vm'
