package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.daos.AlertDAO;
import edu.cmu.sei.kalki.db.daos.StageLogDAO;
import edu.cmu.sei.kalki.db.daos.UmboxLogDAO;
import edu.cmu.sei.kalki.db.models.Alert;
import edu.cmu.sei.kalki.db.models.StageLog;
import edu.cmu.sei.kalki.db.models.UmboxInstance;
import edu.cmu.sei.kalki.db.models.UmboxLog;
import edu.cmu.sei.kalki.uc.alerts.AlertHandlerServlet;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AlertHandlerServletTest extends DatabaseTestBase
{
    private AlertHandlerServlet servlet;

    @BeforeEach
    public void setup() {
        super.setup();
        servlet = new AlertHandlerServlet();
    }

    private void insertTestData(int initState, String alerterId) {
        testAlertType = insertAlertType("test-alert");
        testDeviceType = insertTestDeviceType();
        testDevice = insertTestDevice(initState, testDeviceType);
        testImage = insertTestUmboxImage("kalki/testimage");

        UmboxInstance umboxInstance = new UmboxInstance(alerterId, testImage.getId(), testDevice.getId());
        umboxInstance.insert();

    }

    private JSONObject createAlert(String umboxId, String alertTypeName) {
        JSONObject alert = new JSONObject();
        alert.put("umbox", umboxId);
        alert.put("alert", alertTypeName);
        alert.put("details", "A lot of details!");
        return alert;
    }

    @Test
    public void testUmboxReadyAlert() {
        String alerterId = "12314";
        insertTestData(1, alerterId);

        servlet.processAlert(createAlert(alerterId, "umbox-ready"));

        List<StageLog> stageLogList = StageLogDAO.findAllStageLogs();
        Assertions.assertEquals(1, stageLogList.size());
        Assertions.assertEquals(StageLog.Action.DEPLOY_UMBOX.convert(), stageLogList.get(0).getAction());
        Assertions.assertEquals(StageLog.Stage.FINISH.convert(), stageLogList.get(0).getStage());
    }

    @Test
    public void testUmboxReadyAlertNotFound() {
        servlet.processAlert(createAlert("012345", "umbox-ready"));
        Assertions.assertEquals(0, StageLogDAO.findAllStageLogs().size());
    }

    @Test
    public void testUmboxLogAlert() {
        String alerterId = "12314";
        insertTestData(1, alerterId);
        
        servlet.processAlert(createAlert(alerterId, "log-info"));

        List<UmboxLog> umboxLogList = UmboxLogDAO.findAllUmboxLogsForAlerterId(alerterId);
        Assertions.assertEquals(1, umboxLogList.size());
        Assertions.assertEquals("A lot of details!", umboxLogList.get(0).getDetails());
    }

    @Test
    public void testUmboxAlert() {
        String alerterId = "12314";
        insertTestData(1, alerterId);

        servlet.processAlert(createAlert(alerterId, testAlertType.getName()));

        List<Alert> insertedAlerts = AlertDAO.findAlertsByDevice(testDevice.getId());
        Assertions.assertEquals(1, insertedAlerts.size());
        Assertions.assertEquals(alerterId, insertedAlerts.get(0).getAlerterId());
        Assertions.assertEquals(testAlertType.getName(), insertedAlerts.get(0).getName());
    }

    @Test
    public void testUmboxAlertInvalidAlertTypeName() {
        String alerterId = "12314";
        insertTestData(1, alerterId);

        Assertions.assertThrows(RuntimeException.class, () -> {
            servlet.processAlert(createAlert(alerterId, "invalid-alert-type"));
        });
    }
}
