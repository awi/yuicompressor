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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class YUICompressor {

    public static final int LENGTH_OF_DIGEST = 6;

    /**
     * The entrypoint if called from external (eg. CLI or Maven)
     *
     * @param args the argument to YUICompressor as defined in method usage
     */
    public static void main(String args[]) {

        try {
            mainInternal(args);
        } catch (IllegalArgumentException e) {
            usage();
        }
    }

    /**
     * This is used by the test cases and returns the minified
     * js or css, if the parameter -t is passed.
     *
     * @param args the argument to YUICompressor as defined in method usage
     * @return the minified file, if -t is passed via args
     */
    static String mainInternal(String args[]) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option typeOpt = parser.addStringOption("type");
        CmdLineParser.Option versionOpt = parser.addBooleanOption('V', "version");
        CmdLineParser.Option verboseOpt = parser.addBooleanOption('v', "verbose");
        CmdLineParser.Option nomungeOpt = parser.addBooleanOption("nomunge");
        CmdLineParser.Option linebreakOpt = parser.addStringOption("line-break");
        CmdLineParser.Option preserveSemiOpt = parser.addBooleanOption("preserve-semi");
        CmdLineParser.Option disableOptimizationsOpt = parser.addBooleanOption("disable-optimizations");
        CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
        CmdLineParser.Option charsetOpt = parser.addStringOption("charset");
        CmdLineParser.Option outputFilenameOpt = parser.addStringOption('o', "output");
        CmdLineParser.Option mungemapFilenameOpt = parser.addStringOption('m', "mungemap");
        CmdLineParser.Option preserveUnknownHintsOpt = parser.addBooleanOption('p', "preservehints");
        CmdLineParser.Option testModeOpt = parser.addBooleanOption('t', "test");
        CmdLineParser.Option digestOpt = parser.addBooleanOption('d', "digest");

        Boolean testMode = false;
        Boolean addDigest = false;
        MessageDigest digest = getMessageDigest();
        Reader in = null;
        Writer out = null;
        Writer mungemap = null;

        try {

            parser.parse(args);

            testMode = (Boolean) parser.getOptionValue(testModeOpt);
            if (testMode == null) {
                testMode = false;
            }

            addDigest = (Boolean) parser.getOptionValue(digestOpt);
            if (addDigest == null) {
                addDigest = false;
            }

            Boolean help = (Boolean) parser.getOptionValue(helpOpt);
            if (help != null && help.booleanValue()) {
                usage();
                System.exit(0);
            }

            Boolean version = (Boolean) parser.getOptionValue(versionOpt);
            if (version != null && version.booleanValue()) {
                version();
                System.exit(0);
            }

            boolean verbose = parser.getOptionValue(verboseOpt) != null;

            String charset = (String) parser.getOptionValue(charsetOpt);
            if (charset == null || !Charset.isSupported(charset)) {
                // charset = System.getProperty("file.encoding");
                // if (charset == null) {
                //     charset = "UTF-8";
                // }

                // UTF-8 seems to be a better choice than what the system is reporting
                charset = "UTF-8";


                if (verbose) {
                    System.err.println("\n[INFO] Using charset " + charset);
                }
            }

            int linebreakpos = -1;
            String linebreakstr = (String) parser.getOptionValue(linebreakOpt);
            if (linebreakstr != null) {
                try {
                    linebreakpos = Integer.parseInt(linebreakstr, 10);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal value for option --line-break");
                }
            }

            String typeOverride = (String) parser.getOptionValue(typeOpt);
            if (typeOverride != null && !typeOverride.equalsIgnoreCase("js") && !typeOverride.equalsIgnoreCase("css")) {
                throw new IllegalArgumentException("Missing or illegal value for --type");
            }

            boolean munge = parser.getOptionValue(nomungeOpt) == null;
            boolean preserveAllSemiColons = parser.getOptionValue(preserveSemiOpt) != null;
            boolean disableOptimizations = parser.getOptionValue(disableOptimizationsOpt) != null;
            boolean preserveUnknownHints = parser.getOptionValue(preserveUnknownHintsOpt) != null;

            String[] fileArgs = parser.getRemainingArgs();
            List<String> files = Arrays.asList(fileArgs);
            if (files.isEmpty()) {
                if (typeOverride == null) {
                    throw new IllegalArgumentException("Must specify --type when no input file is given (use stdin as input)");
                }
                files = new java.util.ArrayList<String>();
                files.add("-"); // read from stdin
            } else if (files.size() == 1) {
                // Check whether the only filename is a directory and collect all files
                // with the given typeOverride in the directory and all subdirs.
                Path path = Paths.get(files.get(0));
                if (Files.isDirectory(path)) {
                    files = collectFiles(path, typeOverride);
                }
            }

            // '.css$:-min.css'
            String output = (String) parser.getOptionValue(outputFilenameOpt);
            String pattern[];
            if (output == null) {
                pattern = new String[0];
            } else if (output.matches("(?i)^[a-z]\\:\\\\.*")) { // if output is with something like c:\ dont split it
                pattern = new String[]{output};
            } else {
                pattern = output.split(":");
            }

            try {
                String mungemapFilename = (String) parser.getOptionValue(mungemapFilenameOpt);
                if (mungemapFilename != null) {
                    mungemap = new OutputStreamWriter(new FileOutputStream(mungemapFilename), charset);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            java.util.Iterator filenames = files.iterator();
            while (filenames.hasNext()) {
                String inputFilename = (String) filenames.next();
                String outputFilename = null;
                String type = null;

                try {
                    if (inputFilename.equals("-")) {
                        in = new InputStreamReader(System.in, charset);
                        type = typeOverride;
                    } else {
                        if (typeOverride != null) {
                            type = typeOverride;
                        } else {
                            int idx = inputFilename.lastIndexOf('.');
                            if (idx >= 0 && idx < inputFilename.length() - 1) {
                                type = inputFilename.substring(idx + 1);
                            }
                        }

                        if (type == null || !type.equalsIgnoreCase("js") && !type.equalsIgnoreCase("css")) {
                            usage();
                            System.exit(1);
                        }

                        in = new InputStreamReader(new FileInputStream(inputFilename), charset);
                    }

                    outputFilename = output;
                    // if a substitution pattern was passed in
                    if (pattern.length > 1 && files.size() > 0) {
                        outputFilename = inputFilename.replaceFirst(pattern[0], pattern[1]);
                    }

                    if (type.equalsIgnoreCase("js")) {
                        final String localFilename = inputFilename;

                        JavaScriptCompressor compressor = new JavaScriptCompressor(in, new YUIErrorReporter(localFilename));

                        // Close the input stream first, and then open the output stream,
                        // in case the output file should override the input file.
                        in.close();
                        in = null;

                        if (outputFilename == null) {
                            if (testMode) {
                                out = new StringWriter();
                            } else {
                                out = new OutputStreamWriter(System.out, charset);
                            }
                        } else {
                            out = new OutputStreamWriter(new FileOutputStream(outputFilename), charset);
                            if (mungemap != null) {
                                mungemap.write("\n\nFile: " + outputFilename + "\n\n");
                            }
                        }

                        compressor.compress(out, mungemap, linebreakpos, munge, verbose,
                                preserveAllSemiColons, disableOptimizations, preserveUnknownHints);

                    } else if (type.equalsIgnoreCase("css")) {

                        CssCompressor compressor = new CssCompressor(in);

                        // Close the input stream first, and then open the output stream,
                        // in case the output file should override the input file.
                        in.close();
                        in = null;

                        if (outputFilename == null) {
                            out = new OutputStreamWriter(System.out, charset);
                        } else {
                            out = new OutputStreamWriter(new FileOutputStream(outputFilename), charset);
                        }

                        compressor.compress(out, linebreakpos);
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

                if (addDigest) {
                    var newFilename = generateNewFilename(digest, outputFilename);
                    new File(outputFilename).renameTo(new File(newFilename));
                }
            }
        } catch (CmdLineParser.OptionException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (mungemap != null) {
                try {
                    mungemap.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (testMode) {
            return out.toString();
        } else {
            return null;
        }
    }

    static List<String> collectFiles(Path dir, String type) {
        if (type == null) {
            throw new IllegalArgumentException("ERROR: No type override specified when using a directory as input");
        }

        Iterator<Path> iter;
        try {
            iter = Files.newDirectoryStream(dir).iterator();

            FileSystem fs = dir.getFileSystem();
            final PathMatcher matcher = fs.getPathMatcher("glob:" + "*." + type);
            DirectoryStream.Filter<Path> filter = entry -> matcher.matches(entry.getFileName());

            var result = new ArrayList<String>();

            while (iter.hasNext()) {
                var next = iter.next();
                if (Files.isDirectory(next)) {
                    result.addAll(collectFiles(next, type));
                } else {
                    if (filter.accept(next)) {
//                        var fn = Path.of(dir.toString(),next.toString());
                        result.add(next.toString());
                    }
                }
            }
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

    private static void version() {
        System.err.println("@VERSION@");
    }

    private static void usage() {
        System.err.println(
                "YUICompressor Version: @VERSION@\n"

                        + "\nUsage: java -jar yuicompressor-@VERSION@.jar [options] [input file]\n"
                        + "\n"
                        + "Global Options\n"
                        + "  -V, --version             Print version information\n"
                        + "  -h, --help                Displays this information\n"
                        + "  --type <js|css>           Specifies the type of the input file\n"
                        + "  --charset <charset>       Read the input file using <charset>\n"
                        + "  --line-break <column>     Insert a line break after the specified column number\n"
                        + "  -v, --verbose             Display informational messages and warnings\n"
                        + "  -p, --preservehints       Don't elide unrecognized compiler hints (e.g. \"use strict\", \"use asm\")\n"
                        + "  -m <file>                 Place a mapping of munged identifiers to originals in this file\n\n"
                        + "  -d, --digest              Append a MD5 checksum (abbrev. to first 6 chars) of the file content to the filename"
                        + "  -o <file>                 Place the output into <file>. Defaults to stdout.\n"
                        + "                            Multiple files can be processed using the following syntax:\n"
                        + "                            java -jar yuicompressor.jar -o '.css$:-min.css' *.css\n"
                        + "                            java -jar yuicompressor.jar -o '.js$:-min.js' *.js\n\n"

                        + "JavaScript Options\n"
                        + "  --nomunge                 Minify only, do not obfuscate\n"
                        + "  --preserve-semi           Preserve all semicolons\n"
                        + "  --disable-optimizations   Disable all micro optimizations\n\n"

                        + "If no input file is specified, it defaults to stdin. In this case, the 'type'\n"
                        + "option is required. Otherwise, the 'type' option is required only if the input\n"
                        + "file extension is neither 'js' nor 'css'.");
    }
}
