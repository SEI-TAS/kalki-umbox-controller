#!/usr/bin/env bash

# DHCP server sometimes does not start properly along with machine.
sudo service isc-dhcp-server restart

# Clean up any leftover VMs.
echo "Removing all running umboxes with images still in this folder."
INSTANCE_IMAGES="./images/instances/*.qcow2"
for FILEPATH in ${INSTANCE_IMAGES}; do
    if [ "$FILEPATH" != "$INSTANCE_IMAGES" ]; then
        BASENAME=$(basename $FILEPATH .qcow2)
        echo "Removing VM with name ${BASENAME}"
        sudo virsh destroy $BASENAME
        echo "Removing image file ${FILEPATH}"
        rm -f $FILEPATH
    fi
done

python -m pipenv run python clone_image_api.py
