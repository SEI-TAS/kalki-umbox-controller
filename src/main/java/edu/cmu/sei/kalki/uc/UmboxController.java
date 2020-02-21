package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.alerts.AlertServerStartup;
import edu.cmu.sei.kalki.uc.umbox.DAGManager;

/**
 * Main class for the controller.
 */
public class UmboxController
{
    /**
     * Sets up in memory components.
     */
    public static void startUpComponents()
    {
        AlertServerStartup.start();
        DAGManager.bootstrap();
        DAGManager.startUpDBListener();
    }
}
