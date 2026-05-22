package com.yash.nerve.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
@Component
public class FileTools {

    @Value("${nerve.sandbox.path}")
    private String sandboxPath;

    @Tool(description = """
            Write text content into a file inside the sandbox workspace.
            Creates or overwrites the file.
            Use relative paths only, such as:
            - notes.txt
            - Tasks/daily_tasks.txt
            """)
    public String writeIntoFile(String path, String content) throws IOException {
        try{
            if(path == null || path.isBlank()){
                throw new IllegalArgumentException(
                        "Path can not be empty string"
                );
            }


            Path sandboxRoot = Path.of(sandboxPath)
                    .toAbsolutePath()
                    .normalize();

            Path filePath = sandboxRoot
                    .resolve(path)
                    .normalize();

            if (!filePath.startsWith(sandboxRoot)) {
                throw new SecurityException("Access denied");
            }

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);

            return "File written to: " + filePath;
        }
        catch(Exception ex){
            return ex.getMessage();
        }
    }

    @Tool(description = "Read and return the contents of a text file from the sandbox workspace.")
    public String readFile(String path) throws IOException {
        try{

            Path sandboxRoot = Path.of(sandboxPath)
                    .toAbsolutePath()
                    .normalize();

            Path filePath = sandboxRoot
                    .resolve(path)
                    .normalize();

            if (!filePath.startsWith(sandboxRoot)) {
                throw new SecurityException("Access denied");
            }

            return Files.readString(filePath);
        }
        catch (NoSuchFileException ex){
            return ex.getMessage();
        }
        catch (SecurityException ex){
            return ex.getMessage();
        }
        catch (Exception ex){
            return ex.getMessage();
        }

    }
}