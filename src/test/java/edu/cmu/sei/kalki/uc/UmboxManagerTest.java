package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.daos.SecurityStateDAO;
import edu.cmu.sei.kalki.db.models.SecurityState;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.models.UmboxLookup;
import edu.cmu.sei.kalki.uc.ovs.OpenFlowRule;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSDB;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSSwitch;
import edu.cmu.sei.kalki.uc.umbox.Umbox;
import edu.cmu.sei.kalki.uc.umbox.UmboxManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class UmboxManagerTest extends DatabaseTestBase
{
    private UmboxImage testImage;
    private UmboxLookup testUmboxLookup;

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

    private UmboxImage insertTestUmboxImage() {
        UmboxImage umboxImage = new UmboxImage("kalki/testimage", "");
        umboxImage.insert();
        return umboxImage;
    }

    private UmboxLookup insertUmboxLookup(int policyRuleId, int umboxImageId) {
        UmboxLookup umboxLookup = new UmboxLookup(policyRuleId, umboxImageId, 0);
        umboxLookup.insert();
        return umboxLookup;
    }

    private void insertTestData(int initState) {
        testDeviceType = insertTestDeviceType();
        testDevice = insertTestDevice(initState, testDeviceType);
        testImage = insertTestUmboxImage();
    }

    private void insertUmboxReactions(int initState, int endState) {
        testAlertType = insertAlertType("test-alert");
        testPolicyRule = insertPolicyRule(testAlertType, testDeviceType, initState, endState);
        testUmboxLookup = insertUmboxLookup(testPolicyRule.getId(), testImage.getId());
    }

    @Test
    public void testNoUmboxes() {
        SecurityState currentState = SecurityStateDAO.findByName("Normal");
        insertTestData(currentState.getId());

        umboxManager.setupUmboxesForDevice(testDevice, currentState);

        assert(MockUmbox.numStartTimesCalled == 0);
        Mockito.verify(ovsSwitch, Mockito.never()).addRule(Mockito.any(OpenFlowRule.class));
    }

    @Test
    public void testOneUmbox() {
        SecurityState currentState = SecurityStateDAO.findByName("Normal");
        insertTestData(currentState.getId());
        insertUmboxReactions(currentState.getId(), 2);

        umboxManager.setupUmboxesForDevice(testDevice, currentState);

        assert(MockUmbox.numStartTimesCalled == 1);
        Mockito.verify(ovsSwitch, Mockito.times(6)).addRule(Mockito.any(OpenFlowRule.class));
    }

}
