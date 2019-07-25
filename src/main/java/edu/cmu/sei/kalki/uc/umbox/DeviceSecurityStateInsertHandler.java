package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertHandler;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;

/**
 * Handles trigger when a new security state is detected for a device.
 */
public class DeviceSecurityStateInsertHandler implements InsertHandler
{
    @Override
    public void handleNewInsertion(int deviceSecurityStateId)
    {
        System.out.println("Handling new device security state detection with id: <" + deviceSecurityStateId + ">.");
        DeviceSecurityState currentState = Postgres.findDeviceSecurityState(deviceSecurityStateId);
        if(currentState == null)
        {
            System.out.println("Device security state with given id could not be loaded from DB (" + deviceSecurityStateId + ")");
            return;
        }

        int deviceId = currentState.getDeviceId();
        Device device = Postgres.findDevice(deviceId);
        System.out.println("Found device info for device with id " + deviceId);

        DAGManager.setupUmboxesForDevice(device, currentState);
    }
}
