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
import edu.cmu.sei.kalki.db.models.AlertType;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceType;
import edu.cmu.sei.kalki.db.models.PolicyRule;
import edu.cmu.sei.kalki.db.models.PolicyRuleLog;
import edu.cmu.sei.kalki.db.models.SecurityState;
import edu.cmu.sei.kalki.uc.umbox.PolicyInstanceInsertHandler;
import edu.cmu.sei.kalki.uc.umbox.UmboxManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PolicyInstanceInsertHandlerTest extends DatabaseTestBase
{
    @Mock
    private UmboxManager umboxManager;  // Mockito will create a mock for this and initialize it.

    @InjectMocks
    private PolicyInstanceInsertHandler handler;    // Mockito will initialize this with default constructor, and inject.

    @BeforeEach
    public void setup() {
        super.setup();
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Helper method to set up test data in the DB.
     */
    private PolicyRuleLog insertPolicyRuleLogAndData(int initState, int endState) {
        testAlertType = insertAlertType("test-alert");
        testDeviceType = insertTestDeviceType();
        testPolicyRule = insertPolicyRule(testAlertType, testDeviceType, initState, endState);
        testDevice = insertTestDevice(initState, testDeviceType);

        PolicyRuleLog policyRuleLog = new PolicyRuleLog(testPolicyRule.getId(), testDevice.getId());
        policyRuleLog.insert();
        return policyRuleLog;
    }

    /**
     * Tests that the umbox manager is properly called to set up umboxes.
     */
    @Test
    public void testNotificationHandling() {
        PolicyRuleLog policyRuleLog = insertPolicyRuleLogAndData(1, 2);
        SecurityState currentState = SecurityStateDAO.findSecurityState(testDevice.getCurrentState().getStateId());

        handler.handleNewInsertion(policyRuleLog.getId());

        // Verify the method to set up umboxes was properly called.
        Mockito.verify(umboxManager).setupUmboxesForDevice(Mockito.argThat((Device device) -> device.getId() == testDevice.getId()),
                                                           Mockito.argThat((SecurityState state) -> state.getId() == currentState.getId()));
    }

    /**
     * Tests that non found ids are properly handled.
     */
    @Test
    public void testNoPolicyRuleLogFound() {
        int fakeId = 1;
        handler.handleNewInsertion(fakeId);

        // Verify the method to set up umboxes was NOT called.
        Mockito.verify(umboxManager, Mockito.never()).setupUmboxesForDevice(Mockito.any(Device.class), Mockito.any(SecurityState.class));
    }

    /**
     * Tests that same-state cases are properly handled.
     */
    @Test
    public void testSameStatePolicyRuleLog() {
        // Create a first policy rule log to initialize object (initially it has no previous state).
        PolicyRuleLog policyRuleLog = insertPolicyRuleLogAndData(1, 2);
        handler.handleNewInsertion(policyRuleLog.getId());

        // Create a second rule that stays in the same state, and insert a log of this type.
        testPolicyRule = insertPolicyRule(testAlertType, testDeviceType, 2, 2);
        policyRuleLog = new PolicyRuleLog(testPolicyRule.getId(), testDevice.getId());
        policyRuleLog.insert();

        // Call the handler.
        handler.handleNewInsertion(policyRuleLog.getId());

        // Verify the method to set up umboxes was only called once, not twice.
        Mockito.verify(umboxManager, Mockito.atMost(1)).setupUmboxesForDevice(Mockito.any(Device.class), Mockito.any(SecurityState.class));
    }
}
