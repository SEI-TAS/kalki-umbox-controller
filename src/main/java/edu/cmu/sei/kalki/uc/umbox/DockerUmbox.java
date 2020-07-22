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

import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class DockerUmbox extends Umbox
{
    private static final String PREFIX = "umbox-";
    private static final String API_BASE_URL = "/ovs-docker";
    private static final String API_PORT = "5500";

    private static final String STATUS_KEY = "status";
    private static final String OK_VALUE = "ok";
    private static final String ERROR_VALUE = "error";
    private static final String ERROR_DETAILS_KEY = "error";
    private static final String IN_PORTID_KEY = "in_port_id";
    private static final String OUT_PORTID_KEY = "out_port_id";
    private static final String ESC_PORTID_KEY = "esc_port_id";

    protected static final Logger logger = Logger.getLogger(DockerUmbox.class.getName());

    private String fullBaseURL;
    private String containerName;
    private String apiURL;

    public DockerUmbox(UmboxImage image, Device device)
    {
        super(image, device);
        setupDockerVars();
    }

    public DockerUmbox(UmboxImage image, Device device, int instanceId)
    {
        super(image, device, instanceId);
        setupDockerVars();
    }

    private void setupDockerVars()
    {
        fullBaseURL = "http://" + device.getDataNode().getIpAddress() + ":" + API_PORT + API_BASE_URL;
        containerName = PREFIX + this.umboxId;
        apiURL = "/" + image.getName() + "/" + containerName + "/" + device.getIp();
    }

    @Override
    protected boolean start()
    {
        try
        {
            JSONObject response = sendToOvsDockerServer(apiURL, "POST");
            this.setOvsInPortId(response.getString(IN_PORTID_KEY));
            this.setOvsOutPortId(response.getString(OUT_PORTID_KEY));
            this.setOvsRepliesPortId(response.getString(ESC_PORTID_KEY));
            return true;
        }
        catch (Exception e)
        {
            logger.warning("Error creating docker container: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean stop()
    {
        try
        {
            sendToOvsDockerServer(apiURL, "DELETE");
            return true;
        }
        catch (Exception e)
        {
            logger.warning("Error stopping docker container: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject sendToOvsDockerServer(String URL, String method) throws IOException
    {
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(fullBaseURL + URL);
            logger.info("Sending command to: " + url.toString());
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod(method);
            int responseCode = httpCon.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
                }
                in.close();

                logger.info("Response: " + response.toString());
            }
            else
            {
                logger.warning("GET request was unsuccessful: " + responseCode);
                throw new RuntimeException("Problem sending request to server: " + responseCode);
            }
        } catch (Exception e) {
            logger.warning("Error sending HTTP command: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try{
            JSONObject reply = new JSONObject(response.toString());
            String status = reply.getString(STATUS_KEY);
            if(OK_VALUE.equals(status))
            {
                logger.info("Request returned with OK status.");
                return reply;
            }
            else
            {
                throw new RuntimeException("Problem processing request in server (error status): " + reply.getString(ERROR_DETAILS_KEY));
            }
        } catch (Exception e) {
            logger.warning("Error parsing JSON reply: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static void test()
    {
        try
        {
            Device dev = new Device();
            UmboxImage image = new UmboxImage();
            image.setName("hello-world");

            logger.info("Starting container");
            DockerUmbox u = new DockerUmbox(image, dev);
            u.start();

            logger.info("Waiting for a bit.");
            Thread.sleep(5000);

            logger.info("Stopping and removing container.");
            u.stop();

            logger.info("Finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
