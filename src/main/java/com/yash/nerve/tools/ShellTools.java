package com.yash.nerve.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ShellTools {
    private static final List<String> BLOCKED = List.of(
            "rm", "sudo", "chmod", "chown", "mkfs",
            "dd", "shutdown", "reboot", ">", "curl", "wget"
    );

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
            builder.command("sh","-c",command);
            builder.redirectErrorStream(true);
            Process process=builder.start();
            BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output=new StringBuilder();
            String line;
            while((line=reader.readLine())!=null){
                output.append(line).append("\n");
            }
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Command timed out after 10 seconds";
            }

            return output.isEmpty() ? "Command executed with no output" : output.toString();
        }
        catch (Exception e){
            return "Exception : "+e;
        }

    }
}
