package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

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
        apiURL = "/" + image.getName() + "/" + containerName;
    }

    @Override
    protected boolean start()
    {
        try
        {
            sendToOvsDockerServer(apiURL, "POST");
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

    private void sendToOvsDockerServer(String URL, String method) throws IOException
    {
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
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine);
                }
                in.close();

                System.out.println(response.toString());
            }
            else
            {
                System.out.println("GET request was unsuccessful: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error sending HTTP command: " + e.getMessage());
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
