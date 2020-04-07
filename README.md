# Umbox Controller

## Prerequisites
 - To compile this program, Java JDK 8 is required. This program uses Gradle as its build system, 
but since it uses an included Gradle wrapper, no external Gradle setup is required.
 - To build the Docker image of the program, Docker should be installed first.
 - Kalki-db should be installed on a local Maven repo for this program to compile. 
You can find more details here: https://github.com/SEI-TTG/kalki-db/tree/dev
 - This program requires a Postgres database engine to run. This can be installed manually, or a Docker image
can be used. If the Docker image is used, the Kalki Docker network should be created too.
You can find more details here: https://github.com/SEI-TTG/kalki-db/tree/dev

This program is dependent on several external tools and libraries. If the Docker image is
created, it will automatically include all dependencies. If manual installation of the dependencies 
is needed, see the Dockerfile for details. Here is a list of the main dependencies that are handled by the 
Dockerfile:
 - OVS tools: needed to send OpenFlow commands.
 - VM Umbox Tool dependencies:
   - Libvirt-dev
   - Python 2.7
   - PIP and Pipenv (to set up the Python libraries)
   - Python libraries: Requests and Libvirt-Python
 
## Configuration
The config.json file has several configurable parameters. However, most of them do not need to be
changed from their defaults if the rest of the system is synchronized to use the same values.

Parameters that will usually need to be configured:
 - <b>data_node_ip</b>: the IP address of the Data Node through the control plane. 
 - <b>db_host</b>: the IP address or hostname of the DB server. Usually localhost, but it has to be kalki-db if using
 Docker.
 - <b>db_port</b>: Usually the default port can be used.
 - <b>db_name, db_user, db_password</b>: need to be consistent with the actual DB information being used.
 - <b>db_recreate, db_root_password</b>: only needed if we want DNI to forcefully drop and recreate the DB.
 
## Usage
The simplest way to use this component is by creating a Docker container. First the component
needs to be compiled and a Docker image be created from it. By default, this controller is configured
to handle Docker-based umboxes. To create this image, execute the following command:

`bash build_docker_version.sh`

To execute a container from that image, execute the following command:

`bash run_docker_version.sh`  

A version of this component that supports VM-based umboxes can also be built on demand. This will create a parallel
image that will allow to easily start either version of the component. To do this, execute the following command:

`bash build_vm_version.sh`

To execute a container from that image then, execute the following command:

`bash run_vm_version.sh`