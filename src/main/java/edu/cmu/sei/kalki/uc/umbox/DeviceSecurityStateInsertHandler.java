package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertHandler;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;

import java.util.HashMap;

/**
 * Handles trigger when a new security state is detected for a device.
 */
public class DeviceSecurityStateInsertHandler implements InsertHandler
{
    private static HashMap<Integer, String> lastSecurityState = new HashMap<>();

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

        // Check if state is the same as before, and if so, ignore trigger.
        String lastSecurityStateName = lastSecurityState.get(deviceId);
        if(currentState.getName().equals(lastSecurityStateName))
        {
            System.out.println("Ignoring trigger since device is now in same state as it was in the previous call.");
        }
        else
        {
            lastSecurityState.put(deviceId, currentState.getName());
            DAGManager.setupUmboxesForDevice(device, currentState);
        }
    }
}
