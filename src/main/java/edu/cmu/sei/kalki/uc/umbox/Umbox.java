package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;
import edu.cmu.sei.ttg.kalki.models.UmboxInstance;

import java.lang.reflect.Constructor;
import java.util.Random;

public abstract class Umbox
{
    private static final int MAX_INSTANCES = 1000;

    public static String umboxClass;

    protected int umboxId;
    protected Device device;
    protected UmboxImage image;
    protected String ovsInPortName = "";
    protected String ovsOutPortName = "";
    protected String ovsRepliesPortName = "";
    protected String ovsInPortId = "";
    protected String ovsOutPortId = "";
    protected String ovsRepliesPortId = "";

    public static void setUmboxClass(String classPath)
    {
        umboxClass = classPath;
    }

    public static Umbox createUmbox(UmboxImage image, Device device)
    {
        try {
            Constructor con = Class.forName(umboxClass).getConstructor(UmboxImage.class, Device.class);
            return (Umbox) con.newInstance(image, device);
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Error creating umbox: " + e.getMessage());
            return null;
        }
    }

    public static Umbox createUmbox(UmboxImage image, int instanceId)
    {
        try {
            Constructor con = Class.forName(umboxClass).getConstructor(UmboxImage.class, Integer.TYPE);
            return (Umbox) con.newInstance(image, instanceId);
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Error creating umbox: " + e.getMessage());
            return null;
        }
    }

    /***
     * Constructor for new umboxes.
     * @param device
     * @param image
     */
    protected Umbox(UmboxImage image, Device device)
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
    protected Umbox(UmboxImage image, int instanceId)
    {
        this.image = image;
        this.device = null;
        this.umboxId = instanceId;
    }

    /**
     * Starts a new umbox and stores its info in the DB.
     */
    public void startAndStore()
    {
        try
        {
            start();

            // Store in the DB the information about the newly created umbox instance.
            UmboxInstance instance = new UmboxInstance(String.valueOf(umboxId), image.getId(), device.getId());
            instance.insert();
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
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
     */
    protected abstract boolean start();

    /**
     * Stops a running umbox.
     */
    protected abstract boolean stop();

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
