package com.yahoo.platform.yui.compressor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class YUICompressorTest {

    public static final String SRC_TEST_RESOURCES = "src/test/resources/";

    private String readFile(String filename) throws IOException {
        return Files.readString(Path.of(filename)).trim();
    }

    @ParameterizedTest
    @ValueSource(strings = {"01_prototype.js", "_munge.js", "_string_combo.js", "_string_combo2.js", "_string_combo3.js",
            "_syntax_error.js", "float.js", "issue86.js", "jquery-1.6.4.js", "promise-catch-finally-issue203.js"})
    public void testCase(String filename) throws IOException {
        var file = SRC_TEST_RESOURCES + filename;
        var result = YUICompressor.mainInternal(new String[]{"-t", file});
        assertEquals(readFile(file + ".min"), result, filename);
    }

    @Test
    public void showUsageWhenIllegalArgumentIsGiven() throws IOException {
        var standardErr = System.err;
        var outputStreamCaptor = new ByteArrayOutputStream();

        try {
            System.setErr(new PrintStream(outputStreamCaptor));

            YUICompressor.main(new String[]{});
            assertTrue(outputStreamCaptor.toString().contains("Usage:"));
        } finally {
            System.setErr(standardErr);
        }
    }

    @Test
    public void showThrowIllegalArgumentExceptionWhenIllegalArgumentIsGiven() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> YUICompressor.mainInternal(new String[]{}));
    }

    @Test
    public void testFilenameGeneration() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String filename = SRC_TEST_RESOURCES + "01_prototype.js";
        String newFilename = YUICompressor.generateNewFilename(md, filename);
        assertEquals(SRC_TEST_RESOURCES + "01_prototype-6855b0.js", newFilename);
    }

    @Test
    public void collectAllJavaScriptFilesInDirectoryTree() {
        var files = YUICompressor.collectFiles(Path.of(SRC_TEST_RESOURCES + "testdir"), "js");
        assertEquals(4, files.size());
    }

    @Test
    public void throwAnExceptionWhenNoTypeIsGiven() {
        assertThrows(IllegalArgumentException.class, () -> YUICompressor.collectFiles(Path.of(SRC_TEST_RESOURCES), null));
    }
}