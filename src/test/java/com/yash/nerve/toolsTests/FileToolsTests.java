package com.yash.nerve.toolsTests;

import com.yash.nerve.tools.FileTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class FileToolsTests {
    private FileTools fileTools;
    @BeforeEach
    public void initialiseFileTools(){
        fileTools=new FileTools();
        ReflectionTestUtils.setField(
                fileTools,
                "sandboxPath",
                "src/test/resources/sandbox"
        );
    }
    @Test
    public void writeIntoFileTest() throws IOException {
        String path="./fold.txt";
        String content="test";
        String result=fileTools.writeIntoFile(path,content);
        System.out.println(result);
        assertTrue(result.contains("File written to:"));
    }
    @Test
    void shouldGiveErrorToEmptyPaths() throws IOException {
        String path="";
        String content="test";
        String result=fileTools.writeIntoFile(path,content);
        assertTrue(result.contains("Path can not be empty string"));

    }
}
