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
package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.db.utils.CommandExecutor;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VMUmbox extends Umbox
{
    private static final String UMBOX_TOOL_PATH = "./vm-umbox-tool";
    private static final String UMBOX_TOOL_FILE = "umbox.py";

    protected static final Logger logger = Logger.getLogger(VMUmbox.class.getName());

    private ArrayList<String> commandInfo;
    private String commandWorkingDir;

    public VMUmbox(UmboxImage image, Device device)
    {
        super(image, device);
        setupCommand();
    }

    public VMUmbox(UmboxImage image, Device device, int instanceId)
    {
        super(image, device, instanceId);
        setupCommand();
    }

    /***
     * Common parameters that are the same (needed or optional) for all comands.
     */
    private void setupCommand()
    {
        String dataNodeIP = device.getDataNode().getIpAddress();

        commandWorkingDir = Paths.get(System.getProperty("user.dir"), UMBOX_TOOL_PATH).toString();

        // Basic command parameters.
        commandInfo = new ArrayList<>();
        commandInfo.add("pipenv");
        commandInfo.add("run");
        commandInfo.add("python");
        commandInfo.add(UMBOX_TOOL_FILE);
        commandInfo.add("-s");
        commandInfo.add(dataNodeIP);
        commandInfo.add("-u");
        commandInfo.add(String.valueOf(umboxId));
        if(image != null)
        {
            commandInfo.add("-i");
            commandInfo.add(image.getName());
            commandInfo.add("-f");
            commandInfo.add(image.getFileName());
        }
    }

    /**
     * Starts a new umbox.
     */
    @Override
    protected boolean start()
    {
        List<String> output = null;
        List<String> command = (ArrayList) commandInfo.clone();
        command.add("-c");
        command.add("start");

        try
        {
            output = CommandExecutor.executeCommand(command, commandWorkingDir);

            // Assuming the port name was the last thing printed in the output, get it and process it.
            String ovsPortNames = output.get(output.size() - 1);
            logger.info("Umbox port names: " + ovsPortNames);
            if (ovsPortNames == null)
            {
                throw new RuntimeException("Could not get umbox OVS ports!");
            }

            String[] portNames = ovsPortNames.split(" ");
            if(portNames.length != 3)
            {
                throw new RuntimeException("Could not get 3 OVS port names!");
            }

            // Locally store the port names.
            this.setOvsInPortName(portNames[0]);
            this.setOvsOutPortName(portNames[1]);
            this.setOvsRepliesPortName(portNames[2]);

            return true;
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Could not start umbox properly!");
        }
    }

    /**
     * Stops a running umbox.
     */
    @Override
    protected boolean stop()
    {
        List<String> command = (ArrayList) commandInfo.clone();
        command.add("-c");
        command.add("stop");

        try
        {
            logger.info("Executing stop command.");
            CommandExecutor.executeCommand(command, commandWorkingDir);
            return true;
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Could not stop umbox properly!");
        }
    }

}
