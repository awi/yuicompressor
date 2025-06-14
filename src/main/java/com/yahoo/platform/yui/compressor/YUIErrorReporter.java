package com.yahoo.platform.yui.compressor;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import java.nio.file.Path;

public class YUIErrorReporter implements ErrorReporter {

    private final Path inputFile;

    public YUIErrorReporter(Path inputFile) {
        this.inputFile = inputFile;
    }

    public void warning(String message, String sourceName,
                        int line, String lineSource, int lineOffset) {
        System.err.println("\n[WARNING] in " + inputFile);
        if (line < 0) {
            System.err.println("  " + message);
        } else {
            System.err.println("  " + line + ':' + lineOffset + ':' + message);
        }
    }

    public void error(String message, String sourceName,
                      int line, String lineSource, int lineOffset) {
        System.err.println("[ERROR] in " + inputFile);
        if (line < 0) {
            System.err.println("  " + message);
        } else {
            System.err.println("  " + line + ':' + lineOffset + ':' + message);
        }
    }

    public EvaluatorException runtimeError(String message, String sourceName,
                                           int line, String lineSource, int lineOffset) {
        error(message, sourceName, line, lineSource, lineOffset);
        return new EvaluatorException(message);
    }


}
