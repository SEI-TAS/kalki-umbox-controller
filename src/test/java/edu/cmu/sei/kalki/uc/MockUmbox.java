package edu.cmu.sei.kalki.uc;

import edu.cmu.sei.kalki.db.models.Device;
import edu.cmu.sei.kalki.db.models.UmboxImage;
import edu.cmu.sei.kalki.uc.umbox.Umbox;

/**
 * Mock class used to track stuff about umboxes. Not using Mockito since there are multiple of these local variables
 * used inside the UmboxManager code.
 */
public class MockUmbox extends Umbox
{
    public static int numStartTimesCalled = 0;
    public static int numStopTimesCalled = 0;

    public static void reset() {
        numStartTimesCalled = 0;
        numStopTimesCalled = 0;
    }

    public MockUmbox(UmboxImage image, Device device)
    {
        super(image, device);
    }

    public MockUmbox(UmboxImage image, Device device, int instanceId)
    {
        super(image, device, instanceId);
    }

    @Override
    protected boolean start() {
        numStartTimesCalled++;
        ovsInPortId = "3";
        ovsOutPortId = "4";
        ovsRepliesPortId = "5";
        return true;
    }

    @Override
    protected boolean stop() {
        numStopTimesCalled++;
        return true;
    }
}
