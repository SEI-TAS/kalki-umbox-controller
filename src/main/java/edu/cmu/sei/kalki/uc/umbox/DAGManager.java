package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.uc.ovs.OpenFlowRule;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSDB;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSSwitch;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertListener;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;
import edu.cmu.sei.ttg.kalki.models.UmboxInstance;

import java.util.ArrayList;
import java.util.List;

public class DAGManager
{
    /**
     * Goes over all devices and sets up umboxes for each of them based on their current state.
     */
    public static void bootstrap()
    {
        Umbox.setUmboxClass(Config.data.get("umbox_class"));

        List<Device> devices = Postgres.findAllDevices();
        for(Device device : devices)
        {
            System.out.println("Checking if there are umboxes to be started for device " + device.getName() + ", state " + device.getCurrentState().getName());
            DeviceSecurityState state = device.getCurrentState();
            setupUmboxesForDevice(device, state);
        }
    }

    /**
     * Starts up the listener for device state changes.
     */
    public static void startUpStateListener()
    {
        InsertListener.addHandler(Postgres.TRIGGER_NOTIF_NEW_DEV_SEC_STATE, new DeviceSecurityStateInsertHandler());
        InsertListener.startListening();
    }

    /**
     * Sets up all umboxes for a given device and state. Also clears up previous umboxes if needed.
     * @param device
     * @param currentState
     */
    public static synchronized void setupUmboxesForDevice(Device device, DeviceSecurityState currentState)
    {
        List<UmboxInstance> oldUmboxInstances = Postgres.findUmboxInstances(device.getId());
        System.out.println("Found old umbox instances info for device, umboxes running: " + oldUmboxInstances.size());

        // First find umbox images for this device/state.
        List<UmboxImage> umboxImages = Postgres.findUmboxImagesByDeviceTypeAndSecState(device.getType().getId(), currentState.getStateId());
        System.out.println("Found umboxes for device type " + device.getType().getId() + " and current state " + currentState.getStateId() + ", number of umboxes: " + umboxImages.size());
        if(umboxImages.size() == 0)
        {
            System.out.println("No umboxes associated to this state for this device.");
            return;
        }

        // Then create new umbox instances.
        List<Umbox> newUmboxes = new ArrayList<>();
        for (UmboxImage image : umboxImages)
        {
            try
            {
                System.out.println("Starting umbox instance.");
                Umbox newUmbox = DAGManager.setupUmboxForDevice(image, device);
                newUmboxes.add(newUmbox);
            }
            catch (RuntimeException e)
            {
                System.out.println("Error setting up umbox: " + e.toString());
                e.printStackTrace();
            }
        }

        // Now set up rules between umboxes and networks, and between themselves.
        System.out.println("Setting up rules for umboxes.");
        String ovsDeviceNetworkPort = Config.data.get("ovs_devices_network_port");
        String ovsExternalNetworkPort = Config.data.get("ovs_external_network_port");
        clearRedirectForDevice(device.getIp());
        setRedirectForDevice(device.getIp(), ovsDeviceNetworkPort, ovsExternalNetworkPort, newUmboxes);

        // Finally clear the old umboxes.
        for (UmboxInstance instance : oldUmboxInstances)
        {
            // Image param is not really needed for existing umboxes that we just want to stop, thus null.
            UmboxImage image = Postgres.findUmboxImage(instance.getUmboxImageId());
            Umbox umbox = Umbox.createUmbox(image, Integer.parseInt(instance.getAlerterId()));
            DAGManager.clearUmboxForDevice(umbox, device);
        }
    }

    /**
     * Starts an umbox with the given image and device, getting info about how it is connected.
     * @param image
     * @param device
     */
    public static Umbox setupUmboxForDevice(UmboxImage image, Device device)
    {
        Umbox umbox = Umbox.createUmbox(image, device);

        System.out.println("Starting Umbox.");
        umbox.startAndStore();

        // Get the port ids from the names with a remote API call.
        RemoteOVSDB ovsdb = new RemoteOVSDB(Config.data.get("data_node_ip"));
        String umboxInPortId = ovsdb.getPortId(umbox.getOvsInPortName());
        String umboxOutPortId = ovsdb.getPortId(umbox.getOvsOutPortName());
        String umboxRepliesPortId = ovsdb.getPortId(umbox.getOvsRepliesPortName());
        if(umboxInPortId == null || umboxOutPortId == null || umboxRepliesPortId == null)
        {
            throw new RuntimeException("Could not get port ids!");
        }

        umbox.setOvsInPortId(umboxInPortId);
        umbox.setOvsOutPortId(umboxOutPortId);
        umbox.setOvsRepliesPortId(umboxRepliesPortId);
        return umbox;
    }

