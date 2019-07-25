package edu.cmu.sei.ttg.kalki.dni.alerts;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class AlertServerStartup
{
    private static final String ALERT_URL = "/alert";
    private static final int SERVER_PORT = 6060;

    /**
     * Simply starts a Jetty server, with a handler for new alert notifications.
     */
    public static void start()
    {
        try
        {
            Server httpServer = new Server(SERVER_PORT);
            ServletContextHandler handler = new ServletContextHandler(httpServer, ALERT_URL);
            handler.addServlet(AlertHandlerServlet.class, "/");
            httpServer.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}
