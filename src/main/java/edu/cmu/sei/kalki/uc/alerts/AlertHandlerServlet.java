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
package edu.cmu.sei.kalki.uc.alerts;

import java.io.BufferedReader;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.cmu.sei.kalki.db.daos.AlertTypeDAO;
import edu.cmu.sei.kalki.db.daos.DeviceDAO;
import edu.cmu.sei.kalki.db.daos.UmboxInstanceDAO;
import edu.cmu.sei.kalki.db.models.Alert;
import edu.cmu.sei.kalki.db.models.AlertType;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.StageLog;
import edu.cmu.sei.kalki.db.models.UmboxInstance;
import edu.cmu.sei.kalki.db.models.UmboxLog;
import org.eclipse.jetty.http.HttpStatus;

import org.json.JSONObject;

/**
 * Servlet that handles a new alert and stores it for the controller to use.
 */
public class AlertHandlerServlet extends HttpServlet
{
    // Special alert used to notify us that the umbox has started.
    private static final String UMBOX_READY_ALERT = "umbox-ready";

    // Special alert used to store logging info.
    private static final String UMBOX_LOG_ALERT = "log-info";

    protected static final Logger logger = Logger.getLogger(AlertHandlerServlet.class.getName());

    /**
     * Process a POST request.
     * @param request
     * @param response
     * @throws ServletException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException
    {
        // Parse the JSON data from the alert.
        JSONObject alertData;
        try
        {
            String bodyLine;
            StringBuilder jsonBody = new StringBuilder();
            BufferedReader bodyReader = request.getReader();
            while ((bodyLine = bodyReader.readLine()) != null)
            {
                jsonBody.append(bodyLine);
            }

            alertData = new JSONObject(jsonBody.toString());
        }
        catch (Exception e)
        {
            throw new ServletException("Error parsing request: " + e.toString());
        }

        // Process the alert.
        try
        {
            processAlert(alertData);
        }
        catch (Exception e)
        {
            throw new ServletException("Error processing alert: " + e.toString());
        }

        response.setStatus(HttpStatus.OK_200);
    }

    /**
     * Process a given alert.
     * @param alertData the alert JSON data.
     */
    public void processAlert(JSONObject alertData) {
        // Get information about the alert.
        String umboxId = String.valueOf(alertData.getInt("umbox"));
        String alertTypeName = alertData.getString("alert");
        String alertDetails = alertData.getString("details");
        logger.info("umboxId: " + umboxId);
        logger.info("alert: " + alertTypeName);
        logger.info("alertDetails: " + alertDetails);

        // Handle special alert cases which won't be stored in the alert table.
        if(alertTypeName.equals(UMBOX_READY_ALERT))
        {
            // Get information about the device security status change that triggered this.
            UmboxInstance umbox = UmboxInstanceDAO.findUmboxInstance(umboxId);
            if(umbox == null)
            {
                logger.warning("Error processing alert: umbox instance with id " + umboxId + " was not found in DB.");
                return;
            }
            Device device = DeviceDAO.findDevice(umbox.getDeviceId());
            DeviceSecurityState state = device.getCurrentState();

            // Store into log that the umbox is ready.
            StageLog stageLogInfo = new StageLog(state.getId(), StageLog.Action.DEPLOY_UMBOX, StageLog.Stage.FINISH, umboxId);
            stageLogInfo.insert();
            return;
        }

        if(alertTypeName.equals(UMBOX_LOG_ALERT))
        {
            // Store into log whatever we want to log.
            UmboxLog umboxLogInfo = new UmboxLog(umboxId, alertDetails);
            umboxLogInfo.insert();
            return;
        }

        // Find the alert type in the DB.
        AlertType alertTypeFound = AlertTypeDAO.findAlertTypeByName(alertTypeName);
        if(alertTypeFound == null)
        {
            throw new RuntimeException("Alert type received <" + alertTypeName + "> not found in DB.");
        }

        synchronized (this)
        {
            Alert currentAlert = new Alert(alertTypeName, umboxId, alertTypeFound.getId(), alertDetails);
            currentAlert.insert();
        }
    }
}
