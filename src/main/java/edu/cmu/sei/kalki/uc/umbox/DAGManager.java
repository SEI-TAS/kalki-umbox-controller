package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.uc.ovs.OpenFlowRule;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSDB;
import edu.cmu.sei.kalki.uc.ovs.RemoteOVSSwitch;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertListener;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.SecurityState;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;
import edu.cmu.sei.ttg.kalki.models.UmboxInstance;

import java.util.ArrayList;
import java.util.List;

public class DAGManager
{
    private final static String OVS_DEVICES_NETWORK_PORT = "1";
    private final static String OVS_EXTERNAL_NETWORK_PORT = "2";

    /**
     * Goes over all devices and sets up umboxes for each of them based on their current state.
     */
    public static void bootstrap()
    {
        // Setup the Umbox type to use from the config file.
        Umbox.setUmboxClass(Config.data.get("umbox_class"));

        // Set up umboxes for existing devices.
        List<Device> devices = Postgres.findAllDevices();
        for(Device device : devices)
        {
            System.out.println("Checking if there are umboxes to be started for device " + device.getName() + ", state " + device.getCurrentState().getName());
            DeviceSecurityState state = device.getCurrentState();
            SecurityState currState = Postgres.findSecurityState(state.getStateId());
            setupUmboxesForDevice(device, currState);
        }
    }

    /**
     * Starts up the listener for device state changes.
     */
    public static void startUpStateListener()
    {
        InsertListener.addHandler(Postgres.TRIGGER_NOTIF_NEW_DEV_SEC_STATE, new DeviceSecurityStateInsertHandler());
        InsertListener.addHandler(Postgres.TRIGGER_NOTIF_NEW_POLICY_INSTANCE, new PolicyInstanceInsertHandler());
        InsertListener.startListening();
    }

    /**
     * Sets up all umboxes for a given device and state. Also clears up previous umboxes if needed.
     * @param device
     * @param currentState
     */
    public static synchronized void setupUmboxesForDevice(Device device, SecurityState currentState)
    {
        List<UmboxInstance> oldUmboxInstances = Postgres.findUmboxInstances(device.getId());
        System.out.println("Found old umbox instances info for device, umboxes running: " + oldUmboxInstances.size());

        // First find umbox images for this device/state.
        List<UmboxImage> umboxImages = Postgres.findUmboxImagesByDeviceTypeAndSecState(device.getType().getId(), currentState.getId());
        System.out.println("Found umboxes for device type " + device.getType().getId() + " and current state " + currentState.getId() + ", number of umboxes: " + umboxImages.size());
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
        clearRedirectForDevice(device);
        setRedirectForDevice(device, OVS_DEVICES_NETWORK_PORT, OVS_EXTERNAL_NETWORK_PORT, newUmboxes);

        // Finally clear the old umboxes.
        stopUmboxes(oldUmboxInstances);
    }

    /**
     * Starts an umbox with the given image and device, getting info about how it is connected.
     * @param image
     * @param device
     */
    private static Umbox setupUmboxForDevice(UmboxImage image, Device device)
    {
        Umbox umbox = Umbox.createUmbox(image, device);
        umbox.startAndStore();

        if(umbox.getOvsInPortId().equals("") || umbox.getOvsOutPortId().equals("") || umbox.getOvsRepliesPortId().equals(""))
        {
            // Get the port ids from the names with a remote API call.
            RemoteOVSDB ovsdb = new RemoteOVSDB(Config.data.get("data_node_ip"));
            String umboxInPortId = ovsdb.getPortId(umbox.getOvsInPortName());
            String umboxOutPortId = ovsdb.getPortId(umbox.getOvsOutPortName());
            String umboxRepliesPortId = ovsdb.getPortId(umbox.getOvsRepliesPortName());
            if (umboxInPortId == null || umboxOutPortId == null || umboxRepliesPortId == null)
            {
                throw new RuntimeException("Could not get port ids!");
            }

            umbox.setOvsInPortId(umboxInPortId);
            umbox.setOvsOutPortId(umboxOutPortId);
            umbox.setOvsRepliesPortId(umboxRepliesPortId);
        }
        else
        {
            System.out.println("Port IDs were already received, not sending extra request to get them.");
        }

        return umbox;
    }

    /**
     * Stops all umboxes for the given device, and clears all rules to them.
     * @param device
     */
    public static void clearAllUmboxesForDevice(Device device)
    {
        System.out.println("Clearing all umboxes for this device.");
        clearRedirectForDevice(device);
        List<UmboxInstance> instances = Postgres.findUmboxInstances(device.getId());
        stopUmboxes(instances);
    }

