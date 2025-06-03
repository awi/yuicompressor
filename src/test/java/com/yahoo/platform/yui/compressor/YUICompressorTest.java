package com.yahoo.platform.yui.compressor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YUICompressorTest {

    private String readFile(String filename) throws IOException {
        return Files.readString(Path.of(filename)).trim();
    }

    @ParameterizedTest
    @ValueSource(strings = {"_munge.js", "_string_combo.js", "_string_combo2.js", "_string_combo3.js",
            "_syntax_error.js", "float.js", "issue86.js", "jquery-1.6.4.js", "promise-catch-finally-issue203.js"})
    public void testCase(String filename) throws IOException {
        var file = "src/test/resources/" + filename;
        var result = YUICompressor.main(new String[]{"-t", file});
        assertEquals(readFile(file + ".min"), result, filename);
    }
}