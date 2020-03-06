package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.alerts.AlertServerStartup;
import edu.cmu.sei.kalki.uc.umbox.UmboxManager;

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
        UmboxManager umboxManager = new UmboxManager();
        umboxManager.bootstrap();
        umboxManager.startUpDBListener();
    }
}
