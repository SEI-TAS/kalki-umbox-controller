package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


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

    private String fullBaseURL;
    private String containerName;
    private String apiURL;

    public DockerUmbox(UmboxImage image, Device device)
    {
        super(image, device);
        setupDockerVars();
    }

    public DockerUmbox(UmboxImage image, int instanceId)
    {
        super(image, instanceId);
        setupDockerVars();
    }

    private void setupDockerVars()
    {
        fullBaseURL = "http://" + Config.data.get("data_node_ip") + ":" + API_PORT + API_BASE_URL;
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
            System.out.println("Error creating docker container: " + e.getMessage());
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
            System.out.println("Error stopping docker container: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject sendToOvsDockerServer(String URL, String method) throws IOException
    {
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(fullBaseURL + URL);
            System.out.println("Sending command to: " + url.toString());
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

                System.out.println("Response: " + response.toString());
            }
            else
            {
                System.out.println("GET request was unsuccessful: " + responseCode);
                throw new RuntimeException("Problem sending request to server: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error sending HTTP command: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try{
            JSONObject reply = new JSONObject(response.toString());
            String status = reply.getString(STATUS_KEY);
            if(OK_VALUE.equals(status))
            {
                System.out.println("Request returned with OK status.");
                return reply;
            }
            else
            {
                throw new RuntimeException("Problem processing request in server (error status): " + reply.getString(ERROR_DETAILS_KEY));
            }
        } catch (Exception e) {
            System.out.println("Error parsing JSON reply: " + e.getMessage());
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

            System.out.println("Starting container");
            DockerUmbox u = new DockerUmbox(image, dev);
            u.start();

            System.out.println("Waiting for a bit.");
            Thread.sleep(5000);

            System.out.println("Stopping and removing container.");
            u.stop();

            System.out.println("Finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
