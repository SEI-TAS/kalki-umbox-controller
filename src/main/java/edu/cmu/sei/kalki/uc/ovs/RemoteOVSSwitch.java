package edu.cmu.sei.kalki.uc.ovs;

import edu.cmu.sei.kalki.db.utils.CommandExecutor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * Represents a remove OVS switch. Communicates to it through OpenFlow using the ovs-ofctl local command line
 * tool.
 */
public class RemoteOVSSwitch
{
    private static final String TOOL_COMMAND = "ovs-ofctl -O OpenFlow13";
    private static final String SERVER_PARAM = "tcp:{0}:{1,number,#}";

    private static final String CMD_SHOW = "show";
    private static final String CMD_DUMP_FLOWS = "dump-flows";
    private static final String CMD_ADD = "add-flow";
    private static final String CMD_DEL = "del-flows";

    private static final int DEFAULT_PORT = 6653;

    private String serverIp;
    private int port;

    /**
     * Sets the IP of the server.
     */
    public void setServer(String serverIP)
    {
        setServer(serverIP, DEFAULT_PORT);
    }

    /**
     * Sets the IP and port of the server.
     */
    public void setServer(String serverIP, int port)
    {
        this.serverIp = serverIP;
        this.port = port;
    }

    /**
     * Gets information about the OVS switch.
     * @return
     */
    public List<String> getShowInfo()
    {
        return sendCommand(CMD_SHOW, new ArrayList<>());
    }

    /**
     * Gets information about the current flows/rules in the OVS switch.
     */
    public List<String> getFlowsInfo()
    {
        return sendCommand(CMD_DUMP_FLOWS, new ArrayList<>());
    }

    /**
     * Adds a rule to the OVS switch.
     */
    public List<String> addRule(OpenFlowRule rule)
    {
        String ruleCmd = rule.toString();
        return sendCommand(CMD_ADD, new ArrayList<>(Arrays.asList(ruleCmd)));
    }

    /**
     * Removes the given rule from the OVS switch.
     */
    public List<String> removeRule(OpenFlowRule rule)
    {
        String ruleCmd = rule.toString();
        return sendCommand(CMD_DEL, new ArrayList<>(Arrays.asList(ruleCmd)));
    }

    /***
     * Sends a generic command to a remote OVS Switch through the local command line tool.
     */
    private List<String> sendCommand(String command, List<String> arguments)
    {
        if(serverIp == null) {
            throw new RuntimeException("Server IP has not been configured!");
        }

        List<String> commandInfo = new ArrayList<>();
        commandInfo.addAll(Arrays.asList(TOOL_COMMAND.split(" ")));
        commandInfo.add(command);
        commandInfo.add(MessageFormat.format(SERVER_PARAM, serverIp, port));
        commandInfo.addAll(arguments);

        List<String> output = CommandExecutor.executeCommand(commandInfo, "./");
        return output;
    }
}
