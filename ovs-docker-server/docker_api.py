import sys
import subprocess

from flask import Flask
from flask_restful import Api, Resource

CONTROL_NETWORK = "control-net"
OVS_BRIDGE = "ovs-br"

BASE_URL = "/ovs-docker"
STATUS_KEY = "status"
OK_VALUE = "ok"
ERROR_VALUE = "error"
IN_PORTNAME_KEY = "in_port_name"
OUT_PORTNAME_KEY = "out_port_name"
ESC_PORTNAME_KEY = "esc_port_name"

RUN_CMD = "docker run --rm -dit --network {} --name {} {}"
OVS_ADD_PORT_CMD = "sudo ovs-docker add-port {} {} {}"

GET_PORT_NAME_CMD = 'sudo ovs-vsctl --data=bare --no-heading --columns=name find interface external_ids:container_id="{}}" external_ids:container_iface="{}}"'

STOP_CMD= "docker container stop {}"
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
        print error_output

    # Show output, if any.
    if len(normal_output) > 0:
        print normal_output

    print("Finished executing command")
    return normal_output


class DockerContainer(Resource):
    """Resource for handling OVS-connected docker images."""

    def post(self, image_name, instance_name):
        try:
            # Start docker instance.
            print("Starting container")
            run_command(RUN_CMD.format(CONTROL_NETWORK, instance_name, image_name))

            # Connect OVS ports.
            print("Connecting OVS ports.")
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth1", instance_name))
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth2", instance_name))
            run_command(OVS_ADD_PORT_CMD.format(OVS_BRIDGE, "eth3", instance_name))

            # Get port names.
            eth1_portname = run_command(GET_PORT_NAME_CMD.format(instance_name, "eth1"))
            eth2_portname = run_command(GET_PORT_NAME_CMD.format(instance_name, "eth2"))
            eth3_portname = run_command(GET_PORT_NAME_CMD.format(instance_name, "eth3"))

            # Return OVS port names.
            print("OVS connection successful")
            return {STATUS_KEY: OK_VALUE, IN_PORTNAME_KEY: eth1_portname, OUT_PORTNAME_KEY: eth2_portname, ESC_PORTNAME_KEY: eth3_portname}
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
    api.add_resource(DockerContainer, BASE_URL + "/<string:image_name>/<string:instance_name>")
    app.run(host="0.0.0.0", debug=True)


if __name__ == "__main__":
    main()
