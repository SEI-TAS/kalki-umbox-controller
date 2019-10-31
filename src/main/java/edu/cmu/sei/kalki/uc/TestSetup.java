package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

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

        System.out.println("Reading from file: "+fileName);
        try {
            InputStream is = new FileInputStream(fileName);
            Scanner s = new Scanner(is);

            String line, statement="";
            while(s.hasNextLine()){
                line = s.nextLine();
                if(line.equals("") || line.equals(" ")) {
                    Postgres.executeCommand(statement);
                    statement="";
                } else {
                    statement += line;
                }
            }
            if (!statement.equals(""))
                Postgres.executeCommand(statement);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Test data finished inserting.");
    }

}
