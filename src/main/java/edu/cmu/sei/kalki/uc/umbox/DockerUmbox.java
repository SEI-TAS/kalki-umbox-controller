package edu.cmu.sei.kalki.uc.umbox;

import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.Docker;
import com.amihaiemil.docker.LocalDocker;
import com.amihaiemil.docker.RemoteDocker;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

public class DockerUmbox extends Umbox
{
    private static final String PREFIX = "umbox-";

    private static Docker docker;
    private String umboxContainerName;

    public DockerUmbox(UmboxImage image, Device device)
    {
        super(image, device);
        connectToDocker();
    }

    public DockerUmbox(UmboxImage image, int instanceId)
    {
        super(image, instanceId);
        connectToDocker();
    }

    private void connectToDocker()
    {
        umboxContainerName = "/" + PREFIX + String.valueOf(this.umboxId);

        try
        {
            //docker = new RemoteDocker(new URI(Config.data.get("data_node_ip")));
            docker = new LocalDocker(new File("/var/run/docker.sock"));
        }
        catch(Exception e) //URISyntaxException
        {
            System.out.println("Error processing data node IP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected boolean start()
    {
        try
        {
            docker.containers().create(PREFIX + this.umboxId, image.getName());
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
        Containers containers  = docker.containers();
        Iterator<Container> allContainers = containers.all();
        while(allContainers.hasNext())
        {
            Container container = allContainers.next();
            JsonObject containerInfo;
            try
            {
                containerInfo = container.inspect();
            } catch (IOException e)
            {
                System.out.println("Error getting container information: " + e.toString());
                e.printStackTrace();
                return false;
            }

            if(umboxContainerName.equals(containerInfo.getString("Name")))
            {
                try
                {
                    container.stop();
                }
                catch (Exception e)
                {
                    System.out.println("Could not stop container; assuming already stopped.");
                }

                try
                {
                    container.remove();
                    System.out.println("Container successfully removed.");
                    return true;
                }
                catch (IOException e)
                {
                    System.out.println("Problem removing container.");
                    e.printStackTrace();
                    return false;
                }
            }
        }

        System.out.println("Container was not found.");
        return false;
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
