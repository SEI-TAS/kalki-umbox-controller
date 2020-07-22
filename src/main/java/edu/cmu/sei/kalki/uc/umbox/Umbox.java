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

import edu.cmu.sei.kalki.db.daos.UmboxInstanceDAO;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.models.UmboxInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.logging.Logger;

public abstract class Umbox
{
    private static final int MAX_INSTANCES = 1000;

    protected static final Logger logger = Logger.getLogger(Umbox.class.getName());

    public static Class umboxClass;

    protected int umboxId;
    protected Device device;
    protected UmboxImage image;
    protected String ovsInPortName = "";
    protected String ovsOutPortName = "";
    protected String ovsRepliesPortName = "";
    protected String ovsInPortId = "";
    protected String ovsOutPortId = "";
    protected String ovsRepliesPortId = "";

    public static void setUmboxClass(Class umboxClassToUse)
    {
        umboxClass = umboxClassToUse;
    }

    public static void setUmboxClass(String umboxClassToUse) throws ClassNotFoundException {
        umboxClass = Class.forName(umboxClassToUse);
    }

    public static Umbox createUmbox(UmboxImage image, Device device)
    {
        try {
            Constructor con = umboxClass.getConstructor(UmboxImage.class, Device.class);
            return (Umbox) con.newInstance(image, device);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            throw new RuntimeException("Could not create umbox for the given image and device: " + e.getMessage());
        }
    }

    public static Umbox createUmbox(UmboxImage image, Device device, int instanceId)
    {
        try {
            Constructor con = umboxClass.getConstructor(UmboxImage.class, Device.class, Integer.TYPE);
            return (Umbox) con.newInstance(image, device, instanceId);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            throw new RuntimeException("Could not create umbox for the given image and device: " + e.getMessage());
        }
    }

    /***
     * Constructor for new umboxes.
     * @param device
     * @param image
     */
    protected Umbox(UmboxImage image, Device device)
    {
        this.image = image;
        this.device = device;

        // Generate random id. Check if there is no instance with this ID, and re-generate if there is.
        int tries = 0;
        do
        {
            Random rand = new Random();
            umboxId = rand.nextInt(MAX_INSTANCES);
            tries++;

            if(tries > MAX_INSTANCES)
            {
                throw new RuntimeException("Can't allocate an ID for a new umbox; all of them seem to be allocated.");
            }
        }
        while(UmboxInstanceDAO.findUmboxInstance(String.valueOf(umboxId)) != null);
    }

    /***
     * Constructor for existing umboxes.
     * @param instanceId
     */
    protected Umbox(UmboxImage image, Device device, int instanceId)
    {
        this.image = image;
        this.device = device;
        this.umboxId = instanceId;
    }

    /**
     * Starts a new umbox and stores its info in the DB.
     */
    public boolean startAndStore()
    {
        UmboxInstance instance = null;
        try
        {
            // Store in the DB the information about the newly created umbox instance.
            instance = new UmboxInstance(String.valueOf(umboxId), image.getId(), device.getId());
            instance.insert();

            logger.info("Starting umbox.");
            return start();
        }
        catch (RuntimeException e)
        {
            logger.warning("Error starting umbox: " + e.toString());
            e.printStackTrace();

            try
            {
                if (instance != null)
                {
                    UmboxInstanceDAO.deleteUmboxInstance(instance.getId());
                }
            }
            catch(Exception ex)
            {
                logger.severe("Error removing instance not properly created: " + ex.toString());
            }

            return false;
        }
    }

    /**
     * Stops a running umbox and clears its info from the DB.
     */
    public boolean stopAndClear()
    {
        try
        {
            logger.info("Stopping umbox.");
            boolean success = stop();

            UmboxInstance umboxInstance = UmboxInstanceDAO.findUmboxInstance(String.valueOf(umboxId));
            logger.info("Deleting umbox instance from DB.");
            UmboxInstanceDAO.deleteUmboxInstance(umboxInstance.getId());
            return success;
        }
        catch (RuntimeException e)
        {
            logger.warning("Error stopping umbox: " + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Starts a new umbox.
     */
    protected abstract boolean start();

    /**
     * Stops a running umbox.
     */
    protected abstract boolean stop();

    // Getters and setters.

    public String getOvsInPortName()
    {
        return ovsInPortName;
    }

    public void setOvsInPortName(String ovsInPortName)
    {
        this.ovsInPortName = ovsInPortName;
    }

    public String getOvsOutPortName()
    {
        return ovsOutPortName;
    }

    public void setOvsOutPortName(String ovsOutPortName)
    {
        this.ovsOutPortName = ovsOutPortName;
    }

    public String getOvsInPortId()
    {
        return ovsInPortId;
    }

    public void setOvsInPortId(String ovsInPortId)
    {
        this.ovsInPortId = ovsInPortId;
    }

    public String getOvsOutPortId()
    {
        return ovsOutPortId;
    }

    public void setOvsOutPortId(String ovsOutPortId)
    {
        this.ovsOutPortId = ovsOutPortId;
    }

    public String getOvsRepliesPortName()
    {
        return ovsRepliesPortName;
    }

    public void setOvsRepliesPortName(String ovsRepliesPortName)
    {
        this.ovsRepliesPortName = ovsRepliesPortName;
    }

    public String getOvsRepliesPortId()
    {
        return ovsRepliesPortId;
    }

    public void setOvsRepliesPortId(String ovsRepliesPortId)
    {
        this.ovsRepliesPortId = ovsRepliesPortId;
    }
}
