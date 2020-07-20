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

import edu.cmu.sei.kalki.db.daos.SecurityStateDAO;
import edu.cmu.sei.kalki.db.daos.UmboxInstanceDAO;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.SecurityState;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.models.UmboxLookup;
import edu.cmu.sei.kalki.uc.ovs.OpenFlowRule;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSDB;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSSwitch;
import edu.cmu.sei.kalki.uc.umbox.Umbox;
import edu.cmu.sei.kalki.uc.umbox.UmboxManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class UmboxManagerTest extends DatabaseTestBase
{
    @Mock
    RemoteOVSSwitch ovsSwitch;

    @Mock
    RemoteOVSDB ovsDB;

    @InjectMocks
    UmboxManager umboxManager;

    @BeforeEach
    public void setup() {
        super.setup();
        Umbox.setUmboxClass(MockUmbox.class);
        MockUmbox.reset();
        MockitoAnnotations.initMocks(this);
    }

    private UmboxLookup insertUmboxLookup(int securityStateId, int deviceTypeId, int umboxImageId, int dagOrder) {
        UmboxLookup umboxLookup = new UmboxLookup(securityStateId, deviceTypeId, umboxImageId, dagOrder);
        umboxLookup.insert();
        return umboxLookup;
    }

    private void insertTestData(int initState) {
        testDeviceType = insertTestDeviceType();
        testDevice = insertTestDevice(initState, testDeviceType);
        testImage = insertTestUmboxImage("kalki/testimage");
    }

    private void insertPolicy(int initState, int endState) {
        testAlertType = insertAlertType("test-alert");
        testPolicyRule = insertPolicyRule(testAlertType, testDeviceType, initState, endState);
    }

    @Test
    public void testNoUmboxes() {
        SecurityState currentState = SecurityStateDAO.findByName("Normal");
        insertTestData(currentState.getId());

        umboxManager.setupUmboxesForDevice(testDevice, currentState);

        Assertions.assertEquals(0, MockUmbox.numStartTimesCalled );
        Mockito.verify(ovsSwitch, Mockito.never()).addRule(Mockito.any(OpenFlowRule.class));
        Mockito.verify(ovsSwitch, Mockito.never()).removeRule(Mockito.any(OpenFlowRule.class));
    }

    @Test
    public void testOneUmbox() {
        SecurityState currentState = SecurityStateDAO.findByName("Normal");
        insertTestData(currentState.getId());
        insertUmboxLookup(currentState.getId(), testDeviceType.getId(), testImage.getId(), 0);

        umboxManager.setupUmboxesForDevice(testDevice, currentState);

        Assertions.assertEquals(1, MockUmbox.numStartTimesCalled);
        Assertions.assertEquals(1, UmboxInstanceDAO.findUmboxInstances(testDevice.getId()).size());
        Mockito.verify(ovsSwitch, Mockito.times(6)).addRule(Mockito.any(OpenFlowRule.class));
        Mockito.verify(ovsSwitch, Mockito.times(2)).removeRule(Mockito.any(OpenFlowRule.class));
    }

    @Test
    public void testTwoUmboxes() {
        SecurityState currentState = SecurityStateDAO.findByName("Normal");
        insertTestData(currentState.getId());
        insertUmboxLookup(currentState.getId(), testDeviceType.getId(), testImage.getId(), 0);

        UmboxImage umboxImage2 = insertTestUmboxImage("kalki/testimage2");
        insertUmboxLookup(currentState.getId(), testDeviceType.getId(), umboxImage2.getId(), 1);

        umboxManager.setupUmboxesForDevice(testDevice, currentState);

        // Two umboxes should have been created; 9 rules should have been added (6 + 3 times (num_umboxes -1)); 2 rules removed per device.
        Assertions.assertEquals(2, MockUmbox.numStartTimesCalled);
        Assertions.assertEquals(2, UmboxInstanceDAO.findUmboxInstances(testDevice.getId()).size());
        Mockito.verify(ovsSwitch, Mockito.times(9)).addRule(Mockito.any(OpenFlowRule.class));
        Mockito.verify(ovsSwitch, Mockito.times(2)).removeRule(Mockito.any(OpenFlowRule.class));
    }

    /**
     * Tests that the bootstrap function sets umboxes for configured devices.
     */
    @Test
    public void testBootstrap() {
        SecurityState currentState = SecurityStateDAO.findByName("Normal");
        insertTestData(currentState.getId());
        insertUmboxLookup(currentState.getId(), testDeviceType.getId(), testImage.getId(), 0);

        umboxManager.bootstrap();

        Assertions.assertEquals(1, MockUmbox.numStartTimesCalled);
        Assertions.assertEquals(1, UmboxInstanceDAO.findUmboxInstances(testDevice.getId()).size());
        Mockito.verify(ovsSwitch, Mockito.times(6)).addRule(Mockito.any(OpenFlowRule.class));
        Mockito.verify(ovsSwitch, Mockito.times(2)).removeRule(Mockito.any(OpenFlowRule.class));
    }

    /**
     * Tests that one set of umboxes was deployed, and then a new set was deployed after stopping the first one.
     */
    @Test
    public void testUmboxChanged() {
        SecurityState initialState = SecurityStateDAO.findByName("Normal");
        insertTestData(initialState.getId());
        insertUmboxLookup(initialState.getId(), testDeviceType.getId(), testImage.getId(), 0);

        // Add a second image for the next transition.
        SecurityState secondState = SecurityStateDAO.findByName("Suspicious");
        testImage = insertTestUmboxImage("kalki/testimage2");
        insertPolicy(initialState.getId(), secondState.getId());
        insertUmboxLookup(secondState.getId(), testDeviceType.getId(), testImage.getId(), 0);

        // Set up initial umboxes.
        umboxManager.setupUmboxesForDevice(testDevice, initialState);

        // Move the device to the next state (would be done by the main controller)
        DeviceSecurityState dss = new DeviceSecurityState(testDevice.getId(), secondState.getId());
        testDevice.setCurrentState(dss);
        testDevice.insertOrUpdate();

        // Set new umboxes.
        umboxManager.setupUmboxesForDevice(testDevice, secondState);

        Assertions.assertEquals(2, MockUmbox.numStartTimesCalled);
        Assertions.assertEquals(1, MockUmbox.numStopTimesCalled);
        Assertions.assertEquals(1, UmboxInstanceDAO.findUmboxInstances(testDevice.getId()).size());
        Mockito.verify(ovsSwitch, Mockito.times(12)).addRule(Mockito.any(OpenFlowRule.class));
        Mockito.verify(ovsSwitch, Mockito.times(4)).removeRule(Mockito.any(OpenFlowRule.class));
    }
}
