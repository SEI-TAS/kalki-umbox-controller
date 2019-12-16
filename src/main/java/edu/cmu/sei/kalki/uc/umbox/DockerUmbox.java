package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

public class DockerUmbox extends Umbox
{
    public DockerUmbox(UmboxImage image, Device device)
    {
        super(image, device);
    }

    public DockerUmbox(UmboxImage image, int instanceId)
    {
        super(image, instanceId);
    }

    @Override
    protected boolean start()
    {
        return false;
    }

    @Override
    protected boolean stop()
    {
        return false;
    }
}
