package com.yash.nerve.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.nerve.models.Memory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
@Repository
public class MemoryRepository {
    private ObjectMapper objectMapper;
    private String memoryFilePath;
    public MemoryRepository(ObjectMapper objectMapper, @Value("${nerve.memory.path}") String memoryFilePath){
        this.objectMapper=objectMapper;
        this.memoryFilePath=memoryFilePath;
    }
    public Memory readFile() throws IOException {
        File file=new File(memoryFilePath);
        if(!file.exists()){
            return new Memory();
        }
        return objectMapper.readValue(file,Memory.class);
    }
    public void writeFile(Memory memory) throws IOException {
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(new File(memoryFilePath),memory);
    }
}
