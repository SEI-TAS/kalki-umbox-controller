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

import java.util.List;

public class DAGManager
{
    /**
     * Goes over all devices and sets up umboxes for each of them based on their current state.
     */
    public static void bootstrap()
    {
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
    public static void setupUmboxesForDevice(Device device, DeviceSecurityState currentState)
    {
        // First clear the current umboxes.
        List<UmboxInstance> umboxInstances = Postgres.findUmboxInstances(device.getId());
        System.out.println("Found umbox instances info for device, umboxes running: " + umboxInstances.size());
        for (UmboxInstance instance : umboxInstances)
        {
            // Image param is not really needed for existing umboxes that we just want to stop, thus null.
            Umbox umbox = new VMUmbox(null, Integer.parseInt(instance.getAlerterId()));
            DAGManager.clearUmboxForDevice(umbox, device);
        }

        // TODO: better sync this? Maybe first create new umboxes, then clear previous rules,
        //  then redirect and then stop old ones?
        // Now create the new ones.
        List<UmboxImage> umboxImages = Postgres.findUmboxImagesByDeviceTypeAndSecState(device.getType().getId(), currentState.getStateId());

        // TODO: add support for multiple umbox images in one DAG, at least as a pipe, one after another.
        System.out.println("Found umboxes for device type " + device.getType().getId() + " and current state " + currentState.getStateId() + " , number of umboxes: " + umboxImages.size());
        String ovsInternalPort = Config.data.get("ovs_devices_network_port");
        String ovsExternalPort = Config.data.get("ovs_external_network_port");
        if (umboxImages.size() > 0)
        {
            UmboxImage image = umboxImages.get(0);
            System.out.println("Starting umbox and setting rules.");

            try
            {
                DAGManager.setupUmboxForDevice(image, device, ovsInternalPort, ovsExternalPort);
            }
            catch (RuntimeException e)
            {
                System.out.println("Error setting up umbox: " + e.toString());
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("No umbox associated to this state.");
        }
    }

    /**
     * Starts an umbox with the given image and device, and redirects traffic to/from that device to it.
     * @param image
     * @param device
     */
    public static Umbox setupUmboxForDevice(UmboxImage image, Device device, String ovsInternalPort, String ovsExternalPort)
    {
        // Here we are explicitly stating we are using VMUmboxes. If we wanted to change to another implementation,
        // for now it would be enough to change it here.
        Umbox umbox = new VMUmbox(image, device);

        System.out.println("Starting Umbox.");
        String portName = umbox.startAndStore();
        System.out.println("Port names : " + portName);
        if(portName == null)
        {
            throw new RuntimeException("Could not get umbox OVS port!");
        }

        String[] portNames = portName.split(" ");
        if(portNames.length != 2)
        {
            throw new RuntimeException("Could not get 2 OVS port names!");
        }

        clearRedirectForDevice(device.getIp());

        RemoteOVSDB ovsdb = new RemoteOVSDB(Config.data.get("data_node_ip"));
        String umboxInPortId = ovsdb.getPortId(portNames[0]);
        String umboxOutPortId = ovsdb.getPortId(portNames[1]);
        if(umboxInPortId != null && umboxOutPortId != null)
        {
            redirectToUmbox(device.getIp(), ovsInternalPort, umboxInPortId, umboxOutPortId, ovsExternalPort);
        }

        return umbox;
    }

    /**
     * Stops a given umbox and clears rules directing traffic to it.
     */
    public static void clearUmboxForDevice(Umbox umbox, Device device)
    {
        clearRedirectForDevice(device.getIp());

        System.out.println("Stopping umbox.");
        umbox.stopAndClear();
    }

    /**
     * Stops all umboxes for the given device, and clears all rules to them.
     * @param device
     */
    public static void clearUmboxesForDevice(Device device)
    {
        System.out.println("Clearing all umboxes for this device.");

        clearRedirectForDevice(device.getIp());

        List<UmboxInstance> instances = Postgres.findUmboxInstances(device.getId());
        System.out.println("Stopping all umboxes for this device.");
        for(UmboxInstance instance : instances)
        {
            System.out.println("Stopping umbox.");
            UmboxImage image = Postgres.findUmboxImage(instance.getUmboxImageId());
            Umbox umbox = new VMUmbox(image, Integer.parseInt(instance.getAlerterId()));
            umbox.stopAndClear();
        }
    }

    /**
     * Sends all OpenFlow rules needed to redirect traffic from and to a device to a given umbox.
     * @param deviceIp
     * @param ovsInternalPort
     * @param ovsUmboxInPort
     * @param ovsUmboxOutPort     *
     * @param ovsExternalPort
     */
    private static void redirectToUmbox(String deviceIp, String ovsInternalPort, String ovsUmboxInPort, String ovsUmboxOutPort, String ovsExternalPort)
    {
        OpenFlowRule extToUmbox = new OpenFlowRule(ovsExternalPort, ovsUmboxInPort, "100", null, deviceIp);
        OpenFlowRule umboxToInt = new OpenFlowRule(ovsUmboxOutPort, ovsInternalPort, "110", null, deviceIp);
        OpenFlowRule intToUmbox = new OpenFlowRule(ovsInternalPort, ovsUmboxInPort, "100", deviceIp, null);
        OpenFlowRule umboxToExt = new OpenFlowRule(ovsUmboxOutPort, ovsExternalPort, "110", deviceIp, null);

        RemoteOVSSwitch vSwitch = new RemoteOVSSwitch(Config.data.get("data_node_ip"));
        vSwitch.addRule(extToUmbox);
        vSwitch.addRule(umboxToInt);
        vSwitch.addRule(intToUmbox);
        vSwitch.addRule(umboxToExt);
    }

    /**
     * Clears all rules related to incoming and outgoing traffic for a given device.
     * @param deviceIp
     */
    private static void clearRedirectForDevice(String deviceIp)
    {
        System.out.println("Clearing up rules for device: " + deviceIp);

        OpenFlowRule allFromDevice = new OpenFlowRule(null, null, null, deviceIp, null);
        OpenFlowRule allToDevice = new OpenFlowRule(null, null, null, null, deviceIp);

        RemoteOVSSwitch vSwitch = new RemoteOVSSwitch(Config.data.get("data_node_ip"));
        vSwitch.removeRule(allFromDevice);
        vSwitch.removeRule(allToDevice);
    }
}
