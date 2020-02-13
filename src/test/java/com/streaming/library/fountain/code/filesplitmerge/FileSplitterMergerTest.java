package com.streaming.library.fountain.code.filesplitmerge;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class FileSplitterMergerTest {

    private static final long KILO_BYTE = 1024L;
    private static final Path RESOURCES_PATH = Paths.get("src/test/resources/");
    private static final Path TEST_FILE_PATH = RESOURCES_PATH.resolve("testfile.log");
    private static final String CHECKSUM = "f671811e8beaa0fb6eb17faf5663bd3dd0495dfa54111bad745697e074e69ad5";

    FileSplitterMerger classUnderTest = new FileSplitterMergerImpl();

    private static List<Path> getTestPathSplinted(Path tempDir) {
        List<Path> paths = new ArrayList<>();
        paths.add(tempDir.resolve("splinted-output/testfile.log.binary-Checksum-f671811e8beaa0fb6eb17faf5663bd3dd0495dfa54111bad745697e074e69ad5-ChecksumEnd.splitPart1_3"));
        paths.add(tempDir.resolve("splinted-output/testfile.log.binary-Checksum-f671811e8beaa0fb6eb17faf5663bd3dd0495dfa54111bad745697e074e69ad5-ChecksumEnd.splitPart2_3"));
        paths.add(tempDir.resolve("splinted-output/testfile.log.binary-Checksum-f671811e8beaa0fb6eb17faf5663bd3dd0495dfa54111bad745697e074e69ad5-ChecksumEnd.splitPart3_3"));
        return paths;
    }

    @Test
    public void test_split_with_test_file_returns_FileFragment(@TempDir Path tempDir) {
        //arrange
        File testFile = TEST_FILE_PATH.toFile();
        Path destinationFolder = tempDir.resolve("splinted-output");
        //act
        FileSplitterMerger.FileFragment result = classUnderTest.split(testFile.toPath(), KILO_BYTE, destinationFolder);
        //assert
        Assertions.assertThat(result.getCheckSum()).isEqualTo(CHECKSUM);
        Assertions.assertThat(result.getExceptions().isEmpty()).isTrue();
        Assertions.assertThat(result.getFileProcessingResult()).isEqualTo(FileSplitterMerger.FileProcessingResult.SUCCESS);
        Assertions.assertThat(result.getFragments()).containsExactlyElementsOf(getTestPathSplinted(tempDir));
    }

    @Test
    public void test_split_with_wrong_mbs_returns_failure(@TempDir Path tempDir) {
        //arrange
        File testFile = TEST_FILE_PATH.toFile();
        Path destinationFolder = tempDir.resolve("splinted-output");
        //act
        //assert
        org.junit.jupiter.api.Assertions.assertThrows(InvalidParameterException.class, () -> classUnderTest.split(testFile.toPath(), 0, destinationFolder));
    }

    @Test
    public void test_merge_with_(@TempDir Path tempDir) throws IOException {
        //arrange
        FileUtils.copyDirectory(RESOURCES_PATH.toFile(), tempDir.toFile());
        Path splintedFiles = tempDir.resolve("splinted/");
        Path destinationFile = tempDir.resolve("merged-output/" + TEST_FILE_PATH.getFileName());
        //act
        Path result = classUnderTest.merge(splintedFiles, CHECKSUM, destinationFile, false);
        //assert
        Assertions.assertThat(FileUtils.contentEquals(result.toFile(), TEST_FILE_PATH.toFile())).isTrue();
    }
}
