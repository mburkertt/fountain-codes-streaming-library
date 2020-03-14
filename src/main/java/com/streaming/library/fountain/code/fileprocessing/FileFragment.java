package com.streaming.library.fountain.code.fileprocessing;

import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
public class FileFragment {
        private final List<Path> fragments;
        private final String checkSum;
        private final List<Exception> exceptions;
        private final FileProcessingResult fileProcessingResult;
        private final List<String> errorMessages;

    public FileFragment(
            List<Path> fragments,
            String checkSum,
            List<Exception> exceptions,
            FileProcessingResult fileProcessingResult,
            List<String> errorMessages) {
        this.fragments = fragments;
        this.checkSum = checkSum;
        this.exceptions = exceptions;
        this.fileProcessingResult = fileProcessingResult;
        this.errorMessages = errorMessages;
    }


}
