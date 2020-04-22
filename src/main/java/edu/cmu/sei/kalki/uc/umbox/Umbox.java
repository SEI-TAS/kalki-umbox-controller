package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.db.daos.UmboxInstanceDAO;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.db.models.UmboxInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public abstract class Umbox
{
    private static final int MAX_INSTANCES = 1000;

    public static Class umboxClass;

    protected int umboxId;
    protected Device device;
    protected UmboxImage image;
    protected String ovsInPortName = "";
    protected String ovsOutPortName = "";
    protected String ovsRepliesPortName = "";
    protected String ovsInPortId = "";
    protected String ovsOutPortId = "";
    protected String ovsRepliesPortId = "";

    public static void setUmboxClass(Class umboxClassToUse)
    {
        umboxClass = umboxClassToUse;
    }

    public static void setUmboxClass(String umboxClassToUse) throws ClassNotFoundException {
        umboxClass = Class.forName(umboxClassToUse);
    }

    public static Umbox createUmbox(UmboxImage image, Device device)
    {
        try {
            Constructor con = umboxClass.getConstructor(UmboxImage.class, Device.class);
            return (Umbox) con.newInstance(image, device);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            throw new RuntimeException("Could not create umbox for the given image and device: " + e.getMessage());
        }
    }

    public static Umbox createUmbox(UmboxImage image, Device device, int instanceId)
    {
        try {
            Constructor con = umboxClass.getConstructor(UmboxImage.class, Device.class, Integer.TYPE);
            return (Umbox) con.newInstance(image, device, instanceId);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            e.printStackTrace();
            throw new RuntimeException("Could not create umbox for the given image and device: " + e.getMessage());
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

        // Generate random id. Check if there is no instance with this ID, and re-generate if there is.
        int tries = 0;
        do
        {
            Random rand = new Random();
            umboxId = rand.nextInt(MAX_INSTANCES);
            tries++;

            if(tries > MAX_INSTANCES)
            {
                throw new RuntimeException("Can't allocate an ID for a new umbox; all of them seem to be allocated.");
            }
        }
        while(UmboxInstanceDAO.findUmboxInstance(String.valueOf(umboxId)) != null);
    }

    /***
     * Constructor for existing umboxes.
     * @param instanceId
     */
    protected Umbox(UmboxImage image, Device device, int instanceId)
    {
        this.image = image;
        this.device = device;
        this.umboxId = instanceId;
    }

    /**
     * Starts a new umbox and stores its info in the DB.
     */
    public boolean startAndStore()
    {
        UmboxInstance instance = null;
        try
        {
            // Store in the DB the information about the newly created umbox instance.
            instance = new UmboxInstance(String.valueOf(umboxId), image.getId(), device.getId());
            instance.insert();

            System.out.println("Starting umbox.");
            return start();
        }
        catch (RuntimeException e)
        {
            System.out.println("Error starting umbox: " + e.toString());
            e.printStackTrace();

            try
            {
                if (instance != null)
                {
                    UmboxInstanceDAO.deleteUmboxInstance(instance.getId());
                }
            }
            catch(Exception ex)
            {
                System.out.println("Error removing instance not properly created: " + ex.toString());
            }

            return false;
        }
    }

    /**
     * Stops a running umbox and clears its info from the DB.
     */
    public boolean stopAndClear()
    {
        try
        {
            System.out.println("Stopping umbox.");
            boolean success = stop();

            UmboxInstance umboxInstance = UmboxInstanceDAO.findUmboxInstance(String.valueOf(umboxId));
            System.out.println("Deleting umbox instance from DB.");
            UmboxInstanceDAO.deleteUmboxInstance(umboxInstance.getId());
            return success;
        }
        catch (RuntimeException e)
        {
            System.out.println("Error stopping umbox: " + e.toString());
            e.printStackTrace();
            return false;
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
