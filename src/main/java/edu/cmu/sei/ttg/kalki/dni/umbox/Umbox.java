package edu.cmu.sei.ttg.kalki.dni.umbox;

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
    protected String ovsPortName = "";

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
    public String startAndStore()
    {
        try
        {
            List<String> output = start();

            // Assuming the port name was the last thing printed in the output, get it and return it.
            ovsPortName = output.get(output.size() - 1);
            System.out.println("Umbox port name: " + ovsPortName);
            if (ovsPortName != null)
            {
                // Store in the DB the information about the newly created umbox instance.
                UmboxInstance instance = new UmboxInstance(String.valueOf(umboxId), image.getId(), device.getId());
                instance.insert();
            }
            return ovsPortName;
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            return null;
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

            Postgres.findUmboxInstance(String.valueOf(umboxId)).whenComplete((umboxInstance, exception) ->
            {
                System.out.println("Deleting umbox instance from DB.");
                Postgres.deleteUmboxInstance(umboxInstance.getId());
            });
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

}
