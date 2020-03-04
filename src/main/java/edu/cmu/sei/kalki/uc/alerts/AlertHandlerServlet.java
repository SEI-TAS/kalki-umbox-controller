package edu.cmu.sei.kalki.uc.alerts;

import java.io.BufferedReader;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.cmu.sei.kalki.db.daos.AlertTypeDAO;
import edu.cmu.sei.kalki.db.daos.DeviceDAO;
import edu.cmu.sei.kalki.db.daos.UmboxInstanceDAO;
import edu.cmu.sei.kalki.db.models.Alert;
import edu.cmu.sei.kalki.db.models.AlertType;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.DeviceSecurityState;
import edu.cmu.sei.kalki.db.models.StageLog;
import edu.cmu.sei.kalki.db.models.UmboxInstance;
import edu.cmu.sei.kalki.db.models.UmboxLog;
import org.eclipse.jetty.http.HttpStatus;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet that handles a new alert and stores it for the controller to use.
 */
public class AlertHandlerServlet extends HttpServlet
{
    // Special alert used to notify us that the umbox has started.
    private static final String UMBOX_READY_ALERT = "umbox-ready";

    // Special alert used to store logging info.
    private static final String UMBOX_LOG_ALERT = "log-info";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException
    {
        // Parse the JSON data from the alert.
        JSONObject alertData;
        try
        {
            String bodyLine;
            StringBuilder jsonBody = new StringBuilder();
            BufferedReader bodyReader = request.getReader();
            while ((bodyLine = bodyReader.readLine()) != null)
            {
                jsonBody.append(bodyLine);
            }

            alertData = new JSONObject(jsonBody.toString());
        }
        catch (JSONException e)
        {
            throw new ServletException("Error parsing JSON request string");
        }
        catch (Exception e)
        {
            throw new ServletException("Error parsing request: " + e.toString());
        }

        // Store the alert data.
        try
        {
            // Get information about the alert.
            String umboxId = String.valueOf(alertData.getInt("umbox"));
            String alertTypeName = alertData.getString("alert");
            String alertDetails = alertData.getString("details");
            System.out.println("umboxId: " + umboxId);
            System.out.println("alert: " + alertTypeName);
            System.out.println("alertDetails: " + alertDetails);

            // Handle special alert cases which won't be stored in the alert table.
            if(alertTypeName.equals(UMBOX_READY_ALERT))
            {
                // Get information about the device security status change that triggered this.
                UmboxInstance umbox = UmboxInstanceDAO.findUmboxInstance(umboxId);
                if(umbox == null)
                {
                    System.out.println("Error processing alert: umbox instance with id " + umboxId + " was not found in DB.");
                    return;
                }
                Device device = DeviceDAO.findDevice(umbox.getDeviceId());
                DeviceSecurityState state = device.getCurrentState();

                // Store into log that the umbox is ready.
                StageLog stageLogInfo = new StageLog(state.getId(), StageLog.Action.DEPLOY_UMBOX, StageLog.Stage.FINISH, umboxId);
                stageLogInfo.insert();
                return;
            }

            if(alertTypeName.equals(UMBOX_LOG_ALERT))
            {
                // Store into log whatever we want to log.
                UmboxLog umboxLogInfo = new UmboxLog(umboxId, alertDetails);
                umboxLogInfo.insert();
                return;
            }

            // Find the alert type in the DB.
            AlertType alertTypeFound = null;
            List<AlertType> types = AlertTypeDAO.findAllAlertTypes();
            for(AlertType type : types)
            {
                if(type.getName().equals(alertTypeName))
                {
                    alertTypeFound = type;
                    break;
                }
            }

            if(alertTypeFound == null)
            {
                throw new ServletException("Alert type received <" + alertTypeName + "> not found in DB.");
            }

            synchronized (this)
            {
                Alert currentAlert = new Alert(alertTypeName, umboxId, alertTypeFound.getId(), alertDetails);
                currentAlert.insert();
            }
        }
        catch (JSONException e)
        {
            throw new ServletException("Error parsing JSON request string: " + e.toString());
        }

        response.setStatus(HttpStatus.OK_200);
    }
}
