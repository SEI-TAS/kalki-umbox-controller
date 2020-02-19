package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertHandler;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.SecurityState;
import edu.cmu.sei.ttg.kalki.models.StageLog;

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
        DeviceSecurityState currentDeviceSecurityState = Postgres.findDeviceSecurityState(deviceSecurityStateId);
        if(currentDeviceSecurityState == null)
        {
            System.out.println("Device security state with given id could not be loaded from DB (" + deviceSecurityStateId + ")");
            return;
        }

        int deviceId = currentDeviceSecurityState.getDeviceId();
        Device device = Postgres.findDevice(deviceId);
        System.out.println("Found device info for device with id " + deviceId);

        // Check if state is the same as before, and if so, ignore trigger.
        SecurityState currentSecurityState = Postgres.findSecurityState(currentDeviceSecurityState.getStateId());
        String lastSecurityStateName = lastSecurityState.get(deviceId);
        if(currentSecurityState.getName().equals(lastSecurityStateName))
        {
            System.out.println("Ignoring trigger since device is now in same state as it was in the previous call.");
        }
        else
        {
            // Store into log that we are starting umbox setup
            StageLog stageLogInfo = new StageLog(currentDeviceSecurityState.getId(), StageLog.Action.DEPLOY_UMBOX, StageLog.Stage.REACT, "");
            Postgres.insertStageLog(stageLogInfo);

            lastSecurityState.put(deviceId, currentSecurityState.getName());
            DAGManager.setupUmboxesForDevice(device, currentSecurityState);
        }
    }
}
