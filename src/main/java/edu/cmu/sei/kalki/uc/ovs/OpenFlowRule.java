package edu.cmu.sei.kalki.uc.ovs;

import java.text.MessageFormat;

/***
 * Represents a flow rule in an OpenFlow command.
 */
public class OpenFlowRule
{
    private static final String DEFAULT_TYPE = "ip";
    private static final String DEFAULT_PRIORITY = "200";

    private String trafficType = DEFAULT_TYPE;
    private String priority = DEFAULT_PRIORITY;

    private String inputPort;
    private String outputPort;
    private String sourceIpAddress = null;
    private String destIpAddress = null;

    public OpenFlowRule(String inputPort, String outputPort, String priority, String sourceIpAddress, String destIpAddress)
    {
        this.inputPort = inputPort;
        this.outputPort = outputPort;
        this.priority = priority;
        this.sourceIpAddress = sourceIpAddress;
        this.destIpAddress = destIpAddress;
    }

    /***
     * Converts the rule into a string representation that can be used by the ovs tool that adds flows.
     * @return
     */
    public String toString()
    {
        String ruleString = "";

        if(trafficType != null)
        {
            ruleString += MessageFormat.format("{0}, ", trafficType);
        }

        if(priority != null)
        {
            ruleString += MessageFormat.format("priority={0}, ", priority);
        }

        if(inputPort != null)
        {
            ruleString += MessageFormat.format("in_port={0}, ", inputPort);
        }

        if(sourceIpAddress != null)
        {
            ruleString += MessageFormat.format("ip_src={0}, ", sourceIpAddress);
        }

        if(destIpAddress != null)
        {
            ruleString += MessageFormat.format("ip_dst={0}, ", destIpAddress);
        }

        if(outputPort != null)
        {
            if(outputPort.equals("-1"))
            {
                ruleString += "actions=drop";
            }
            else
            {
                ruleString += MessageFormat.format("actions=output:{0}, ", outputPort);
            }
        }

        ruleString += "";
        return ruleString;
    }
}
