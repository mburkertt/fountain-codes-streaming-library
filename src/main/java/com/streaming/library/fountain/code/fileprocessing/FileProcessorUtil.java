package com.streaming.library.fountain.code.fileprocessing;

import java.nio.file.Path;
import java.util.List;

public interface FileProcessorUtil {

 /**
  * Split a file into multiples files.
  *
  * @param fileToSplit Name of file to be split.
  * @param mBPerSplit  maximum number of MB per file.
  * @param destinationDirectory  maximum number of MB per file.
  */
 FileFragment split(Path fileToSplit, int mBPerSplit, Path destinationDirectory);

 /**
  * Merges a List of files back into one.
  *
  * @param filesToMerge list of path with the files to merge
  * @param destinationDirectory directory where the outcome should be saved
  * @param checkSum checksum to verify if the outcome is still equal to origin
  * @return MergeResult which inherits all information about the merging process and outcome.
  */
 MergeResult merge(List<Path> filesToMerge, Path destinationDirectory, String checkSum);
}
