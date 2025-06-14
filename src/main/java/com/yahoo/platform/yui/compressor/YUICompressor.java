/*
 * YUI Compressor
 * http://developer.yahoo.com/yui/compressor/
 * Author: Julien Lecomte -  http://www.julienlecomte.net/
 * Copyright (c) 2011 Yahoo! Inc.  All rights reserved.
 * The copyrights embodied in the content of this file are licensed
 * by Yahoo! Inc. under the BSD (revised) open source license.
 */
package com.yahoo.platform.yui.compressor;

import jargs.gnu.CmdLineParser;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class YUICompressor {

    public static final int LENGTH_OF_DIGEST = 6;
    public static final String CSS = "css";
    public static final String JS = "js";

    /**
     * The entrypoint if called from external (eg. CLI or Maven)
     *
     * @param args the argument to YUICompressor as defined in method usage
     */
    public static void main(String args[]) {

        MessageDigest digest = getMessageDigest();
        Reader in = null;
        Writer out = null;


        var options = new Options();
        try {
            options.parseOptions(args);
        } catch (CmdLineParser.IllegalOptionValueException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

        if (options.isHelp()) {
            Options.usage();
            System.exit(0);
        }

        if (options.isVerbose()) {
            System.err.println("\n[INFO] Using charset " + options.getCharSet());
        }

        try {
            FileUtils.deleteDirectory(Path.of(options.getOutputDir()).toFile());
        } catch (IOException e) {
            throw new RuntimeException("Cannot delete output directory " + options.getOutputDir(), e);
        }

        var type = options.getType();
        List<Path> files = collectFiles(Path.of(options.getInputDir()), type);

        for (Path inputFilename : files) {
            String outputFilename = null;

            try {
                in = new InputStreamReader(new FileInputStream(inputFilename.toString()), options.getCharSet());

                // Get the path relative to the inputDir
                var relFilename = Path.of(options.getInputDir()).relativize(inputFilename);

                // Add this relative path to the outputDir
                outputFilename = Path.of(options.getOutputDir(), relFilename.toString()).toString();

                if (options.hasReplacePattern()) {
                    outputFilename = outputFilename.replaceFirst(options.getReplacePatternFrom(), options.getReplacePatternTo());
                }

                if (type.equalsIgnoreCase(JS)) {

                    JavaScriptCompressor compressor = new JavaScriptCompressor(in, new YUIErrorReporter(inputFilename));

                    // Close the input stream first, and then open the output stream,
                    // in case the output file should override the input file.
                    in.close();
                    in = null;

                    out = createOutputFile(outputFilename, options);

                    compressor.compress(out, null, -1, options.isMunge(), options.isVerbose(),
                            false, true, false);

                } else if (type.equalsIgnoreCase(CSS)) {

                    CssCompressor compressor = new CssCompressor(in);

                    // Close the input stream first, and then open the output stream,
                    // in case the output file should override the input file.
                    in.close();
                    in = null;

                    out = createOutputFile(outputFilename, options);

                    compressor.compress(out, -1);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (options.isDigest()) {
                var newFilename = generateNewFilename(digest, outputFilename);
                new File(outputFilename).renameTo(new File(newFilename));
            }
        }
    }

    private static OutputStreamWriter createOutputFile(String outputFilename, Options options) throws UnsupportedEncodingException, FileNotFoundException {
        Path.of(outputFilename).getParent().toFile().mkdirs();
        return new OutputStreamWriter(new FileOutputStream(outputFilename), options.getCharSet());
    }

    static List<Path> collectFiles(Path dir, String type) {
        if (type == null) {
            throw new IllegalArgumentException("ERROR: No type override specified when using a directory as input");
        }

        try {
            var result = new ArrayList<Path>();
            Files.walkFileTree(dir, new ResourceFileVisitor(type, result));
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String generateNewFilename(MessageDigest digest, String outputFilename) {
        digest.reset();

        byte[] ba = null;
        try {
            ba = digest.digest(Files.readString(Paths.get(outputFilename)).getBytes());
        } catch (IOException e) {
            System.err.println("Cannot read file " + outputFilename);
            System.exit(1);
        }

        // Convert to hex string
        var sb = new StringBuilder();

        for (byte b : ba) {
            sb.append(String.format("%02x", b));
        }

        // Add to filename bfore extension
        var idxExtension = outputFilename.lastIndexOf('.');

        return outputFilename.substring(0, idxExtension) + "-" + sb.substring(0, LENGTH_OF_DIGEST) + outputFilename.substring(idxExtension);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("MessageDigest MD5 not found!");
            System.exit(1);
        }
        return null;
    }


}