    /**
     * Stops all umbox instances provided.
     * @param umboxes
     */
    private static void stopUmboxes(List<UmboxInstance> umboxes)
    {
        System.out.println("Stopping all umboxes given.");
        for(UmboxInstance instance : umboxes)
        {
            UmboxImage image = Postgres.findUmboxImage(instance.getUmboxImageId());
            Device device = Postgres.findDevice(instance.getDeviceId());
            Umbox umbox = Umbox.createUmbox(image, device, Integer.parseInt(instance.getAlerterId()));
            umbox.stopAndClear();
        }
    }

    /**
     */
    private static void setRedirectForDevice(Device device, String ovsDevicePort, String ovsExternalPort, List<Umbox> umboxes)
    {
        if(umboxes.size() == 0)
        {
            System.out.println("No umboxes, no rules to set up.");
            return;
        }

        String cleanDeviceIp = cleanDeviceIp(device.getIp());

        List<OpenFlowRule> rules = new ArrayList<>();

        // Setup entry rules for umbox chain.
        System.out.println("Creating entry rules for device: " + cleanDeviceIp);
        Umbox firstUmbox = umboxes.get(0);
        rules.add(new OpenFlowRule(ovsExternalPort, firstUmbox.getOvsInPortId(), "100", null, cleanDeviceIp));
        rules.add(new OpenFlowRule(ovsDevicePort, firstUmbox.getOvsInPortId(), "100", cleanDeviceIp, null));
        rules.add(new OpenFlowRule(firstUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));

        // Setup intermediate rules for umbox chain.
        System.out.println("Creating intermediate rules for device: " + cleanDeviceIp);
        String prevUmboxOutPortId = firstUmbox.getOvsOutPortId();
        for(int i = 1; i < umboxes.size(); i++)
        {
            // We could use only 1 rule here without src/dest IP, but we use two to make it easier later to delete all rules associated to the device IP.
            Umbox currUmbox = umboxes.get(i);
            rules.add(new OpenFlowRule(prevUmboxOutPortId, currUmbox.getOvsInPortId(), "100", null, cleanDeviceIp));
            rules.add(new OpenFlowRule(prevUmboxOutPortId, currUmbox.getOvsInPortId(), "100", cleanDeviceIp, null));
            rules.add(new OpenFlowRule(currUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));
            prevUmboxOutPortId = currUmbox.getOvsOutPortId();
        }

        // Setup exit rules for umbox chain.
        System.out.println("Creating exit rules for device: " + cleanDeviceIp);
        Umbox lastUmbox = umboxes.get(umboxes.size() - 1);
        rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsDevicePort, "100", null, cleanDeviceIp));
        rules.add(new OpenFlowRule(lastUmbox.getOvsOutPortId(), ovsExternalPort, "100", cleanDeviceIp, null));
        rules.add(new OpenFlowRule(lastUmbox.getOvsRepliesPortId(), ovsExternalPort, "100", null, null));

        // Set the OVS switch to actually store the rules.
        System.out.println("Sending rules for device: " + cleanDeviceIp);
        RemoteOVSSwitch vSwitch = new RemoteOVSSwitch(Config.data.get("data_node_ip"));
        for(OpenFlowRule rule : rules)
        {
            vSwitch.addRule(rule);
        }
    }

    /**
     * Clears all rules related to incoming and outgoing traffic for a given device.
     * @param device
     */
    private static void clearRedirectForDevice(Device device)
    {
        System.out.println("Clearing up rules for device: " + device.getIp());

        String cleanDeviceIp = cleanDeviceIp(device.getIp());

        OpenFlowRule allFromDevice = new OpenFlowRule(null, null, null, cleanDeviceIp, null);
        OpenFlowRule allToDevice = new OpenFlowRule(null, null, null, null, cleanDeviceIp);

        RemoteOVSSwitch vSwitch = new RemoteOVSSwitch(Config.data.get("data_node_ip"));
        vSwitch.removeRule(allFromDevice);
        vSwitch.removeRule(allToDevice);
    }

    /**
     * Clean out any ports, if present, in the IP field.
     * @param deviceIp
     * @return
     */
    private static String cleanDeviceIp(String deviceIp)
    {
        String cleanDeviceIp = deviceIp.split(":")[0];
        System.out.println("Cleaned device IP: " + cleanDeviceIp);
        return cleanDeviceIp;
    }
}
