package edu.cmu.sei.ttg.kalki.dni;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/***
 * Simple class for quick tests.
 */
public class ProgramTests
{
    /**
     * Sets up test DB, main program threads, and config singleton data.
     */
    @BeforeAll
    static void setUpEnvironment() throws IOException, InterruptedException
    {
        IntegrationTestProgram.setUpEnvironment();
    }

    /***
     * Full test based on trigger. Inserts a new sec state for a device, simulating that its state has changed.
     */
    //@Test
    void runTriggerTest() throws InterruptedException
    {
        IntegrationTestProgram.runTriggerTest();
    }
}
