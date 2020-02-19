package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertHandler;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.PolicyRule;
import edu.cmu.sei.ttg.kalki.models.PolicyRuleLog;
import edu.cmu.sei.ttg.kalki.models.SecurityState;
import edu.cmu.sei.ttg.kalki.models.StageLog;

import java.util.HashMap;

public class PolicyInstanceInsertHandler implements InsertHandler
{
    private static HashMap<Integer, String> lastSecurityStateMap = new HashMap<>();

    @Override
    public void handleNewInsertion(int policyRuleLogId)
    {
        System.out.println("Handling new device policy rule log detection with id: <" + policyRuleLogId + ">.");
        PolicyRuleLog newPolicyRuleLog = Postgres.findPolicyRuleLog(policyRuleLogId);
        if(newPolicyRuleLog == null)
        {
            System.out.println("Policy rule log with given id could not be loaded from DB (" + policyRuleLogId + ")");
            return;
        }

        int deviceId = newPolicyRuleLog.getDeviceId();
        Device device = Postgres.findDevice(deviceId);
        System.out.println("Found device info for device with id " + deviceId);

        PolicyRule rule = Postgres.findPolicyRule(newPolicyRuleLog.getPolicyRuleId());
        DeviceSecurityState currentDevSecState = device.getCurrentState();
        SecurityState currentState = Postgres.findSecurityState(rule.getStateTransId());

        // Check if state is the same as before, and if so, ignore trigger.
        String lastSecurityStateName = lastSecurityStateMap.get(deviceId);
        if(currentState.getName().equals(lastSecurityStateName))
        {
            System.out.println("Ignoring trigger since device is now in same state as it was in the previous call.");
        }
        else
        {
            // Store into log that we are starting umbox setup
            StageLog stageLogInfo = new StageLog(currentDevSecState.getId(), StageLog.Action.DEPLOY_UMBOX, StageLog.Stage.REACT, "");
            Postgres.insertStageLog(stageLogInfo);

            lastSecurityStateMap.put(deviceId, currentState.getName());
            DAGManager.setupUmboxesForDevice(device, currentState);
        }
    }
}
