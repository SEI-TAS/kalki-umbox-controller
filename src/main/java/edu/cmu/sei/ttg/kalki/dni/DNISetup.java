package edu.cmu.sei.ttg.kalki.dni;

import edu.cmu.sei.ttg.kalki.dni.alerts.AlertServerStartup;
import edu.cmu.sei.ttg.kalki.dni.umbox.DAGManager;
import edu.cmu.sei.ttg.kalki.dni.umbox.DeviceSecurityStateInsertHandler;
import edu.cmu.sei.ttg.kalki.dni.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.listeners.InsertListener;

import java.sql.SQLException;

/**
 * Entry point for the program.
 */
public class DNISetup
{
    /**
     * Sets up the DB and a connection to it, plus the alert handler.
     */
    public static void startupDBandAlertComponents()
    {
        try
        {
            DNISetup.setupDatabase();
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
    public static void startupUmboxComponents()
    {
        try
        {
            DAGManager.bootstrap();
            InsertListener.startUpListener(Postgres.TRIGGER_NOTIF_NEW_DEV_SEC_STATE, new DeviceSecurityStateInsertHandler());
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
        String setupDB = Config.data.get("db_setup");

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

        if(setupDB.equals("true"))
        {
            // Create tables, triggers, and more.
            Postgres.setupDatabase();
        }
    }
}