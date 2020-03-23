package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.daos.SecurityStateDAO;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.PolicyRuleLog;
import edu.cmu.sei.kalki.db.models.SecurityState;
import edu.cmu.sei.kalki.uc.umbox.DeviceSecurityStateInsertHandler;
import edu.cmu.sei.kalki.uc.umbox.UmboxManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class DeviceSecurityStateInsertHandlerTest extends DatabaseTestBase
{
    @Mock
    private UmboxManager umboxManager;  // Mockito will create a mock for this and initialize it.

    @InjectMocks
    private DeviceSecurityStateInsertHandler handler;    // Mockito will initialize this with default constructor, and inject.

    @BeforeEach
    public void setup() {
        super.setup();
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Helper method to set up test data in the DB.
     */
    private void insertDeviceData(int initState) {
        testDeviceType = insertTestDeviceType();
        testDevice = insertTestDevice(initState, testDeviceType);
    }

    /**
     * Tests that the umbox manager is properly called to set up umboxes.
     */
    @Test
    public void testNotificationHandling() {
        insertDeviceData(1);
        SecurityState currentState = SecurityStateDAO.findSecurityState(testDevice.getCurrentState().getStateId());

        handler.handleNewInsertion(testDevice.getCurrentState().getId());

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
    public void testSameState() {
        // Create a first initial device (initially it has no previous state).
        insertDeviceData(1);
        handler.handleNewInsertion(testDevice.getCurrentState().getId());

        // Change the device state.
        DeviceSecurityState dss = new DeviceSecurityState(testDevice.getId(), 2);
        dss.insert();
        testDevice.setCurrentState(dss);
        testDevice.insertOrUpdate();

        // Call the handler.
        handler.handleNewInsertion(dss.getId());

        // Verify the method to set up umboxes was only called once, not twice.
        Mockito.verify(umboxManager, Mockito.atMost(1)).setupUmboxesForDevice(Mockito.any(Device.class), Mockito.any(SecurityState.class));
    }
}
