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
import requests
import json
import config

OD_RESTCONF_PORT = "8181"

# Assuming default auth.
USER = "admin"
PASS = "admin"

NODES_URL = "/opendaylight-inventory:nodes"
FLOW_URL = "/node/{}/table/0/flow/{}"

FLOW_PREFIX = "flow-node-inventory"

# Used to identify a bridge that is not connected to any port, since it is the bridge itself.
BRIDGE_PORT_PLACEHOLDER = 4294967294


def od_restconf_request(method, request_type, request_url, headers={}, payload={}):
    """A generic restconf request to OD."""

    url = "http://" + config.get_config().opendaylight_ip + ":" + OD_RESTCONF_PORT + "/restconf/" + request_type + request_url
    print url
    headers["Accept"] = "application/json"

    if method == "get":
        reply = requests.get(url, headers=headers, auth=(USER, PASS))
    else:
        reply = requests.post(url, payload, headers=headers, auth=(USER, PASS))

    print reply
    print reply.content
    return json.loads(reply.content)


def get_switch_info():
    """Obtains the current switch id from OD."""

    switch_id = ""
    connections = []

    reply = od_restconf_request("get", "operational", NODES_URL)
    print reply

    if len(reply["nodes"]["node"]) > 0:
        json_node = reply["nodes"]["node"][0]

        # Get id.
        if "openflow" in json_node["id"]:
            switch_id = json_node["id"]

        # Get local port and bridge name info.
        for connector in json_node["node-connector"]:
            connection_info = {}

            connection_info["interface"] = connector[FLOW_PREFIX + ":name"]

            connection_info["type"] = "port"
            connection_info["port"] = connector[FLOW_PREFIX + ":port-number"]
            if connection_info["port"] == BRIDGE_PORT_PLACEHOLDER:
                connection_info["port"] = None
                connection_info["type"] = "bridge"

            if "address-tracker:addresses" in connector:
                connection_info["ip"] = connector["address-tracker:addresses"][0]["ip"]
            else:
                connection_info["ip"] = None
            connections.append(connection_info)

    return {"id": switch_id, "connections": connections}


def set_new_flow(switch_id, flow_id, from_port, to_port):
    """Sets a flow for the given switch."""

    defaultPriority = 500
    #flow_instructions.update({"ingressPort": from_port})

    match_data = {}

    action = {}
    action["order"] = 0
    action["output-action"] = {"output-node-connector": to_port, "max-length": 65535}
    instruction_data = {}
    instruction_data["order"] = "0"
    instruction_data["apply-actions"] = {"action": [action]}

    flow_data = {}
    flow_data["strict"] = False
    flow_data["instructions"] = {"instruction": [instruction_data]}
    flow_data["match"] = match_data
    flow_data["table_id"] = 0
    flow_data["id"] = flow_id
    flow_data["cookie_mask"] = 255
    flow_data["installHw"] = False
    flow_data["hard-timeout"] = 12
    flow_data["cookie"] = 4
    flow_data["idle-timeout"] = 34
    flow_data["flow-name"] = ""
    flow_data["priority"] = defaultPriority
    flow_data["barrier"] = False

    flow = {"flow": [flow_data]}

    headers = {"Content-Type": "application/json"}
    reply = od_restconf_request("post", "config", NODES_URL + FLOW_URL.format(switch_id, flow_id),
                                headers=headers, payload=json.dumps(flow))
    return reply


def remove_flow():
    pass
