package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.uc.umbox.DAGManager;
import edu.cmu.sei.kalki.uc.umbox.Umbox;
import edu.cmu.sei.kalki.uc.umbox.VMUmbox;
import edu.cmu.sei.kalki.uc.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.DeviceType;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;
import edu.cmu.sei.ttg.kalki.models.UmboxLookup;

import java.io.IOException;

/***
 * Simple class for quick tests.
 */
public class IntegrationTestProgram
{
    private static int testDeviceId = -1;
    private static int testUmboxImageId = -1;
    private static int testUmboxLookupId = -1;

    private static final int NORM_DEVICE_STATE_ID = 1;
    private static final int SUSP_DEVICE_STATE_ID = 2;
    private static final String TEST_IMAGE_NAME = "umbox-sniffer";
    private static final String TEST_IMAGE_FILENAME = "umbox-sniffer.qcow2";

    /**
     * Sets up test DB, main program threads, and config singleton data.
     */
    static void setUpEnvironment() throws IOException, InterruptedException
    {
        Config.load("config.json");

        Config.data.put("db_recreate", "true");
        Config.data.put("db_setup", "true");

        Config.data.put("db_name", "kalkidb_test");
        Config.data.put("db_user", "kalkiuser_test");

        UCSetup.startupDBandAlertComponents();
        UCSetup.startupUmboxComponents();

        insertTestData();

        // Wait for data to be inserted.
        while(testUmboxLookupId == -1)
        {
            Thread.sleep(100);
        }

        System.out.println("Test data finished inserting.");
    }

    /***
     * Inserts default data to run simple tests.
     */
    private static void insertTestData()
    {
        int defaultType = 1;
        DeviceType defType = new DeviceType(1, "test");
        String deviceIp = "192.168.56.103";
        Device newDevice = new Device("testDevice", "test device", defType, deviceIp, 10, 10);
        int deviceId = Postgres.insertDevice(newDevice);
        testDeviceId = deviceId;

        DeviceSecurityState secState = new DeviceSecurityState(testDeviceId, SUSP_DEVICE_STATE_ID);
        Postgres.insertDeviceSecurityState(secState);

        UmboxImage image = new UmboxImage(TEST_IMAGE_NAME, TEST_IMAGE_FILENAME);
        int umboxImageId = Postgres.insertUmboxImage(image);
        testUmboxImageId = umboxImageId;
        UmboxLookup lookup = new UmboxLookup();
        lookup.setUmboxImageId(umboxImageId);
        lookup.setDeviceTypeId(defaultType);
        lookup.setStateId(SUSP_DEVICE_STATE_ID);
        lookup.setDagOrder(1);
        testUmboxLookupId = Postgres.insertUmboxLookup(lookup);
    }

    /***
     * Simple test to try out starting and directing traffic to a umbox.
     */
    void runVmTest()
    {
        Device device = Postgres.findDevice(testDeviceId);
        UmboxImage image = Postgres.findUmboxImage(testUmboxImageId);
        Umbox umbox = new VMUmbox(image, device);
        System.out.println("Starting VM.");
        umbox.startAndStore();

        int sleepInSeconds = 20;
        try
        {
            Thread.sleep(sleepInSeconds * 1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        System.out.println("Stopping VM");
        umbox.stopAndClear();
    }

    /***
     * Simple test to try out starting and directing traffic to a umbox.
     */
    void runOvsTest()
    {
        Device device = Postgres.findDevice(testDeviceId);
        UmboxImage image = Postgres.findUmboxImage(testUmboxImageId);
        System.out.println("Starting umbox and setting rules.");
        Umbox umbox = DAGManager.setupUmboxForDevice(image, device);

        System.out.println("Waiting for some seconds...");
        int sleepInSeconds = 20;
        try
        {
            Thread.sleep(sleepInSeconds * 1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        System.out.println("Clearing rules and stopping umbox");
        DAGManager.clearUmboxForDevice(umbox, device);
    }

    /***
     * Full test based on trigger. Inserts a new sec state for a device, simulating that its state has changed.
     */
    static void runTriggerTest() throws InterruptedException
    {
        DeviceSecurityState secState = new DeviceSecurityState(testDeviceId, SUSP_DEVICE_STATE_ID);
        Postgres.insertDeviceSecurityState(secState);

        // Simple wait to have time to check out results.
        System.out.println("Sleeping to allow manual evaluation...");
        Thread.sleep(60000);
        System.out.println("Finished sleeping.");

        System.out.println("Clearing rules and stopping umbox");
        Device device = Postgres.findDevice(testDeviceId);
        DAGManager.clearUmboxesForDevice(device);

        System.out.println("Waiting for async cleanup");
        try
        {
            Thread.sleep(5 * 1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

    }

    static void runBootupTest()
    {
        System.out.println("Bootup test!");
        UCSetup.startupUmboxComponents();
    }

    public static void main(String[] args)
    {
        try
        {
            setUpEnvironment();
            runTriggerTest();
            //runBootupTest();
        }
        catch (InterruptedException | IOException e)
        {
            e.printStackTrace();
        }
    }
}
