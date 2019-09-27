package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;
import edu.cmu.sei.ttg.kalki.models.UmboxInstance;

import java.util.List;
import java.util.Random;

public abstract class Umbox
{
    private static final int MAX_INSTANCES = 1000;

    protected int umboxId;
    protected Device device;
    protected UmboxImage image;
    protected String ovsInPortName = "";
    protected String ovsOutPortName = "";
    protected String ovsRepliesPortName = "";
    protected String ovsInPortId = "";
    protected String ovsOutPortId = "";
    protected String ovsRepliesPortId = "";

    /***
     * Constructor for new umboxes.
     * @param device
     * @param image
     */
    public Umbox(UmboxImage image, Device device)
    {
        this.image = image;
        this.device = device;

        // Generate random id.
        Random rand = new Random();
        umboxId = rand.nextInt(MAX_INSTANCES);
    }

    /***
     * Constructor for existing umboxes.
     * @param instanceId
     */
    public Umbox(UmboxImage image, int instanceId)
    {
        this.image = image;
        this.device = null;
        this.umboxId = instanceId;
    }

    /**
     * Starts a new umbox and stores its info in the DB.
     * @returns the name of the OVS port the umbox was connected to.
     */
    public void startAndStore()
    {
        List<String> output = start();

        // Assuming the port name was the last thing printed in the output, get it and process it.
        String ovsPortNames = output.get(output.size() - 1);
        System.out.println("Umbox port names: " + ovsPortNames);
        if (ovsPortNames == null)
        {
            throw new RuntimeException("Could not get umbox OVS ports!");
        }

        String[] portNames = ovsPortNames.split(" ");
        if(portNames.length != 3)
        {
            throw new RuntimeException("Could not get 3 OVS port names!");
        }

        // Locally store the port names.
        this.setOvsInPortName(portNames[0]);
        this.setOvsOutPortName(portNames[1]);
        this.setOvsRepliesPortName(portNames[2]);

        // Store in the DB the information about the newly created umbox instance.
        UmboxInstance instance = new UmboxInstance(String.valueOf(umboxId), image.getId(), device.getId());
        instance.insert();
    }

    /**
     * Stops a running umbox and clears its info from the DB.
     */
    public void stopAndClear()
    {
        try
        {
            stop();

            UmboxInstance umboxInstance = Postgres.findUmboxInstance(String.valueOf(umboxId));
            System.out.println("Deleting umbox instance from DB.");
            Postgres.deleteUmboxInstance(umboxInstance.getId());
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Starts a new umbox.
     * @returns the name of the OVS port the umbox was connected to.
     */
    protected abstract List<String> start();

    /**
     * Stops a running umbox.
     */
    protected abstract List<String> stop();

    // Getters and setters.

    public String getOvsInPortName()
    {
        return ovsInPortName;
    }

    public void setOvsInPortName(String ovsInPortName)
    {
        this.ovsInPortName = ovsInPortName;
    }

    public String getOvsOutPortName()
    {
        return ovsOutPortName;
    }

    public void setOvsOutPortName(String ovsOutPortName)
    {
        this.ovsOutPortName = ovsOutPortName;
    }

    public String getOvsInPortId()
    {
        return ovsInPortId;
    }

    public void setOvsInPortId(String ovsInPortId)
    {
        this.ovsInPortId = ovsInPortId;
    }

    public String getOvsOutPortId()
    {
        return ovsOutPortId;
    }

    public void setOvsOutPortId(String ovsOutPortId)
    {
        this.ovsOutPortId = ovsOutPortId;
    }

    public String getOvsRepliesPortName()
    {
        return ovsRepliesPortName;
    }

    public void setOvsRepliesPortName(String ovsRepliesPortName)
    {
        this.ovsRepliesPortName = ovsRepliesPortName;
    }

    public String getOvsRepliesPortId()
    {
        return ovsRepliesPortId;
    }

    public void setOvsRepliesPortId(String ovsRepliesPortId)
    {
        this.ovsRepliesPortId = ovsRepliesPortId;
    }
}
