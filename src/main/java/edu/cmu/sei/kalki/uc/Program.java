package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.utils.Config;

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
            if(args.length == 1 && args[0].equals("test"))
            {
                IntegrationTestProgram.main(args);
            }
            else
            {
                Config.load("config.json");
                UCSetup.startupDBandAlertComponents();
                UCSetup.startupUmboxBootstrap();
                UCSetup.startupUmboxStateListener();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
