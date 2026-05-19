package com.yash.nerve.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
@Component
public class FileTools {
    @Tool(description = "Write text content into a file at the given path. Creates or overwrites the file. Only execute this when you are asked to write into a file")
    public void writeIntoFile(String path,String content) throws IOException {
        Files.writeString(Path.of(path), content);
    }
    @Tool(description = "Read and return the contents of a text file from the given path. Only use this when the user explicitly asks to read, open, or access a specific file and provides a file path.")
    public String readFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
