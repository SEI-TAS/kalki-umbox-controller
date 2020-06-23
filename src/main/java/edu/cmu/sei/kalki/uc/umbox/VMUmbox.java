package edu.cmu.sei.kalki.uc.umbox;

import edu.cmu.sei.kalki.db.utils.CommandExecutor;
import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VMUmbox extends Umbox
{
    private static final String UMBOX_TOOL_PATH = "./vm-umbox-tool";
    private static final String UMBOX_TOOL_FILE = "umbox.py";

    protected static final Logger logger = Logger.getLogger(VMUmbox.class.getName());

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
        String dataNodeIP = device.getDataNode().getIpAddress();

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
            logger.info("Umbox port names: " + ovsPortNames);
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
            logger.info("Executing stop command.");
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
