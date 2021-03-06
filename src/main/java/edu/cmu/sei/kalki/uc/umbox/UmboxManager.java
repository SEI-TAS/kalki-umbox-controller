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

import edu.cmu.sei.kalki.db.daos.DeviceDAO;
import edu.cmu.sei.kalki.db.daos.SecurityStateDAO;
import edu.cmu.sei.kalki.db.daos.UmboxImageDAO;
import edu.cmu.sei.kalki.db.daos.UmboxInstanceDAO;
import edu.cmu.sei.kalki.uc.ovs.OpenFlowRule;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSDB;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSSwitch;
import edu.cmu.sei.kalki.db.database.Postgres;
import edu.cmu.sei.kalki.db.listeners.InsertListener;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.SecurityState;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.models.UmboxInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UmboxManager
{
    private final static String OVS_DEVICES_NETWORK_PORT = "1";
    private final static String OVS_EXTERNAL_NETWORK_PORT = "2";

    protected static final Logger logger = Logger.getLogger(UmboxManager.class.getName());

    private RemoteOVSSwitch ovsSwitch = new RemoteOVSSwitch();
    private RemoteOVSDB ovsDB = new RemoteOVSDB();

    /**
     * Configure the umbox class to use.
     * @param umboxClass
     * @throws ClassNotFoundException
     */
    public void setUmboxClass(String umboxClass) throws ClassNotFoundException {
        Umbox.setUmboxClass(umboxClass);
    }

    /**
     * Goes over all devices and sets up umboxes for each of them based on their current state.
     */
    public void bootstrap()
    {
        // Set up umboxes for existing devices.
        List<Device> devices = DeviceDAO.findAllDevices();
        for(Device device : devices)
        {
            logger.info("Checking if there are umboxes to be started for device " + device.getName() + ", state " + device.getCurrentState().getName());
            DeviceSecurityState state = device.getCurrentState();
            SecurityState currState = SecurityStateDAO.findSecurityState(state.getStateId());
            setupUmboxesForDevice(device, currState);
        }
    }

    /**
     * Starts up the listener for policy rule log.
     */
    public void startUpDBListener()
    {
        InsertListener.addHandler(Postgres.TRIGGER_NOTIF_NEW_DEV_SEC_STATE, new DeviceSecurityStateInsertHandler());
        //InsertListener.addHandler(Postgres.TRIGGER_NOTIF_NEW_POLICY_INSTANCE, new PolicyInstanceInsertHandler());
        InsertListener.startListening();
    }

    /**
     * Stops up the listener for policy rule log.
     */
    public void stopDBListener()
    {
        InsertListener.stopListening();
    }

    /**
     * Sets up all umboxes for a given device and state. Also clears up previous umboxes if needed.
     * @param device
     * @param currentState
     */
    public synchronized void setupUmboxesForDevice(Device device, SecurityState currentState)
    {
        List<UmboxInstance> oldUmboxInstances = UmboxInstanceDAO.findUmboxInstances(device.getId());
        logger.info("Found old umbox instances info for device, umboxes running: " + oldUmboxInstances.size());

        // First find umbox images for this device/state.
        List<UmboxImage> umboxImages = UmboxImageDAO.findUmboxImagesByDeviceTypeAndSecState(device.getType().getId(), currentState.getId());
        logger.info("Found umboxes for device type " + device.getType().getId() + " and current state " + currentState.getId() + ", number of umboxes: " + umboxImages.size());
        if(umboxImages.size() == 0)
        {
            logger.info("No umboxes associated to this state for this device.");
            return;
        }

        // Then create new umbox instances.
        List<Umbox> newUmboxes = new ArrayList<>();
        for (UmboxImage image : umboxImages)
        {
            try
            {
                logger.info("Starting umbox instance.");
                Umbox newUmbox = setupUmboxForDevice(image, device);
                newUmboxes.add(newUmbox);
            }
            catch (RuntimeException e)
            {
                logger.warning("Error setting up umbox: " + e.toString());
                e.printStackTrace();
            }
        }

        // Now set up rules between umboxes and networks, and between themselves.
        logger.info("Setting up rules for umboxes.");
        clearRedirectForDevice(device);
        setRedirectForDevice(device, OVS_DEVICES_NETWORK_PORT, OVS_EXTERNAL_NETWORK_PORT, newUmboxes);

        // Finally clear the old umboxes.
        stopUmboxes(oldUmboxInstances);
    }

    /**
     * Starts an umbox with the given image and device, getting info about how it is connected.
     * @param image
     * @param device
     */
    private Umbox setupUmboxForDevice(UmboxImage image, Device device)
    {
        Umbox umbox = Umbox.createUmbox(image, device);
        umbox.startAndStore();

        if(umbox.getOvsInPortId().equals("") || umbox.getOvsOutPortId().equals("") || umbox.getOvsRepliesPortId().equals(""))
        {
            // Get the port ids from the names with a remote API call.
            ovsDB.setServer(device.getDataNode().getIpAddress());
            String umboxInPortId = ovsDB.getPortId(umbox.getOvsInPortName());
            String umboxOutPortId = ovsDB.getPortId(umbox.getOvsOutPortName());
            String umboxRepliesPortId = ovsDB.getPortId(umbox.getOvsRepliesPortName());
            if (umboxInPortId == null || umboxOutPortId == null || umboxRepliesPortId == null)
            {
                throw new RuntimeException("Could not get port ids!");
            }

            umbox.setOvsInPortId(umboxInPortId);
            umbox.setOvsOutPortId(umboxOutPortId);
            umbox.setOvsRepliesPortId(umboxRepliesPortId);
        }
        else
        {
            logger.info("Port IDs were already received, not sending extra request to get them.");
        }

        return umbox;
    }

    /**
     * Stops all umboxes for the given device, and clears all rules to them.
     * @param device
     */
    public void clearAllUmboxesForDevice(Device device)
    {
        logger.info("Clearing all umboxes for this device.");
        clearRedirectForDevice(device);
        List<UmboxInstance> instances = UmboxInstanceDAO.findUmboxInstances(device.getId());
        stopUmboxes(instances);
    }

    /**
     * Stops all umbox instances provided.
     * @param umboxes
     */
    private void stopUmboxes(List<UmboxInstance> umboxes)
    {
        logger.info("Stopping all umboxes given: " + umboxes.size());
        for(UmboxInstance instance : umboxes)
        {
            UmboxImage image = UmboxImageDAO.findUmboxImage(instance.getUmboxImageId());
            Device device = DeviceDAO.findDevice(instance.getDeviceId());
            Umbox umbox = Umbox.createUmbox(image, device, Integer.parseInt(instance.getAlerterId()));
            umbox.stopAndClear();
        }
    }

    /**
     */
    private void setRedirectForDevice(Device device, String ovsDevicePort, String ovsExternalPort, List<Umbox> umboxes)
    {
        if(umboxes.size() == 0)
        {
            logger.info("No umboxes, no rules to set up.");
            return;
        }

        String cleanDeviceIp = cleanDeviceIp(device.getIp());

        List<OpenFlowRule> rules = new ArrayList<>();

        // Setup entry rules for umbox chain.
        logger.info("Creating entry rules for device: " + cleanDeviceIp);
        Umbox firstUmbox = umboxes.get(0);
        rules.add(new OpenFlowRule(ovsExternalPort, firstUmbox.getOvsInPortId(), "100", null, cleanDeviceIp));
        rules.add(new OpenFlowRule(ovsDevicePort, firstUmbox.getOvsInPortId(), "100", cleanDeviceIp, null));
        rules.add(new OpenFlowRule(firstUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));

        // Setup intermediate rules for umbox chain.
        logger.info("Creating intermediate rules for device: " + cleanDeviceIp);
        String prevUmboxOutPortId = firstUmbox.getOvsOutPortId();
        for(int i = 1; i < umboxes.size(); i++)
        {
            // We could use only 1 rule here without src/dest IP, but we use two to make it easier later to delete all rules associated to the device IP.
            Umbox currUmbox = umboxes.get(i);
            rules.add(new OpenFlowRule(prevUmboxOutPortId, currUmbox.getOvsInPortId(), "100", null, cleanDeviceIp));
            rules.add(new OpenFlowRule(prevUmboxOutPortId, currUmbox.getOvsInPortId(), "100", cleanDeviceIp, null));
            rules.add(new OpenFlowRule(currUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));
            prevUmboxOutPortId = currUmbox.getOvsOutPortId();
        }

        // Setup exit rules for umbox chain.
        logger.info("Creating exit rules for device: " + cleanDeviceIp);
        Umbox lastUmbox = umboxes.get(umboxes.size() - 1);
        rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsDevicePort, "100", null, cleanDeviceIp));
        rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsExternalPort, "100", cleanDeviceIp, null));
        rules.add(new OpenFlowRule(lastUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));

        // Set the OVS switch to actually store the rules.
        logger.info("Sending rules for device: " + cleanDeviceIp);
        ovsSwitch.setServer(device.getDataNode().getIpAddress());
        for(OpenFlowRule rule : rules)
        {
            ovsSwitch.addRule(rule);
        }
    }

    /**
     * Clears all rules related to incoming and outgoing traffic for a given device.
     * @param device
     */
    private void clearRedirectForDevice(Device device)
    {
        logger.info("Clearing up rules for device: " + device.getIp());

        String cleanDeviceIp = cleanDeviceIp(device.getIp());

        OpenFlowRule allFromDevice = new OpenFlowRule(null, null, null, cleanDeviceIp, null);
        OpenFlowRule allToDevice = new OpenFlowRule(null, null, null, null, cleanDeviceIp);

        ovsSwitch.setServer(device.getDataNode().getIpAddress());
        ovsSwitch.removeRule(allFromDevice);
        ovsSwitch.removeRule(allToDevice);
    }

    /**
     * Clean out any ports, if present, in the IP field.
     * @param deviceIp
     * @return
     */
    private String cleanDeviceIp(String deviceIp)
    {
        String cleanDeviceIp = deviceIp.split(":")[0];
        logger.info("Cleaned device IP: " + cleanDeviceIp);
        return cleanDeviceIp;
    }
}
