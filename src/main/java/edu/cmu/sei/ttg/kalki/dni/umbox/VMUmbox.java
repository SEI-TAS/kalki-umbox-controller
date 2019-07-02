package edu.cmu.sei.ttg.kalki.dni.umbox;

import edu.cmu.sei.ttg.kalki.dni.utils.CommandExecutor;
import edu.cmu.sei.ttg.kalki.dni.utils.Config;
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

    public VMUmbox(UmboxImage image, int instanceId)
    {
        super(image, instanceId);
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
            commandInfo.add(image.getPath());
        }
        commandInfo.add("-bc");
        commandInfo.add(controlBridge);
        commandInfo.add("-bd");
        commandInfo.add(ovsDataBridge);
    }

    /**
     * Starts a new umbox.
     * @returns the name of the OVS port the umbox was connected to.
     */
    protected List<String> start()
    {
        List<String> command = (ArrayList) commandInfo.clone();
        command.add("-c");
        command.add("start");

        try
        {
            return CommandExecutor.executeCommand(command, commandWorkingDir);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Stops a running umbox.
     */
    protected List<String> stop()
    {
        List<String> command = (ArrayList) commandInfo.clone();
        command.add("-c");
        command.add("stop");

        try
        {
            System.out.println("Executing stop command.");
            return CommandExecutor.executeCommand(command, commandWorkingDir);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            return null;
        }
    }

}
