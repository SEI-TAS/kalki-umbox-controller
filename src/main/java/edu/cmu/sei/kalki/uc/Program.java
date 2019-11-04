package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.utils.Config;

/**
 * Entry point for the program.
 */

public class Program
{
    /**
     * Entry point for the program.
     * If tests params are received, DB and DB data will be prepared accordingly to run those tests.
     */
    public static void main(String[] args)
    {
        try
        {
            boolean runTests = false;
            String testFile = "tests/test.sql";
            if(args.length >= 1 && args[0].equals("test"))
            {
                runTests = true;
                if (args.length >= 2)
                {
                    testFile = args[1];
                }
            }

            Config.load("config.json");
            if(runTests)
            {
                TestSetup.overwriteDBConfig();
            }

            UCSetup.startupDBandAlertComponents();
            if(runTests)
            {
                TestSetup.insertTestData(testFile);
            }

            UCSetup.startupUmboxBootstrap();
            UCSetup.startupUmboxStateListener();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
