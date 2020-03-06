package edu.cmu.sei.kalki.uc;

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

public class PolicyInstanceInsertHandlerTest extends TestBase
{
    private AlertType testAlertType;
    private DeviceType testDeviceType;
    private PolicyRule testPolicyRule;
    private Device testDevice;

    @Mock
    private UmboxManager umboxManager;

    @InjectMocks
    private PolicyInstanceInsertHandler handler = new PolicyInstanceInsertHandler();

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

        handler.handleNewInsertion(policyRuleLog.getId());

        // Verify the method to set up umboxes was properly called.
        Mockito.verify(umboxManager).setupUmboxesForDevice(Mockito.any(Device.class), Mockito.any(SecurityState.class));
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
