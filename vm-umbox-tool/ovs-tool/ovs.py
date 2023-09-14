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
import subprocess
import shlex
from argparse import ArgumentParser

OF_COMMAND_BASE = "sudo ovs-ofctl -O OpenFlow13"
OF_COMMAND_SERVER = "tcp:{}:{}"
DEFAULT_SWITCH_PORT = 6653

DEFAULT_PRIORITY = "200"

OVSDB_COMMAND_BASE = "sudo ovs-vsctl"
OVSDB_COMMAND_SERVER = "--db=tcp:{}:{}"
DEFAULT_OVSDB_PORT = 6654

class OpenFlowRule(object):
    """Represents an OF rule."""

    def __init__(self, in_port, out_port, proto=None, nw_dest_port=None):
        self.in_port = in_port
        self.out_port = out_port
        self.proto = proto
        self.nw_dest_port = nw_dest_port

        self.type = "ip"
        self.priority = DEFAULT_PRIORITY
        self.src_ip = None
        self.dest_ip = None

    def build_rule(self):
        """Creates a string with the flow rule from the information in this object."""
        rule_string = "\""

        if self.type is not None:
            rule_string += "{}, ".format(self.type)

        if self.proto is not None:
            rule_string += "nw_proto={}, ".format(self.proto)

        if self.nw_dest_port is not None:
            rule_string += "tcp_dst={}, ".format(self.nw_dest_port)

        if self.priority is not None:
            rule_string += "priority={}, ".format(self.priority)

        if self.in_port is not None:
            rule_string += "in_port={}, ".format(self.in_port)

        if self.src_ip is not None:
            rule_string += "ip_src={}, ".format(self.src_ip)

        if self.dest_ip is not None:
            rule_string += "ip_dst={}, ".format(self.dest_ip)

        if self.out_port is not None:
            rule_string += "actions=output:{}, ".format(self.out_port)
        elif self.out_port == -1:
            rule_string += "actions=drop"

        return rule_string + "\""


class RemoteVSwitch(object):
    """Represents a remove OVS switch. Communicates to it through OpenFlow using the ovs-ofctl local command line
    tool."""

    def __init__(self, server_ip, switch_port):
        self.server_ip = server_ip
        self.switch_port = switch_port

    def _send_openflow_command(self, command, arguments=""):
        """Sends an OpenFlow command through ovs-ofctl to a remove ovsswitchd server."""
        try:
            server_info = OF_COMMAND_SERVER.format(self.server_ip, self.switch_port)
            full_command = OF_COMMAND_BASE + " " + command + " " + server_info + " " + arguments
            print("Executing command: " + full_command)
            output = subprocess.check_output(shlex.split(full_command))
            print("Output of command: " + output)
            return output.rstrip()
        except subprocess.CalledProcessError as e:
            print("Error executing command: " + str(e))

    def execute_show_command(self):
        """Sends the show command through OF to remote switch."""
        return self._send_openflow_command("show")

    def execute_dump_flows_command(self):
        """Send the dump-flows command through OF to remote switch."""
        return self._send_openflow_command("dump-flows")

    def add_rule(self, of_rule):
        """Adds a new rule/flow to the switch."""
        rule_string = of_rule.build_rule()
        print("Adding rule: " + rule_string)
        self._send_openflow_command("add-flow", rule_string)

    def remove_rule(self, of_rule):
        """Removes a rule/flow from the switch."""
        rule_string = of_rule.build_rule()
        print("Removing rule: " + rule_string)
        self._send_openflow_command("del-flows", rule_string)


class RemoteOVSDB(object):
    """Represents a remove OVS DB. Communicates to it through OpenFlow using the ovs-vsctl local command line
    tool."""

    def __init__(self, server_ip, db_port):
        self.server_ip = server_ip
        self.db_port = db_port

    def _send_db_command(self, command, arguments=""):
        """Sends an DB command through ovs-vsctl to a remove ovsdb server."""
        try:
            server_info = OVSDB_COMMAND_SERVER.format(self.server_ip, self.db_port)
            full_command = OVSDB_COMMAND_BASE + " " + server_info + " " + command + " " + arguments
            print("Executing DB command: " + full_command)
            output = subprocess.check_output(shlex.split(full_command))
            print("Output of command: " + output)
            return output.rstrip()
        except subprocess.CalledProcessError as e:
            print("Error executing command: " + str(e))

    def get_port_id(self, port_name):
        """Gets the id of a port given its id."""
        port_name_command = "get Interface {} ofport".format(port_name)
        return self._send_db_command(port_name_command)


def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument("-c", "--command", dest="command", required=True, help="Command: start or stop")
    parser.add_argument("-s", "--server", dest="datanodeip", required=True, help="IP of the data node server")
    parser.add_argument("-si", "--sourceip", dest="sourceip", required=False, help="Source IP")
    parser.add_argument("-di", "--destip", dest="destip", required=False, help="Destination IP")
    parser.add_argument("-i", "--inport", dest="inport", required=False, help="Input port name/number on virtual switch")
    parser.add_argument("-o", "--outport", dest="outport", required=False, help="Output port name/number on virtual switch")
    parser.add_argument("-p", "--priority", dest="priority", required=False, help="Priority")
    parser.add_argument("-pr", "--proto", dest="proto", required=False, help="IP Protocol")
    parser.add_argument("-ndp", "--nw_dest_port", dest="nw_dest_port", required=False, help="IP Dest Port")
    args = parser.parse_args()
    return args


def get_port_number(ovsdbip, port_name):
    if port_name is None:
        return None

    try:
        port_number = int(port_name)
        return port_number
    except ValueError:
        ovsdb = RemoteOVSDB(ovsdbip, DEFAULT_OVSDB_PORT)
        port_number = ovsdb.get_port_id(str(port_name))
        if port_number is None:
            print("Unable to obtain port number for {}; aborting.".format(port_name))
            exit(1)

        print("Port number for {} is {}".format(port_name, port_number))
        return port_number


def main():
    args = parse_arguments()
    print("Command: " + args.command)
    print("Data node to use: " + args.datanodeip)
    switch = RemoteVSwitch(args.datanodeip, DEFAULT_SWITCH_PORT)

    if args.command == "add_rule" or args.command == "del_rule":
        print("Protocol: " + str(args.proto))
        print("Dest NW Port: " + str(args.nw_dest_port))
        print("Source IP: " + str(args.sourceip))
        print("Destination IP: " + str(args.destip))
        print("Input port name: " + str(args.inport))
        print("Output port name: " + str(args.outport))
        print("Priority: " + str(args.priority))

        # First get port numbers if needed.
        in_port_number = get_port_number(args.datanodeip, args.inport)
        out_port_number = get_port_number(args.datanodeip, args.outport)

        # Set up rule with received IPs and OVS ports.
        rule = OpenFlowRule(in_port_number, out_port_number, args.proto, args.nw_dest_port)
        rule.src_ip = args.sourceip
        rule.dest_ip = args.destip
        if args.priority is not None:
            rule.priority = args.priority

        if args.command == "add_rule":
            switch.add_rule(rule)
        else:
            # Have to disable priority and output as that is not accepted when deleting flows.
            rule.priority = None
            rule.out_port = None
            switch.remove_rule(rule)
    else:
        switch.execute_show_command()
        switch.execute_dump_flows_command()


if __name__ == "__main__":
    main()
