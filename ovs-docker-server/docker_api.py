import sys
import subprocess

from flask import Flask
from flask_restful import Api, Resource

# Existing networks and bridges.
CONTROL_NETWORK = "ovsdockerserver_control-net"
OVS_BRIDGE = "ovs-br"

# API info.
API_BASE_URL = "/ovs-docker"
API_PORT = "5500"

# Reply keys and values.
STATUS_KEY = "status"
OK_VALUE = "ok"
ERROR_VALUE = "error"
ERROR_DETAILS_KEY = "error"
IN_PORTID_KEY = "in_port_id"
OUT_PORTID_KEY = "out_port_id"
ESC_PORTID_KEY = "esc_port_id"

# Docker and OVS commands.
RUN_CMD = "docker run --rm -dit --network {} --hostname {} --name {} {} {}"
STOP_CMD = "docker container stop {}"
OVS_ADD_PORT_CMD = "bash ./ovs-scripts/ovs-docker.sh add-port {} {} {}"
OVS_CLEAR_CMD = "bash ./ovs-scripts/ovs-docker.sh del-ports {} {}"
GET_PORT_ID_CMD = 'ovs-vsctl --data=bare --no-heading --columns=ofport find interface external_ids:container_id="{}" external_ids:container_iface="{}"'


def run_command(command):
    """ Starts the a command in a separate process, and waits for it."""
    print("Executing command: " + command, flush=True)
    tool_pipe = subprocess.PIPE
    tool_process = subprocess.Popen(command, shell=True, stdin=tool_pipe, stdout=tool_pipe, stderr=tool_pipe)
    normal_output, error_output = tool_process.communicate()

    # Show output, if any.
    if len(normal_output) > 0:
        print(normal_output.decode(), flush=True)

    # Show errors, if any.
    if len(error_output) > 0:
        error_msg = "Error executing command: " + error_output.decode()
        print(error_msg, flush=True)
        raise Exception(error_msg)

    print("Finished executing command", flush=True)
    return normal_output.decode()


class DockerContainer(Resource):
    """Resource for handling OVS-connected docker images."""

    def post(self, image_name, container_name, ip_address):
        """Receives the image name, the name to give the container, and the IP of the device being monitored."""
        try:
            # Start docker instance.
            print("Starting container", flush=True)
            run_command(RUN_CMD.format(CONTROL_NETWORK, container_name, container_name, image_name, ip_address))
            print("Container started", flush=True)

            # Connect OVS ports.
            print("Connecting OVS ports.", flush=True)
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth1", container_name))
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth2", container_name))
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth3", container_name))

            # Get port names.
            eth1_portid = run_command(GET_PORT_ID_CMD.format(container_name, "eth1")).rstrip("\n")
            eth2_portid = run_command(GET_PORT_ID_CMD.format(container_name, "eth2")).rstrip("\n")
            eth3_portid = run_command(GET_PORT_ID_CMD.format(container_name, "eth3")).rstrip("\n")
            print("OVS connection successful", flush=True)

            # Return OVS port names.
            return {STATUS_KEY: OK_VALUE, IN_PORTID_KEY: eth1_portid, OUT_PORTID_KEY: eth2_portid, ESC_PORTID_KEY: eth3_portid}
        except Exception as e:
            error_msg = "Error starting docker instance: " + str(e)
            print(error_msg, flush=True)
            return {STATUS_KEY: ERROR_VALUE, ERROR_DETAILS_KEY: error_msg}

    def delete(self, image_name, instance_name, ip_address):
        """Remove an existing docker instance and its OVS connections."""
        try:
            print("Stopping container", flush=True)
            run_command(OVS_CLEAR_CMD.format(OVS_BRIDGE, instance_name))
            run_command(STOP_CMD.format(instance_name))
            print("Stopping successful", flush=True)
            return {STATUS_KEY: OK_VALUE}
        except Exception as e:
            error_msg = "Error stopping docker instance: " + str(e)
            print(error_msg, flush=True)
            return {STATUS_KEY: ERROR_VALUE, ERROR_DETAILS_KEY: error_msg}


def main():
    print("Loading ovs-docker server", flush=True)

    if len(sys.argv) != 2:
        print("Did not receive IP to bind to; exiting")
        exit(-1)
    binding_ip = sys.argv[1]
    print("Will bind to host " + binding_ip, flush=True)

    app = Flask(__name__)
    api = Api(app)
    api.add_resource(DockerContainer, API_BASE_URL + "/<string:image_name>/<string:container_name>/<string:ip_address>")
    app.run(host=binding_ip, port=API_PORT, debug=False)


if __name__ == "__main__":
    main()
