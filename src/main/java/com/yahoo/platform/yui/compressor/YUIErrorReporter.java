package com.yahoo.platform.yui.compressor;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

public class YUIErrorReporter implements ErrorReporter {

    private final String localFilename;

    public YUIErrorReporter(String localFilename) {
        this.localFilename = localFilename;
    }

    public void warning(String message, String sourceName,
                        int line, String lineSource, int lineOffset) {
        System.err.println("\n[WARNING] in " + localFilename);
        if (line < 0) {
            System.err.println("  " + message);
        } else {
            System.err.println("  " + line + ':' + lineOffset + ':' + message);
        }
    }

    public void error(String message, String sourceName,
                      int line, String lineSource, int lineOffset) {
        System.err.println("[ERROR] in " + localFilename);
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
