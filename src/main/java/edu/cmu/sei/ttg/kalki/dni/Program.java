package edu.cmu.sei.ttg.kalki.dni;

import edu.cmu.sei.ttg.kalki.dni.utils.Config;

/**
 * Entry point for the program.
 */

public class Program
{
    /**
     * Entry point for the program.
     */
    public static void main(String[] args)
    {
        try
        {
            Config.load("config.json");
            DNISetup.startupDBandAlertComponents();
            DNISetup.startupUmboxComponents();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
