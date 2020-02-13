package com.streaming.library.fountain.code.filesplitmerge;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
class FileSplitterMergerImpl implements FileSplitterMerger {

    private static final String READ_MODE = "r";
    private static final String CHECKSUM = "-Checksum-";
    private static final String CHECKSUM_END = "-ChecksumEnd";
    private static final int ZERO = 0;
    private static final String CHECKSUM_EXCEPTION = "Exception occurred during processing no checksum available";
    private static final String SUFFIX = ".splitPart";
    private static final String BINARY = ".binary";
    private static final String WRITEABLE_MODE = "rw";
    private static final int ONE = 1;
    private static final long BYTE_REMNANT_APPENDER = 1;
    private static final String UNDERSCORE = "_";
    private static final FileSplitterMerger INSTANCE = new FileSplitterMergerImpl();

    private static void cleanUpSplittedFiles(List<Path> fileFragmentsToDelete) {
        fileFragmentsToDelete
                .forEach(fileToDelete -> fileToDelete.toFile().deleteOnExit());
    }

    private static boolean isMergedFileLocationOk(Path mergedFile) {
        Optional<Path> mergedPath = Optional.of(mergedFile.getParent());
        if (!notExists(mergedPath.get())) {
            return true;
        }
        log.warn("MergedFile location is not valid");
        return false;
    }

    private static Path verifyEquality(String checkSumOriginal, Path mergedFile) throws IOException {
        File testFileMerged = FileUtils.getFile(mergedFile.toFile());
        String checkSumMerge = checksum(testFileMerged);
        if (checkSumOriginal.contentEquals(checkSumMerge)) {
            return mergedFile;
        }
        throw new IllegalArgumentException("Checksum of the merged file is not correct");
    }

    private static boolean notExists(Path pathToCheck) {
        if (Files.notExists(pathToCheck)) {
            log.warn(pathToCheck.toString() + " has to be an existing file");
            return true;
        }
        return false;
    }

