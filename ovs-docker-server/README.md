# OVS-Docker-Server

## Prerequisites
 - Docker and OVSSwitch should be installed before using this application. If on a Ubuntu/Debian system, the script `install_packages.sh` should be able to install and setup both properly.
 
## Configuration
The `config.sh` file has several configurable parameters, that need to be set up before fully installing and using this server.

 - CONTROL_NIC: has to point to the network interface card that will be used to communicate with the Control Node.
 - IOT_NIC: has to point to the network interface card that will connect to the IoT devices/sensors.
 - EXT_NIC: has to point to the network interface card that will connect to any external network/clients.
 - CONTROL_NET_PREFIX: has to be configured to the prefix for the IP network used to communicate with the Control node on CONTROL_NIC.
 - CONTROL_NET: if needed, needs to be modified to indicate the netmask for that same control network.
 - UMBOX_IP_RANGE: if needed, can be modified to change the range of IPs that will be assigned to umboxes on this same control network.
 
## Usage
Before using the server, some steps need to be performed:
 - Create a Docker control network needs to be set up. This only needs to be done once. To do this, run `bash setup_docker_network.sh`, after having properly configured `config.sh` (see previous section). 
 - Create a Docker image of the server. This needs to be done only once, unless the components of the server change. To do this, run `bash build_container.sh`.

To start this server in a Docker container, execute the following:

`bash run_container.sh`  

Note that all OVS network rules and running docker umboxes will be reset each time the server is started.

To stop the server, execute this in another console window:

`bash stop_container.sh`
