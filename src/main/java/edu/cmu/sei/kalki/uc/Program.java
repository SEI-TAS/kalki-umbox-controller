package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.database.Postgres;
import edu.cmu.sei.kalki.db.utils.Config;
import edu.cmu.sei.kalki.db.utils.TestDB;

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
            Config.load("config.json");

            if(args.length >= 2 && args[0].equals("test"))
            {
                String testFile = args[1];
                TestDB.recreateTestDB();
                TestDB.initialize();
                TestDB.insertTestData(testFile);
            }
            else
            {
                Postgres.initializeFromConfig();
            }

            UmboxController.startUpComponents();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
