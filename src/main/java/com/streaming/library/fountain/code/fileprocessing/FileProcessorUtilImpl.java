package com.streaming.library.fountain.code.fileprocessing;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FileProcessorUtilImpl implements FileProcessorUtil {

    private static final String FILE_SUFFIX = "_.fragment";
    private static final String READ_MODE = "r";
    private static final String WRITE_MODE = "rw";
    private static final long KILO_BYTE = 1024L;
    private static final String POSITION = "-POSITION_";
    private static int ZERO = 0;
    private static int ONE = 1;

    @Override
    public FileFragment split(final Path fileToSplit, final int mBPerSplit, final Path destinationDirectory) {
        stateIsOkFor(mBPerSplit);
        List<Exception> exceptions = new ArrayList<>();
        FileProcessingResult fileProcessingResult = FileProcessingResult.SUCCESS;
        List<String> errorMessages = new ArrayList<>();
        List<Path> partFiles = new ArrayList<>();
        final long sourceSize = calculateSourceSize(fileToSplit);
        final long bytesPerSplit = calculateBytesPerSplit(mBPerSplit);
        final long numSplits = sourceSize / bytesPerSplit;
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        try (RandomAccessFile sourceFile = new RandomAccessFile(fileToSplit.toString(), READ_MODE);
             FileChannel sourceChannel = sourceFile.getChannel()) {
            for (; position < numSplits; position++) {
                writePartToFile(bytesPerSplit, position * bytesPerSplit, sourceChannel, partFiles, destinationDirectory);
            }
            if (remainingBytes > 0) {
                writePartToFile(remainingBytes, position * bytesPerSplit, sourceChannel, partFiles, destinationDirectory);
            }
        } catch (IOException e) {
            exceptions.add(e);
            errorMessages.add("Error during Files.size operation with filename: " + partFiles.toString());
            fileProcessingResult = FileProcessingResult.FAILURE;
        }
        return new FileFragment(partFiles, calculateChecksum(fileToSplit), exceptions, fileProcessingResult, errorMessages);
    }

    @Override
    public MergeResult merge(List<Path> filesToMerge, Path destinationDirectory, String checkSum) {
        List<Exception> exceptions = new ArrayList<>();
        FileProcessingResult fileProcessingResult = FileProcessingResult.SUCCESS;
        List<String> errorMessages = new ArrayList<>();
        try (FileOutputStream fileOutputStream = new FileOutputStream(destinationDirectory.toFile());
             BufferedOutputStream mergingStream = new BufferedOutputStream(fileOutputStream)) {
            filesToMerge.forEach(fileToMerge -> writeFile(fileToMerge, mergingStream));
        } catch (IOException e) {
            exceptions.add(e);
            errorMessages.add("Error during Files.size operation");
            fileProcessingResult = FileProcessingResult.FAILURE;
        }
        return new MergeResult(destinationDirectory, exceptions, fileProcessingResult, errorMessages);
    }

    private static void writePartToFile(long byteSize, long position, FileChannel sourceChannel, List<Path> partFiles, final Path destinationDirectory) throws IOException {
        Path fileName = Paths.get(destinationDirectory.toString() + UUID.randomUUID() + POSITION + position + FILE_SUFFIX);
        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), WRITE_MODE);
             FileChannel toChannel = toFile.getChannel()) {
            sourceChannel.position(position);
            toChannel.transferFrom(sourceChannel, ZERO, byteSize);
        }
        partFiles.add(fileName);
    }

    private static void stateIsOkFor(final int mBPerSplit) {
        if (mBPerSplit <= 0) {
            throw new UncheckedIOException(new IOException("mBPerSplit must be more than zero"));
        }
    }

    private static long calculateSourceSize(final Path fileToSplit) {
        try {
            return Files.size(fileToSplit);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long calculateBytesPerSplit(final int mBPerSplit) {
        return KILO_BYTE * KILO_BYTE * mBPerSplit;
    }

    private static String calculateChecksum(Path fileToSplit) {
        try {
            return DigestUtils.sha256Hex(Files.newInputStream(fileToSplit));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path getLastElementOf(List<Path> partFiles){
        if(!partFiles.isEmpty()){
            return partFiles.get(partFiles.size() - ONE);
        }
        return Path.of("no valid path");
    }

    private static void writeFile(Path fileToMerge, BufferedOutputStream mergingStream){
        try {
            Files.copy(fileToMerge, mergingStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
