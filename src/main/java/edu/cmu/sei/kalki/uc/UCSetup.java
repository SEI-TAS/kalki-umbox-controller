package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.alerts.AlertServerStartup;
import edu.cmu.sei.kalki.uc.umbox.DAGManager;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;

import java.sql.SQLException;

/**
 * Methods to setup and bootstrap subcomponents.
 */
public class UCSetup
{
    /**
     * Sets up the DB and a connection to it, plus the alert handler.
     */
    public static void startupDBandAlertComponents()
    {
        try
        {
            UCSetup.setupDatabase();
            AlertServerStartup.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Sets up umbox-related components and listeners.
     */
    public static void startupUmboxBootstrap()
    {
        try
        {
            DAGManager.bootstrap();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void startupUmboxStateListener()
    {
        try
        {
            DAGManager.startUpStateListener();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Creates DB objects if needed, but also initializes the DB singleton.
     * @throws SQLException
     */
    private static void setupDatabase() throws SQLException
    {
        String rootUser = Config.data.get("db_root_user");
        String rootPassword = Config.data.get("db_root_password");

        String dbHost = Config.data.get("db_host");
        String dbPort = Config.data.get("db_port");
        String dbName = Config.data.get("db_name");
        String dbUser = Config.data.get("db_user");
        String dbPass = Config.data.get("db_password");
        String recreateDB = Config.data.get("db_recreate");

        if(recreateDB.equals("true"))
        {
            // Recreate DB and user.
            Postgres.removeDatabase(dbHost, rootUser, rootPassword, dbName);
            Postgres.removeUser(dbHost, rootUser, rootPassword, dbUser);
            Postgres.createUserIfNotExists(dbHost, rootUser, rootPassword, dbUser, dbPass);
            Postgres.createDBIfNotExists(dbHost, rootUser, rootPassword, dbName, dbUser);
        }

        // Make initial connection, setting up the singleton.
        Postgres.initialize(dbHost, dbPort, dbName, dbUser, dbPass);
    }
}