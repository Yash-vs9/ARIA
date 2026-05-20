package com.yash.nerve.tools;

import com.yash.nerve.config.SandboxConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ShellTools {
    private final SandboxConfig sandboxConfig;
    private static final List<String> BLOCKED = List.of(
            "rm", "sudo", "chmod", "chown", "mkfs",
            "dd", "shutdown", "reboot", ">", "curl", "wget"
    );

    public ShellTools(SandboxConfig sandboxConfig) {
        this.sandboxConfig = sandboxConfig;
    }

    @Tool(description = "Execute a shell command and return its output. " +
            "Use this to find files (ls, find), check directories, or get system info. " +
            "Only use safe read-only commands. Never use rm, sudo, or destructive commands.")
    public String runCommand(String command){
        try{
            String lower = command.toLowerCase();
            for (String blocked : BLOCKED) {
                if (lower.contains(blocked)) {
                    return "Blocked: command contains restricted keyword '" + blocked + "'";
                }
            }

            ProcessBuilder builder=new ProcessBuilder();
            builder.directory(new File(sandboxConfig.getPath()));
            builder.command("sh","-c",command);
            builder.redirectErrorStream(false);

            Process process=builder.start();
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Command timed out after 10 seconds";
            }

            String output = stdout.isEmpty() ? stderr : stdout;
            String[] lines = output.split("\n");
            if (lines.length > 100) {
                output = String.join("\n",
                        java.util.Arrays.copyOfRange(lines, 0, 100)) +
                        "\n... (truncated at 100 lines)";
            }

            return output.isEmpty() ? "Command completed with no output." : output;
        }
        catch (Exception e){
            return "Exception : "+e;
        }

    }
}
