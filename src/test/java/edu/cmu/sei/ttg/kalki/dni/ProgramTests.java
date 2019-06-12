package edu.cmu.sei.ttg.kalki.dni;

import edu.cmu.sei.ttg.kalki.dni.umbox.DAGManager;
import edu.cmu.sei.ttg.kalki.dni.umbox.Umbox;
import edu.cmu.sei.ttg.kalki.dni.umbox.VMUmbox;
import edu.cmu.sei.ttg.kalki.dni.utils.Config;
import edu.cmu.sei.ttg.kalki.database.Postgres;
import edu.cmu.sei.ttg.kalki.models.Device;
import edu.cmu.sei.ttg.kalki.models.DeviceSecurityState;
import edu.cmu.sei.ttg.kalki.models.DeviceType;
import edu.cmu.sei.ttg.kalki.models.UmboxImage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/***
 * Simple class for quick tests.
 */
class ProgramTests
{
    private static int testDeviceId = -1;
    private static int testUmboxImageId = -1;
    private static int testUmboxLookupId = -1;

    private static final int SUSP_DEVICE_STATE_ID = 2;
    private static final String TEST_IMAGE_NAME = "umbox-sniffer";
    private static final String TEST_IMAGE_PATH = "/home/kalki/images/umbox-sniffer.qcow2";

    /**
     * Sets up test DB, main program threads, and config singleton data.
     */
    @BeforeAll
    static void setUpEnviornment() throws IOException, InterruptedException
    {
        Config.load("config.json");

        Config.data.put("db_reset", "true");
        Config.data.put("db_name", "kalkidb_test");
        Config.data.put("db_user", "kalkiuser_test");

        DNISetup.startUpComponents();

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
        Postgres.insertDevice(newDevice).whenComplete((deviceId, devException) -> {
            testDeviceId = deviceId;

            UmboxImage image = new UmboxImage(TEST_IMAGE_NAME, TEST_IMAGE_PATH);
            Postgres.insertUmboxImage(image).whenComplete((umboxImageId, umException) ->
            {
                testUmboxImageId = umboxImageId;
                testUmboxLookupId = Postgres.insertUmboxLookup(umboxImageId, defaultType, SUSP_DEVICE_STATE_ID, 1);
            });
        });
    }

    /***
     * Simple test to try out starting and directing traffic to a umbox.
     */
    void runVmTest()
    {
        Postgres.findDevice(testDeviceId).whenComplete((device, exception) ->
        {
            Postgres.findUmboxImage(testUmboxImageId).whenComplete((image, imageExc) ->
            {
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
            });
        });
    }

    /***
     * Simple test to try out starting and directing traffic to a umbox.
     */
    void runOvsTest()
    {
        Postgres.findDevice(testDeviceId).whenComplete((device, exception) ->
        {
            Postgres.findUmboxImage(testUmboxImageId).whenComplete((image, imageExc) ->
            {
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
            });
        });
    }

    /***
     * Full test based on trigger. Inserts a new sec state for a device, simulating that its state has changed.
     */
    @Test
    void runTriggerTest() throws InterruptedException
    {
        DeviceSecurityState secState = new DeviceSecurityState(testDeviceId, SUSP_DEVICE_STATE_ID);
        Postgres.insertDeviceSecurityState(secState);

        // Simple wait to have time to check out results.
        System.out.println("Sleeping to allow manual evaluation...");
        Thread.sleep(60000);
        System.out.println("Finished sleeping.");

        System.out.println("Clearing rules and stopping umbox");
        Postgres.findDevice(testDeviceId).whenComplete((device, e) ->
        {
            DAGManager.clearUmboxesForDevice(device);
        });

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
}