    private static String checksum(File file) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.sha256Hex(fileInputStream);
        }
    }

    private static void paramValidation(Path fileToSplit, long chunkSizeInByte, Path targetDirectory) {
        if (chunkSizeInByte <= 0) {
            throw new InvalidParameterException("Chunk size is not allowed to be smaller than 1");
        }
        if (notExists(targetDirectory)) {
            throw new InvalidPathException("No valid target directory parameter entered", targetDirectory.toString());
        }
        if (notExists(fileToSplit)) {
            throw new InvalidPathException("No valid fileToSplit parameter entered", fileToSplit.toString());
        }
    }

    private static void writePartToFile(
            Path targetDirectory,
            FileChannel sourceChannel,
            long position,
            long chunkSizeInByte,
            List<Path> partFiles,
            String checkSum,
            long amountOfSplits,
            String originalFileName) throws IOException {
        long currentChunk = partFiles.size() + ONE;
        Path fileName = Paths.get(targetDirectory.toString()).resolve(
                originalFileName
                        + BINARY
                        + CHECKSUM
                        + checkSum
                        + CHECKSUM_END
                        + SUFFIX
                        + splitNumberWithDigits(amountOfSplits, currentChunk)
                        + UNDERSCORE
                        + amountOfSplits);
        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), WRITEABLE_MODE);
             FileChannel toChannel = toFile.getChannel()) {
            //noinspection resource (justification: we need position and try block makes no sense for this)
            sourceChannel.position(position);
            toChannel.transferFrom(sourceChannel, ZERO, chunkSizeInByte);
        } catch (IOException e) {
            log.warn("Problem during writing file: {}", fileName.toString());
            throw new IOException(e);
        }
        partFiles.add(fileName);
    }

    private static String splitNumberWithDigits(long amountOfSplits, long currentSplitNumber) {
        int amountOfSplitsDigits = String.valueOf(amountOfSplits).length();
        int currentSplitNumberDigits = String.valueOf(currentSplitNumber).length();
        int digitDifference = amountOfSplitsDigits - currentSplitNumberDigits;
        StringBuilder stringBuilder = new StringBuilder();
        while (digitDifference > ZERO) {
            stringBuilder.append(ZERO);
            digitDifference--;
        }
        stringBuilder.append(currentSplitNumber);
        return stringBuilder.toString();
    }

    private static List<Path> gatherFilesToMerge(Path pathWithFilesToMerge) throws IOException {
        Set<Path> splintedFiles = Files
                .walk(pathWithFilesToMerge)
                .filter(Files::isRegularFile)
                .collect(Collectors.toSet());
        List<String> stringList = new ArrayList<>(splintedFiles)
                .stream()
                .map(path -> path.toAbsolutePath().toString())
                .sorted()
                .collect(Collectors.toList());
        return stringList
                .stream()
                .map(s -> Paths.get(s))
                .collect(Collectors.toList());
    }

    @Override
    public FileSplitterMerger getInstance() {
        return INSTANCE;
    }

    @Override
    public Path merge(Path pathWithFilesToMerge, String checkSum, Path mergedFile, boolean deleteMergedFileFragments) throws IOException {
        List<Path> fileFragments = gatherFilesToMerge(pathWithFilesToMerge);
        List<Path> fileFragmentsToDelete = new ArrayList<>();
        Optional<Path> mergedPath = Optional.of(mergedFile.getParent());
        Files.createDirectories(mergedPath.get());
        if (!isMergedFileLocationOk(mergedFile)) {
            throw new InvalidPathException("Not valid mergedFile parameter entered", mergedFile.toString());
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(mergedFile.toFile());
             BufferedOutputStream mergingStream = new BufferedOutputStream(fileOutputStream)) {
            for (Path fileToMerge : fileFragments) {
                Files.copy(fileToMerge, mergingStream);
                fileFragmentsToDelete.add(fileToMerge);
            }
            if (deleteMergedFileFragments) {
                cleanUpSplittedFiles(fileFragmentsToDelete);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
        return verifyEquality(checkSum, mergedFile);
    }

    @Override
    public FileFragment split(Path fileToSplit, long chunkSizeInByte, Path targetDirectory) {
        List<Path> partFiles = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        try {
            Files.createDirectories(targetDirectory);
        } catch (IOException e) {
            exceptions.add(e);
            return new FileFragment(partFiles, CHECKSUM_EXCEPTION, exceptions, FileProcessingResult.FAILURE);
        }
        paramValidation(fileToSplit, chunkSizeInByte, targetDirectory);
        Optional<String> fileToSplitNameOpt = Optional.of(fileToSplit.toString());
        try (RandomAccessFile sourceFile = new RandomAccessFile(fileToSplitNameOpt.get(), READ_MODE);
             FileChannel sourceChannel = sourceFile.getChannel()) {
            String checkSum = checksum(fileToSplit.toFile());
            long sourceSize = Files.size(fileToSplit);
            long amountOfSplitsWithoutRemnant = sourceSize / chunkSizeInByte;
            long remainingBytes = sourceSize % chunkSizeInByte;
            long amountOfSplits = amountOfSplitsWithoutRemnant + BYTE_REMNANT_APPENDER;
            int chunkNumber = ZERO;
            Optional<Path> fileNameToSplitOpt = Optional.of(fileToSplit.getFileName());
            while (chunkNumber < amountOfSplitsWithoutRemnant) {
                writePartToFile(
                        targetDirectory,
                        sourceChannel,
                        chunkNumber * chunkSizeInByte,
                        chunkSizeInByte,
                        partFiles,
                        checkSum,
                        amountOfSplits,
                        fileNameToSplitOpt.get().toString());
                chunkNumber++;
            }
            if (remainingBytes > ZERO) {
                writePartToFile(
                        targetDirectory,
                        sourceChannel,
                        chunkNumber * chunkSizeInByte,
                        remainingBytes,
                        partFiles,
                        checkSum,
                        amountOfSplits,
                        fileNameToSplitOpt.get().toString());
            }
            return new FileFragment(partFiles, checkSum, exceptions, FileProcessingResult.SUCCESS);
        } catch (IOException e) {
            cleanUpSplittedFiles(partFiles);
            exceptions.add(new IOException("Error during writing file chunks"));
            return new FileFragment(partFiles, CHECKSUM_EXCEPTION, exceptions, FileProcessingResult.FAILURE);
        }
    }
}
