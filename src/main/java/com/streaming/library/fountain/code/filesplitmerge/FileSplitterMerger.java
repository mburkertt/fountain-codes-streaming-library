package com.streaming.library.fountain.code.filesplitmerge;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileSplitterMerger {

 /**
  * Split a file into multiples files.
  *
  * @param fileToSplit     Name of file to be split
  * @param chunkSizeInByte maximum number of byte per file
  * @param targetDirectory target where the split outcome is saved
  */
 FileFragment split(Path fileToSplit, long chunkSizeInByte, Path targetDirectory);

 /**
  * Merges a List of files back into one.
  *
  * @param pathWithFilesToMerge path where the splinted files are located
  * @param mergedFile           directory where the outcome should be saved
  * @param checkSum             checksum to verify if the outcome is still equal to origin
  * @return FileProcessingResult success or failure status
  */
 Path merge(Path pathWithFilesToMerge, String checkSum, Path mergedFile, boolean deleteMergedFileFragments) throws IOException;

 FileSplitterMerger getInstance();

 enum FileProcessingResult {
  SUCCESS, FAILURE
 }

 @Getter
 class FileFragment {
  private final List<Path> fragments;
  private final String checkSum;
  private final List<Exception> exceptions;
  private final FileProcessingResult fileProcessingResult;

  public FileFragment(
          List<Path> fragments,
          String checkSum,
          List<Exception> exceptions,
          FileProcessingResult fileProcessingResult) {
   this.fragments = fragments;
   this.checkSum = checkSum;
   this.exceptions = exceptions;
   this.fileProcessingResult = fileProcessingResult;
  }
 }
}