    /**
     * Stops a given umbox and clears rules directing traffic to it.
     */
    public static void clearUmboxForDevice(Umbox umbox, Device device)
    {
        System.out.println("Stopping umbox.");
        umbox.stopAndClear();
    }

    /**
     * Stops all umboxes for the given device, and clears all rules to them.
     * @param device
     */
    public static void clearAllUmboxesForDevice(Device device)
    {
        System.out.println("Clearing all umboxes for this device.");

        clearRedirectForDevice(device.getIp());

        List<UmboxInstance> instances = Postgres.findUmboxInstances(device.getId());
        System.out.println("Stopping all umboxes for this device.");
        for(UmboxInstance instance : instances)
        {
            System.out.println("Stopping umbox.");
            UmboxImage image = Postgres.findUmboxImage(instance.getUmboxImageId());
            Umbox umbox = Umbox.createUmbox(image, Integer.parseInt(instance.getAlerterId()));
            umbox.stopAndClear();
        }
    }

    /**
     */
    private static void setRedirectForDevice(String deviceIp, String ovsDevicePort, String ovsExternalPort, List<Umbox> umboxes)
    {
        if(umboxes.size() == 0)
        {
            System.out.println("No umboxes, no rules to set up.");
            return;
        }

        // Clean out any ports, if present, in the IP field.
        deviceIp = deviceIp.split(":")[0];
        System.out.println("Cleaned device: " + deviceIp);

        List<OpenFlowRule> rules = new ArrayList<>();

        // Setup entry rules for umbox chain.
        System.out.println("Creating entry rules for device: " + deviceIp);
        Umbox firstUmbox = umboxes.get(0);
        rules.add(new OpenFlowRule(ovsExternalPort, firstUmbox.getOvsInPortId(), "100", null, deviceIp));
        rules.add(new OpenFlowRule(ovsDevicePort, firstUmbox.getOvsInPortId(), "100", deviceIp, null));   //THIS ONE
        //rules.add(new OpenFlowRule(ovsDevicePort, firstUmbox.getOvsInPortId(), "100", deviceIp, null, null, "10.27.152.5"));   //THIS ONE
        rules.add(new OpenFlowRule(firstUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));

        // Setup intermediate rules for umbox chain.
        System.out.println("Creating intermediate rules for device: " + deviceIp);
        String prevUmboxOutPortId = firstUmbox.getOvsOutPortId();
        for(int i = 1; i < umboxes.size(); i++)
        {
            // We could use only 1 rule here without src/dest IP, but we use two to make it easier later to delete all rules associated to the device IP.
            Umbox currUmbox = umboxes.get(i);
            rules.add(new OpenFlowRule(prevUmboxOutPortId, currUmbox.getOvsInPortId(), "100", null, deviceIp));
            rules.add(new OpenFlowRule(prevUmboxOutPortId, currUmbox.getOvsInPortId(), "100", deviceIp, null));
            rules.add(new OpenFlowRule(currUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));
            prevUmboxOutPortId = currUmbox.getOvsOutPortId();
        }

        // Setup exit rules for umbox chain.
        System.out.println("Creating exit rules for device: " + deviceIp);
        Umbox lastUmbox = umboxes.get(umboxes.size() - 1);
        rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsDevicePort, "100", null, deviceIp));  //THIS ONE
        //rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsDevicePort, "100", null, deviceIp, "10.27.151.217", null));  //THIS ONE
        rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsExternalPort, "100", deviceIp, null));
        rules.add(new OpenFlowRule(lastUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));

        // Set the OVS switch to actually store the rules.
        System.out.println("Sending rules for device: " + deviceIp);
        RemoteOVSSwitch vSwitch = new RemoteOVSSwitch(Config.data.get("data_node_ip"));
        for(OpenFlowRule rule : rules)
        {
            vSwitch.addRule(rule);
        }
    }

    /**
     * Clears all rules related to incoming and outgoing traffic for a given device.
     * @param deviceIp
     */
    private static void clearRedirectForDevice(String deviceIp)
    {
        System.out.println("Clearing up rules for device: " + deviceIp);

        // Clean out any ports, if present, in the IP field.
        deviceIp = deviceIp.split(":")[0];
        System.out.println("Cleaned device: " + deviceIp);

        OpenFlowRule allFromDevice = new OpenFlowRule(null, null, null, deviceIp, null);
        OpenFlowRule allToDevice = new OpenFlowRule(null, null, null, null, deviceIp);

        RemoteOVSSwitch vSwitch = new RemoteOVSSwitch(Config.data.get("data_node_ip"));
        vSwitch.removeRule(allFromDevice);
        vSwitch.removeRule(allToDevice);
    }
}
