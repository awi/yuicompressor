package com.yahoo.platform.yui.compressor;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class YUICompressorTest {

    public static final String BASE_DIR_JS_FILES = "src/test/resources/scripts/";
    public static final String BASE_DIR_JS_FILES_GENERATED = "src/test/resources/scripts-gen/";
    public static final String BASE_DIR_JS_FILES_EXPCTED = "src/test/resources/scripts-exp/" +
            "";

    public final String REPLACE_PATTERN = "-min";

    private void assertGeneratedFiles() throws IOException {
        var pathToGeneratedFiles = YUICompressor.collectFiles(Path.of(BASE_DIR_JS_FILES_GENERATED), "min");

        boolean failed = false;

        for (var file : pathToGeneratedFiles) {
            var pathToExpectedFile = Path.of(BASE_DIR_JS_FILES_EXPCTED, file.getFileName().toString());

            var expected = Files.readString(pathToExpectedFile).trim();
            var generated = Files.readString(file);

            if (!expected.equals(generated)) {
                failed = true;
                System.err.println("Failed for file: " + file);
            }
        }

        if (failed) {
            fail("Failed for some files");
        }
    }

    @Test
    public void testMinification() throws IOException {

        YUICompressor.main(new String[]{"-m", "--type", "js", "-p", ".js:.js.min", "-i", BASE_DIR_JS_FILES, "-o", BASE_DIR_JS_FILES_GENERATED});
        assertGeneratedFiles();
    }

//    @Test
//    public void showUsageWhenIllegalArgumentIsGiven() throws IOException {
//        var standardErr = System.err;
//        var outputStreamCaptor = new ByteArrayOutputStream();
//
//        try {
//            System.setErr(new PrintStream(outputStreamCaptor));
//
//            YUICompressor.main(new String[]{});
//            assertTrue(outputStreamCaptor.toString().contains("Usage:"));
//        } finally {
//            System.setErr(standardErr);
//        }
//    }

    @Test
    public void showThrowIllegalArgumentExceptionWhenIllegalArgumentIsGiven() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> YUICompressor.main(new String[]{}));
    }

    @Test
    public void testFilenameGeneration() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String filename = BASE_DIR_JS_FILES + "01_prototype.js";
        String newFilename = YUICompressor.generateNewFilename(md, filename);
        assertEquals(BASE_DIR_JS_FILES + "01_prototype-6855b0.js", newFilename);
    }

    @Test
    public void collectAllJavaScriptFilesInDirectoryTree() {
        var files = YUICompressor.collectFiles(Path.of(BASE_DIR_JS_FILES + "testdir"), "js");
        assertEquals(4, files.size());
    }

    @Test
    public void throwAnExceptionWhenNoTypeIsGiven() {
        assertThrows(IllegalArgumentException.class, () -> YUICompressor.collectFiles(Path.of(BASE_DIR_JS_FILES), null));
    }
}