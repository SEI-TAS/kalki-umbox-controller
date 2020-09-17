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
package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.database.Postgres;
import edu.cmu.sei.kalki.db.models.Alert;
import edu.cmu.sei.kalki.db.models.AlertType;
import edu.cmu.sei.kalki.db.models.DataNode;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.DeviceType;
import edu.cmu.sei.kalki.db.models.PolicyCondition;
import edu.cmu.sei.kalki.db.models.PolicyRule;
import edu.cmu.sei.kalki.db.models.StateTransition;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.utils.TestDB;

import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class DatabaseTestBase
{
    protected AlertType testAlertType;
    protected DeviceType testDeviceType;
    protected PolicyRule testPolicyRule;
    protected Device testDevice;
    protected UmboxImage testImage;

    public void setup() {
        try {
            System.out.println("Base before each");
            Postgres.setLoggingLevel(Level.SEVERE);
            TestDB.recreateTestDB();
            TestDB.initialize();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void reset() {
        try {
            TestDB.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts a test alert type.
     */
    protected AlertType insertAlertType(String alertTypeName) {
        AlertType alertType = new AlertType(alertTypeName, "test alert", "test");
        alertType.insert();
        return alertType;
    }

    /**
     * Inserts a device type, as well as a policy rule for the given alert type.
     */
    protected DeviceType insertTestDeviceType() {
        DeviceType deviceType = new DeviceType(1, "Test Type");
        deviceType.insert();
        return deviceType;
    }

    protected PolicyRule insertPolicyRule(AlertType alertType, DeviceType deviceType, int initState, int endState) {
        List<Integer> alertTypeIds = new ArrayList<>();
        alertTypeIds.add(alertType.getId());
        PolicyCondition policyCondition = new PolicyCondition(10, alertTypeIds);
        policyCondition.insert();

        // Rule 1
        StateTransition stateTransition = new StateTransition(initState, endState);
        stateTransition.insert();

        PolicyRule policyRule = new PolicyRule(stateTransition.getId(), policyCondition.getId(), deviceType.getId(), 10);
        policyRule.insert();

        return policyRule;
    }

    /**
     * Inserts a test device in the given state and of the given type.
     */
    protected Device insertTestDevice(int stateId, DeviceType deviceType) {
        DataNode dataNode = new DataNode("TestDN", "localhost");
        dataNode.insert();
        Device device = new Device("Test Device", "device", deviceType, "127.0.0.1", 1, 1, dataNode, "");
        device.insert();

        DeviceSecurityState dss = new DeviceSecurityState(device.getId(), stateId);
        dss.insert();
        device.setCurrentState(dss);
        device.insertOrUpdate();

        return device;
    }

    /**
     * Inserts a specific alert to the DB
     */
    protected void insertAlert(Device device, AlertType alertType) {
        System.out.println("Inserting test alert of type: " + alertType.getName());
        Alert alert = new Alert(device.getId(), alertType.getName(), alertType.getId(), "this is a test");
        alert.insert();
    }

    /**
     * Inserts an umbox image with the given name and no file.
     */
    protected UmboxImage insertTestUmboxImage(String imageName) {
        UmboxImage umboxImage = new UmboxImage(imageName, "");
        umboxImage.insert();
        return umboxImage;
    }

    /**
     * Waits for the given amount of ms.
     */
    protected void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
