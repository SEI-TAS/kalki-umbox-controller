package edu.cmu.sei.kalki.uc.ovs;

import edu.cmu.sei.kalki.db.utils.CommandExecutor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * Represents a remove OVS DB. Communicates to it through OpenFlow using the ovs-vsctl local command line
 * tool.
 */
public class RemoteOVSDB
{
    private static final String TOOL_COMMAND = "ovs-vsctl";
    private static final String SERVER_PARAM = "--db=tcp:{0}:{1,number,#}";
    private static final String GET_PORT_COMMAND = "get Interface {0} ofport";

    private static final int DEFAULT_PORT = 6654;

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

    /***
     * Gets the numeric id of a port given the port's name.
     */
    public String getPortId(String portName)
    {
        List<String> output = sendCommand(MessageFormat.format(GET_PORT_COMMAND, portName), new ArrayList<>());
        return output.get(0);
    }

    /***
     * Sends a generic command to a remote OVS DB through the local command line tool.
     */
    private List<String> sendCommand(String command, List<String> arguments)
    {
        if(serverIp == null) {
            throw new RuntimeException("Server IP has not been configured!");
        }
        
        System.out.print("Preparing command to OVS DB.");
        List<String> commandInfo = new ArrayList<>();
        commandInfo.add(TOOL_COMMAND);
        commandInfo.add(MessageFormat.format(SERVER_PARAM, serverIp, port));
        commandInfo.addAll(Arrays.asList(command.split(" ")));
        commandInfo.addAll(arguments);

        System.out.print("Sending command to OVS DB: " + commandInfo.toString());
        List<String> output = CommandExecutor.executeCommand(commandInfo, "./");
        return output;
    }
}
