# 
#  Kalki - A Software-Defined IoT Security Platform
#  Copyright 2020 Carnegie Mellon University.
#  NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
#  Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.
#  [DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see Copyright notice for non-US Government use and distribution.
#  This Software includes and/or makes use of the following Third-Party Software subject to its own license:
#  1. Google Guava (https://github.com/google/guava) Copyright 2007 The Guava Authors.
#  2. JSON.simple (https://code.google.com/archive/p/json-simple/) Copyright 2006-2009 Yidong Fang, Chris Nokleberg.
#  3. JUnit (https://junit.org/junit5/docs/5.0.1/api/overview-summary.html) Copyright 2020 The JUnit Team.
#  4. Play Framework (https://www.playframework.com/) Copyright 2020 Lightbend Inc..
#  5. PostgreSQL (https://opensource.org/licenses/postgresql) Copyright 1996-2020 The PostgreSQL Global Development Group.
#  6. Jackson (https://github.com/FasterXML/jackson-core) Copyright 2013 FasterXML.
#  7. JSON (https://www.json.org/license.html) Copyright 2002 JSON.org.
#  8. Apache Commons (https://commons.apache.org/) Copyright 2004 The Apache Software Foundation.
#  9. RuleBook (https://github.com/deliveredtechnologies/rulebook/blob/develop/LICENSE.txt) Copyright 2020 Delivered Technologies.
#  10. SLF4J (http://www.slf4j.org/license.html) Copyright 2004-2017 QOS.ch.
#  11. Eclipse Jetty (https://www.eclipse.org/jetty/licenses.html) Copyright 1995-2020 Mort Bay Consulting Pty Ltd and others..
#  12. Mockito (https://github.com/mockito/mockito/wiki/License) Copyright 2007 Mockito contributors.
#  13. SubEtha SMTP (https://github.com/voodoodyne/subethasmtp) Copyright 2006-2007 SubEthaMail.org.
#  14. JSch - Java Secure Channel (http://www.jcraft.com/jsch/) Copyright 2002-2015 Atsuhiko Yamanaka, JCraft,Inc. .
#  15. ouimeaux (https://github.com/iancmcc/ouimeaux) Copyright 2014 Ian McCracken.
#  16. Flask (https://github.com/pallets/flask) Copyright 2010 Pallets.
#  17. Flask-RESTful (https://github.com/flask-restful/flask-restful) Copyright 2013 Twilio, Inc..
#  18. libvirt-python (https://github.com/libvirt/libvirt-python) Copyright 2016 RedHat, Fedora project.
#  19. Requests: HTTP for Humans (https://github.com/psf/requests) Copyright 2019 Kenneth Reitz.
#  20. netifaces (https://github.com/al45tair/netifaces) Copyright 2007-2018 Alastair Houghton.
#  21. ipaddress (https://github.com/phihag/ipaddress) Copyright 2001-2014 Python Software Foundation.
#  DM20-0543
#
#
import sys
import subprocess
import urllib.parse

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
ERROR_DETAILS_KEY = "error"
IN_PORTID_KEY = "in_port_id"
OUT_PORTID_KEY = "out_port_id"
ESC_PORTID_KEY = "esc_port_id"

# Docker and OVS commands.
RUN_CMD = "docker run --rm -dit --network {} --hostname {} --cap-add NET_ADMIN --name {} {} {}"
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
            image_name = urllib.parse.unquote(image_name)
            container_name = urllib.parse.unquote(container_name)
            ip_address = urllib.parse.unquote(ip_address)

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
            image_name = urllib.parse.unquote(image_name)
            instance_name = urllib.parse.unquote(instance_name)

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

    try:
        app.run(host=binding_ip, port=API_PORT, debug=False)
    except Exception as e:
        print("Could not bind to listen on given address (" + binding_ip + "): " + str(e))


if __name__ == "__main__":
    main()
