/*
 * Kalki - A Software-Defined IoT Security Platform
 * Copyright 2020 Carnegie Mellon University.
 * NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
 * Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.
 * [DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see Copyright notice for non-US Government use and distribution.
 * This Software includes and/or makes use of the following Third-Party Software subject to its own license:
 * 1. Google Guava (https://github.com/google/guava) Copyright 2007 The Guava Authors.
 * 2. JSON.simple (https://code.google.com/archive/p/json-simple/) Copyright 2006-2009 Yidong Fang, Chris Nokleberg.
 * 3. JUnit (https://junit.org/junit5/docs/5.0.1/api/overview-summary.html) Copyright 2020 The JUnit Team.
 * 4. Play Framework (https://www.playframework.com/) Copyright 2020 Lightbend Inc..
 * 5. PostgreSQL (https://opensource.org/licenses/postgresql) Copyright 1996-2020 The PostgreSQL Global Development Group.
 * 6. Jackson (https://github.com/FasterXML/jackson-core) Copyright 2013 FasterXML.
 * 7. JSON (https://www.json.org/license.html) Copyright 2002 JSON.org.
 * 8. Apache Commons (https://commons.apache.org/) Copyright 2004 The Apache Software Foundation.
 * 9. RuleBook (https://github.com/deliveredtechnologies/rulebook/blob/develop/LICENSE.txt) Copyright 2020 Delivered Technologies.
 * 10. SLF4J (http://www.slf4j.org/license.html) Copyright 2004-2017 QOS.ch.
 * 11. Eclipse Jetty (https://www.eclipse.org/jetty/licenses.html) Copyright 1995-2020 Mort Bay Consulting Pty Ltd and others..
 * 12. Mockito (https://github.com/mockito/mockito/wiki/License) Copyright 2007 Mockito contributors.
 * 13. SubEtha SMTP (https://github.com/voodoodyne/subethasmtp) Copyright 2006-2007 SubEthaMail.org.
 * 14. JSch - Java Secure Channel (http://www.jcraft.com/jsch/) Copyright 2002-2015 Atsuhiko Yamanaka, JCraft,Inc. .
 * 15. ouimeaux (https://github.com/iancmcc/ouimeaux) Copyright 2014 Ian McCracken.
 * 16. Flask (https://github.com/pallets/flask) Copyright 2010 Pallets.
 * 17. Flask-RESTful (https://github.com/flask-restful/flask-restful) Copyright 2013 Twilio, Inc..
 * 18. libvirt-python (https://github.com/libvirt/libvirt-python) Copyright 2016 RedHat, Fedora project.
 * 19. Requests: HTTP for Humans (https://github.com/psf/requests) Copyright 2019 Kenneth Reitz.
 * 20. netifaces (https://github.com/al45tair/netifaces) Copyright 2007-2018 Alastair Houghton.
 * 21. ipaddress (https://github.com/phihag/ipaddress) Copyright 2001-2014 Python Software Foundation.
 * DM20-0543
 *
 */
package edu.cmu.sei.kalki.uc.ovs;

import edu.cmu.sei.kalki.db.utils.CommandExecutor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * Represents a remove OVS switch. Communicates to it through OpenFlow using the ovs-ofctl local command line
 * tool.
 */
public class RemoteOVSSwitch
{
    private static final String TOOL_COMMAND = "ovs-ofctl -O OpenFlow13";
    private static final String SERVER_PARAM = "tcp:{0}:{1,number,#}";

    private static final String CMD_SHOW = "show";
    private static final String CMD_DUMP_FLOWS = "dump-flows";
    private static final String CMD_ADD = "add-flow";
    private static final String CMD_DEL = "del-flows";

    private static final int DEFAULT_PORT = 6653;

    private String serverIp;
    private int port;

    /**
     * Sets the IP of the server.
     */
    public void setServer(String serverIP)
    {
        setServer(serverIP, DEFAULT_PORT);
    }

    /**
     * Sets the IP and port of the server.
     */
    public void setServer(String serverIP, int port)
    {
        this.serverIp = serverIP;
        this.port = port;
    }

    /**
     * Gets information about the OVS switch.
     * @return
     */
    public List<String> getShowInfo()
    {
        return sendCommand(CMD_SHOW, new ArrayList<>());
    }

    /**
     * Gets information about the current flows/rules in the OVS switch.
     */
    public List<String> getFlowsInfo()
    {
        return sendCommand(CMD_DUMP_FLOWS, new ArrayList<>());
    }

    /**
     * Adds a rule to the OVS switch.
     */
    public List<String> addRule(OpenFlowRule rule)
    {
        String ruleCmd = rule.toString();
        return sendCommand(CMD_ADD, new ArrayList<>(Arrays.asList(ruleCmd)));
    }

    /**
     * Removes the given rule from the OVS switch.
     */
    public List<String> removeRule(OpenFlowRule rule)
    {
        String ruleCmd = rule.toString();
        return sendCommand(CMD_DEL, new ArrayList<>(Arrays.asList(ruleCmd)));
    }

    /***
     * Sends a generic command to a remote OVS Switch through the local command line tool.
     */
    private List<String> sendCommand(String command, List<String> arguments)
    {
        if(serverIp == null) {
            throw new RuntimeException("Server IP has not been configured!");
        }

        List<String> commandInfo = new ArrayList<>();
        commandInfo.addAll(Arrays.asList(TOOL_COMMAND.split(" ")));
        commandInfo.add(command);
        commandInfo.add(MessageFormat.format(SERVER_PARAM, serverIp, port));
        commandInfo.addAll(arguments);

        List<String> output = CommandExecutor.executeCommand(commandInfo, "./");
        return output;
    }
}
