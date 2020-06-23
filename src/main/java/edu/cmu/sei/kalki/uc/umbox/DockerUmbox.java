package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class DockerUmbox extends Umbox
{
    private static final String PREFIX = "umbox-";
    private static final String API_BASE_URL = "/ovs-docker";
    private static final String API_PORT = "5500";

    private static final String STATUS_KEY = "status";
    private static final String OK_VALUE = "ok";
    private static final String ERROR_VALUE = "error";
    private static final String ERROR_DETAILS_KEY = "error";
    private static final String IN_PORTID_KEY = "in_port_id";
    private static final String OUT_PORTID_KEY = "out_port_id";
    private static final String ESC_PORTID_KEY = "esc_port_id";

    protected static final Logger logger = Logger.getLogger(DockerUmbox.class.getName());

    private String fullBaseURL;
    private String containerName;
    private String apiURL;

    public DockerUmbox(UmboxImage image, Device device)
    {
        super(image, device);
        setupDockerVars();
    }

    public DockerUmbox(UmboxImage image, Device device, int instanceId)
    {
        super(image, device, instanceId);
        setupDockerVars();
    }

    private void setupDockerVars()
    {
        fullBaseURL = "http://" + device.getDataNode().getIpAddress() + ":" + API_PORT + API_BASE_URL;
        containerName = PREFIX + this.umboxId;
        apiURL = "/" + image.getName() + "/" + containerName + "/" + device.getIp();
    }

    @Override
    protected boolean start()
    {
        try
        {
            JSONObject response = sendToOvsDockerServer(apiURL, "POST");
            this.setOvsInPortId(response.getString(IN_PORTID_KEY));
            this.setOvsOutPortId(response.getString(OUT_PORTID_KEY));
            this.setOvsRepliesPortId(response.getString(ESC_PORTID_KEY));
            return true;
        }
        catch (Exception e)
        {
            logger.warning("Error creating docker container: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean stop()
    {
        try
        {
            sendToOvsDockerServer(apiURL, "DELETE");
            return true;
        }
        catch (Exception e)
        {
            logger.warning("Error stopping docker container: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject sendToOvsDockerServer(String URL, String method) throws IOException
    {
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(fullBaseURL + URL);
            logger.info("Sending command to: " + url.toString());
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod(method);
            int responseCode = httpCon.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
                }
                in.close();

                logger.info("Response: " + response.toString());
            }
            else
            {
                logger.warning("GET request was unsuccessful: " + responseCode);
                throw new RuntimeException("Problem sending request to server: " + responseCode);
            }
        } catch (Exception e) {
            logger.warning("Error sending HTTP command: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try{
            JSONObject reply = new JSONObject(response.toString());
            String status = reply.getString(STATUS_KEY);
            if(OK_VALUE.equals(status))
            {
                logger.info("Request returned with OK status.");
                return reply;
            }
            else
            {
                throw new RuntimeException("Problem processing request in server (error status): " + reply.getString(ERROR_DETAILS_KEY));
            }
        } catch (Exception e) {
            logger.warning("Error parsing JSON reply: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static void test()
    {
        try
        {
            Device dev = new Device();
            UmboxImage image = new UmboxImage();
            image.setName("hello-world");

            logger.info("Starting container");
            DockerUmbox u = new DockerUmbox(image, dev);
            u.start();

            logger.info("Waiting for a bit.");
            Thread.sleep(5000);

            logger.info("Stopping and removing container.");
            u.stop();

            logger.info("Finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
