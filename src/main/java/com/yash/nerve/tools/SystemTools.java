package com.yash.nerve.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Component
public class SystemTools {

    @Tool(description = "Get current RAM usage of the system.")
    public String getRAMUsage() {
        System.out.println("🔧 Tool called: getRAMUsage");

        return runSystemCommand("vm_stat");  // Mac

        // Linux: return runSystemCommand("free -h");
    }

    @Tool(description = "Get top CPU consuming processes right now.")
    public String getCPUUsage() {
        System.out.println("🔧 Tool called: getCPUUsage");

        return runSystemCommand("ps aux -r | head -10");
    }

    @Tool(description = "Get disk space usage.")
    public String getDiskSpace() {
        System.out.println("🔧 Tool called: getDiskSpace");

        return runSystemCommand("df -h | grep -v tmpfs");
    }

    @Tool(description = "Get list of running processes.")
    public String getRunningProcesses() {
        System.out.println("🔧 Tool called: getProcesses");

        return runSystemCommand("ps aux | head -20");
    }

    @Tool(description = "Check what is running on a specific port number.")
    public String checkPort(String port) {
        return runSystemCommand("lsof -i :" + port);
    }

    // Private — not exposed as tool, just a helper
    private String runSystemCommand(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder("sh", "-c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String output = new String(process.getInputStream().readAllBytes());

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Command timed out.";
            }

            return output.isEmpty() ? "No output." : output;

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}