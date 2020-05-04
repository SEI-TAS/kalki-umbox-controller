# OVS-Docker-Server

## Prerequisites
 - Docker and OVSSwitch should be installed before using this application. If on a Ubuntu/Debian system, the script `install_packages.sh` should be able to install and setup both properly.
 
## Configuration
The `config.sh` file has several configurable parameters, that need to be set up before fully installing and using this server.

 - CONTROL_NIC: has to point to the network interface card that will be used to communicate with the Control Node.
 - IOT_NIC: has to point to the network interface card that will connect to the IoT devices/sensors.
 - EXT_NIC: has to point to the network interface card that will connect to any external network/clients.
 - CONTROL_NET_PREFIX: has to be configured to the prefix for the IP network used to communicate with the Control node on CONTROL_NIC.
 - IP_ON_CONTROL_NIC: needs to be modified to indicate the IP address of the data node in the control network.
 - CONTROL_NET: if needed, needs to be modified to indicate the netmask for that same control network.
 - UMBOX_IP_RANGE: if needed, can be modified to change the range of IPs that will be assigned to umboxes on this same control network.
 
## Usage
All commands have to be executed from the ovs-docker-server folder to work properly.

Before using the server, some steps need to be performed:
 - Create a Docker image of the server. This needs to be done only once, unless the components of the server change. To do this, run `bash build_container.sh`.

Note that all OVS network rules and running docker umboxes will be reset each time the server is started.

To start this server in a Docker container using Docker-Compose, execute the following:

`bash run_compose.sh`  

When exiting the log view after running this, containers will continue running in the background. 

If the log window is exited, the logs can be still monitored with this command:

`bash compose_logs.sh`

To stop the server and clean up all resources, execute this:

`bash stop_compose.sh`
