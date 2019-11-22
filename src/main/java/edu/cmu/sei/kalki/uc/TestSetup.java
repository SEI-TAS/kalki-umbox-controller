package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;

/***
 * Simple class for setting up quick tests.
 */
public class TestSetup
{
    /**
     * Overwrite default DB params to create a new, temp test DB.
     */
    static void overwriteDBConfig()
    {
        Config.data.put("db_recreate", "true");
        Config.data.put("db_name", "kalkidb_test");
        Config.data.put("db_user", "kalkiuser_test");
    }

    /***
     * Inserts data from the given file to prepare to run simple tests.
     */
    static void insertTestData(String fileName)
    {
        System.out.println("Inserting test data.");

        Postgres.executeSQLFile(fileName);

        System.out.println("Test data finished inserting.");
    }

}
