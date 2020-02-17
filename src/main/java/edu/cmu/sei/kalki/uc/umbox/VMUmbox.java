package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.uc.utils.CommandExecutor;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VMUmbox extends Umbox
{
    private static final String UMBOX_TOOL_PATH = "./vm-umbox-tool";
    private static final String UMBOX_TOOL_FILE = "umbox.py";

    private ArrayList<String> commandInfo;
    private String commandWorkingDir;

    public VMUmbox(UmboxImage image, Device device)
    {
        super(image, device);
        setupCommand();
    }

    public VMUmbox(UmboxImage image, Device device, int instanceId)
    {
        super(image, device, instanceId);
        setupCommand();
    }

    /***
     * Common parameters that are the same (needed or optional) for all comands.
     */
    private void setupCommand()
    {
        String dataNodeIP = Config.data.get("data_node_ip");
        String ovsDataBridge = Config.data.get("ovs_data_bridge");
        String controlBridge = Config.data.get("control_bridge");

        commandWorkingDir = Paths.get(System.getProperty("user.dir"), UMBOX_TOOL_PATH).toString();

        // Basic command parameters.
        commandInfo = new ArrayList<>();
        commandInfo.add("pipenv");
        commandInfo.add("run");
        commandInfo.add("python");
        commandInfo.add(UMBOX_TOOL_FILE);
        commandInfo.add("-s");
        commandInfo.add(dataNodeIP);
        commandInfo.add("-u");
        commandInfo.add(String.valueOf(umboxId));
        if(image != null)
        {
            commandInfo.add("-i");
            commandInfo.add(image.getName());
            commandInfo.add("-f");
            commandInfo.add(image.getFileName());
        }
        commandInfo.add("-bc");
        commandInfo.add(controlBridge);
        commandInfo.add("-bd");
        commandInfo.add(ovsDataBridge);
    }

    /**
     * Starts a new umbox.
     */
    @Override
    protected boolean start()
    {
        List<String> output = null;
        List<String> command = (ArrayList) commandInfo.clone();
        command.add("-c");
        command.add("start");

        try
        {
            output = CommandExecutor.executeCommand(command, commandWorkingDir);

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

            return true;
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Could not start umbox properly!");
        }
    }

    /**
     * Stops a running umbox.
     */
    @Override
    protected boolean stop()
    {
        List<String> command = (ArrayList) commandInfo.clone();
        command.add("-c");
        command.add("stop");

        try
        {
            System.out.println("Executing stop command.");
            CommandExecutor.executeCommand(command, commandWorkingDir);
            return true;
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Could not stop umbox properly!");
        }
    }

}
