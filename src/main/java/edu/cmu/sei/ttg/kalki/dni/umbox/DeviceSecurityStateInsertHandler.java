package edu.cmu.sei.ttg.kalki.dni.umbox;

import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertHandler;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;
import edu.cmu.sei.ttg.kalki.models.UmboxInstance;

import java.util.List;

public class DeviceSecurityStateInsertHandler implements InsertHandler
{
    @Override
    public void handleNewInsertion(int deviceSecurityStateId)
    {
        System.out.println("Handling new device security state insertion with id: <" + deviceSecurityStateId + ">.");
        DeviceSecurityState stateChange = Postgres.findDeviceSecurityState(deviceSecurityStateId);

        if(stateChange == null)
        {
            System.out.println("Device security state with given id could not be loaded from DB (" + deviceSecurityStateId + ")");
            return;
        }

        int deviceId = stateChange.getDeviceId();

        // Find umboxes for given state and device
        // Get umbox image info.
        Device device = Postgres.findDevice(deviceId);
        System.out.println("Found device info for device with id " + deviceId);

        List<UmboxInstance> umboxInstances = Postgres.findUmboxInstances(deviceId);
        System.out.println("Found umbox instances info for device, umboxes running: " + umboxInstances.size());

        // First clear the current umboxes.
        for(UmboxInstance instance : umboxInstances)
        {
            // Image is not really needed for existing umboxes that we just want to stop, thus null.
            Umbox umbox = new VMUmbox(null, Integer.parseInt(instance.getAlerterId()));
            DAGManager.clearUmboxForDevice(umbox, device);
        }

        // TODO: better sync this? Maybe first create new umboxes, then clear previous rules,
        //  then redirect and then stop old ones?
        // Now create the new ones.
        List<UmboxImage> umboxImages = Postgres.findUmboxImagesByDeviceTypeAndSecState(device.getType().getId(), stateChange.getStateId());

        // TODO: add support for multiple umbox images in one DAG, at least as a pipe, one after another.
        System.out.println("Found umboxes for device type " + device.getType().getId() + " and current state " + stateChange.getStateId() + " , number of umboxes: " + umboxImages.size());
        if(umboxImages.size() > 0)
        {
            UmboxImage image = umboxImages.get(0);
            System.out.println("Starting umbox and setting rules.");

            try
            {
                DAGManager.setupUmboxForDevice(image, device);
            }
            catch(RuntimeException e)
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
}
