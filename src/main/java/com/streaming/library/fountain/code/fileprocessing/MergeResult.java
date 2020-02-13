package com.streaming.library.fountain.code.fileprocessing;

import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
public class MergeResult {

    private final Path reconstructedFile;
    private final List<Exception> exceptions;
    private final FileProcessingResult fileProcessingResult;
    private final List<String> errorMessages;

    public MergeResult(Path reconstructedFile, List<Exception> exceptions, FileProcessingResult fileProcessingResult, List<String> errorMessages) {
        this.reconstructedFile = reconstructedFile;
        this.exceptions = exceptions;
        this.fileProcessingResult = fileProcessingResult;
        this.errorMessages = errorMessages;
    }
}
