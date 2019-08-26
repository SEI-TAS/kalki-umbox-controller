package edu.cmu.sei.kalki.uc.alerts;

import java.io.BufferedReader;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.models.Alert;
import edu.cmu.sei.ttg.kalki.models.AlertType;
import org.eclipse.jetty.http.HttpStatus;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet that handles a new alert and stores it for the controller to use.
 */
public class AlertHandlerServlet extends HttpServlet
{
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
            int umboxId = alertData.getInt("umbox");
            String alertTypeName = alertData.getString("alert");

            // Store info in DB
            System.out.println("umboxId: " + umboxId);
            System.out.println("alert: " + alertTypeName);

            // Find the alert type in the DB.
            AlertType alertTypeFound = null;
            List<AlertType> types = Postgres.findAllAlertTypes();
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

            Alert currentAlert = new Alert(alertTypeName, umboxId, alertTypeFound.getId());
            Postgres.insertAlert(currentAlert);
        }
        catch (JSONException e)
        {
            throw new ServletException("Error parsing JSON request string: " + e.toString());
        }

        response.setStatus(HttpStatus.OK_200);
    }
}
