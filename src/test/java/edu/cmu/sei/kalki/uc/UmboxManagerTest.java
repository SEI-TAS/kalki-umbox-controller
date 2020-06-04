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