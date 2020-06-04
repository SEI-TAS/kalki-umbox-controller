package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.database.Postgres;
import edu.cmu.sei.kalki.db.models.Alert;
import edu.cmu.sei.kalki.db.models.AlertType;
import edu.cmu.sei.kalki.db.models.DataNode;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.DeviceType;
import edu.cmu.sei.kalki.db.models.PolicyCondition;
import edu.cmu.sei.kalki.db.models.PolicyRule;
import edu.cmu.sei.kalki.db.models.StateTransition;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.utils.TestDB;

import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class DatabaseTestBase
{
    protected AlertType testAlertType;
    protected DeviceType testDeviceType;
    protected PolicyRule testPolicyRule;
    protected Device testDevice;
    protected UmboxImage testImage;

    public void setup() {
        try {
            System.out.println("Base before each");
            Postgres.setLoggingLevel(Level.SEVERE);
            TestDB.recreateTestDB();
            TestDB.initialize();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void reset() {
        try {
            TestDB.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts a test alert type.
     */
    protected AlertType insertAlertType(String alertTypeName) {
        AlertType alertType = new AlertType(alertTypeName, "test alert", "test");
        alertType.insert();
        return alertType;
    }

    /**
     * Inserts a device type, as well as a policy rule for the given alert type.
     */
    protected DeviceType insertTestDeviceType() {
        DeviceType deviceType = new DeviceType(1, "Test Type");
        deviceType.insert();
        return deviceType;
    }

    protected PolicyRule insertPolicyRule(AlertType alertType, DeviceType deviceType, int initState, int endState) {
        List<Integer> alertTypeIds = new ArrayList<>();
        alertTypeIds.add(alertType.getId());
        PolicyCondition policyCondition = new PolicyCondition(10, alertTypeIds);
        policyCondition.insert();

        // Rule 1
        StateTransition stateTransition = new StateTransition(initState, endState);
        stateTransition.insert();

        PolicyRule policyRule = new PolicyRule(stateTransition.getId(), policyCondition.getId(), deviceType.getId(), 10);
        policyRule.insert();

        return policyRule;
    }

    /**
     * Inserts a test device in the given state and of the given type.
     */
    protected Device insertTestDevice(int stateId, DeviceType deviceType) {
        DataNode dataNode = new DataNode("TestDN", "localhost");
        dataNode.insert();
        Device device = new Device("Test Device", "device", deviceType, "127.0.0.1", 1, 1, dataNode);
        device.insert();

        DeviceSecurityState dss = new DeviceSecurityState(device.getId(), stateId);
        dss.insert();
        device.setCurrentState(dss);
        device.insertOrUpdate();

        return device;
    }

    /**
     * Inserts a specific alert to the DB
     */
    protected void insertAlert(Device device, AlertType alertType) {
        System.out.println("Inserting test alert of type: " + alertType.getName());
        Alert alert = new Alert(device.getId(), alertType.getName(), alertType.getId(), "this is a test");
        alert.insert();
    }

    /**
     * Inserts an umbox image with the given name and no file.
     */
    protected UmboxImage insertTestUmboxImage(String imageName) {
        UmboxImage umboxImage = new UmboxImage(imageName, "");
        umboxImage.insert();
        return umboxImage;
    }

    /**
     * Waits for the given amount of ms.
     */
    protected void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}