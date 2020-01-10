import sys
import subprocess

from flask import Flask
from flask_restful import Api, Resource

# Existing networks and bridges.
CONTROL_NETWORK = "control-net"
OVS_BRIDGE = "ovs-br"

# API info.
API_BASE_URL = "/ovs-docker"
API_PORT = "5500"

# Reply keys and values.
STATUS_KEY = "status"
OK_VALUE = "ok"
ERROR_VALUE = "error"
IN_PORTID_KEY = "in_port_id"
OUT_PORTID_KEY = "out_port_id"
ESC_PORTID_KEY = "esc_port_id"

# Docker and OVS commands.
RUN_CMD = "docker run --rm -dit --network {} --name {} {}"
OVS_ADD_PORT_CMD = "sudo ovs-docker add-port {} {} {}"
GET_PORT_ID_CMD = 'sudo ovs-vsctl --data=bare --no-heading --columns=ofport find interface external_ids:container_id="{}" external_ids:container_iface="{}"'
STOP_CMD = "docker container stop {}"
OVS_CLEAR_CMD = "sudo ovs-docker del-ports {} {}"


def run_command(command):
    """ Starts the a command in a separate process, and waits for it."""
    print("Executing command: " + command)
    sys.stdout.flush()
    tool_pipe = subprocess.PIPE
    tool_process = subprocess.Popen(command, shell=True, stdin=tool_pipe, stdout=tool_pipe, stderr=tool_pipe)
    normal_output, error_output = tool_process.communicate()

    # Show errors, if any.
    if len(error_output) > 0:
        error_msg = "Error executing command: " + error_output
        print(error_msg)
        raise Exception(error_msg)

    # Show output, if any.
    if len(normal_output) > 0:
        print(normal_output)

    print("Finished executing command")
    return normal_output


class DockerContainer(Resource):
    """Resource for handling OVS-connected docker images."""

    def post(self, image_name, container_name):
        try:
            # Start docker instance.
            print("Starting container")
            run_command(RUN_CMD.format(CONTROL_NETWORK, container_name, image_name))
            print("Container started")

            # Connect OVS ports.
            print("Connecting OVS ports.")
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth1", container_name))
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth2", container_name))
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth3", container_name))

            # Get port names.
            eth1_portid = run_command(GET_PORT_ID_CMD.format(container_name, "eth1")).rstrip("\n)")
            eth2_portid = run_command(GET_PORT_ID_CMD.format(container_name, "eth2")).rstrip("\n)")
            eth3_portid = run_command(GET_PORT_ID_CMD.format(container_name, "eth3")).rstrip("\n)")
            print("OVS connection successful")

            # Start the container.
            #print("Starting container")
            #run_command(START_CMD.format(container_name))

            # Return OVS port names.
            return {STATUS_KEY: OK_VALUE, IN_PORTID_KEY: eth1_portid, OUT_PORTID_KEY: eth2_portid, ESC_PORTID_KEY: eth3_portid}
        except Exception as e:
            print("Error starting docker instance: " + str(e))
            return {STATUS_KEY: ERROR_VALUE}

    def delete(self, image_name, instance_name):
        """Remove an existing docker instance and its OVS connections."""
        try:
            print("Stopping container")
            run_command(OVS_CLEAR_CMD.format(OVS_BRIDGE, instance_name))
            run_command(STOP_CMD.format(instance_name))
            print("Stopping successful")
            return {STATUS_KEY: OK_VALUE}
        except Exception as e:
            print("Error stopping docker instance: " + str(e))
            return {STATUS_KEY: ERROR_VALUE}


def main():
    print("Loading ovs-docker server")

    app = Flask(__name__)
    api = Api(app)
    api.add_resource(DockerContainer, API_BASE_URL + "/<string:image_name>/<string:container_name>")
    app.run(host="0.0.0.0", port=API_PORT, debug=True)


if __name__ == "__main__":
    main()
