package com.streaming.library.fountain.code;

import com.streaming.library.fountain.code.fileprocessing.FileProcessorUtil;
import com.streaming.library.fountain.code.fileprocessing.FileProcessorUtilImpl;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Paths;

public class FileProcessorUtilTest {

    FileProcessorUtil classUnderTest = new FileProcessorUtilImpl();

    @Test
    public void test(){
        //arrange
        String testFileName = "testfile.log";
        //InputStream inputStream = Files.getClass().getClassLoader().getResourceAsStream(testFileName);

        //act

        //assert
    }
}
