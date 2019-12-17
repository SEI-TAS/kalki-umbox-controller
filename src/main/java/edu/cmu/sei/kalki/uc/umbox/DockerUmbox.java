package edu.cmu.sei.kalki.uc.umbox;

import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.Docker;
import com.amihaiemil.docker.RemoteDocker;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DockerUmbox extends Umbox
{
    private static final String PREFIX = "umbox-";

    private static Docker docker;
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
        try
        {
            docker = new RemoteDocker(new URI(Config.data.get("data_node_ip")));
        }
        catch(URISyntaxException e)
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
        catch (IOException e)
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
        for(Container container : containers)
        {
            if(container.getString("name").equals(String.valueOf(this.umboxId)))
            {
                try
                {
                    container.stop();
                    return true;
                } catch (IOException e)
                {
                    System.out.println("Problem stopping container.");
                    e.printStackTrace();
                    return false;
                }
            }
        }

        System.out.println("Container was not found.");
        return false;
    }
}
