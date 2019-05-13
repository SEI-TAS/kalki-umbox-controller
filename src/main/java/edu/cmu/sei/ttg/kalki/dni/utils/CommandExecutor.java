package edu.cmu.sei.ttg.kalki.dni.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommandExecutor
{
    /**
     * Executes the given external command.
     * @param commandAndParams
     */
    public static List<String> executeCommand(List<String> commandAndParams)
    {
        List<String> outputs = new ArrayList<>();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.command(commandAndParams);

        try
        {
            Process process = processBuilder.start();
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            System.out.println("Command output: ");
            String line;
            while((line = outputReader.readLine()) != null)
            {
                outputs.add(line);
                System.out.println(line);
            }

            int exitVal = process.waitFor();
            if(exitVal == 0)
            {
                System.out.println("Command execution returned successfully.");
            }
            else
            {
                System.out.println("Command execution returned unsuccessfully.");
                throw new RuntimeException("Command was not executed successfully!");
            }

        }
        catch(IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

        return outputs;
    }
}
