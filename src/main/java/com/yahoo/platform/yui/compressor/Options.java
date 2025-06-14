package com.yahoo.platform.yui.compressor;

import jargs.gnu.CmdLineParser;

import java.util.List;
import java.util.Optional;

public class Options {
    public final String TEST_MODE = "testMode";

    private boolean verbose = false;
    private boolean digest = false;
    private boolean help = false;
    private boolean munge = false;

    private String charSet;
    private String type;

    private String inputDir = ".";
    private String outputDir = ".";

    private String replacePattern;

    public Options() {
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isMunge() {
        return munge;
    }

    public String getCharSet() {
        return charSet;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String getType() {
        return type;
    }

    public String getInputDir() {
        return inputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public boolean isDigest() {
        return digest;
    }

    public boolean hasReplacePattern() {
        return replacePattern != null && replacePattern.split(":").length > 1;
    }

    public String getReplacePatternFrom() {
        return replacePattern.split(":")[0];
    }

    public String getReplacePatternTo() {
        return replacePattern.split(":")[1];
    }

    public void parseOptions(String[] args) throws CmdLineParser.IllegalOptionValueException {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option typeOpt = parser.addStringOption("type");
        CmdLineParser.Option verboseOpt = parser.addBooleanOption('v', "verbose");
        CmdLineParser.Option mungeOpt = parser.addBooleanOption('m', "munge");
        CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
        CmdLineParser.Option charsetOpt = parser.addStringOption("charset");
        CmdLineParser.Option inputDirOpt = parser.addStringOption('i', "input");
        CmdLineParser.Option outputDirOpt = parser.addStringOption('o', "output");
        CmdLineParser.Option digestOpt = parser.addBooleanOption('d', "digest");
        CmdLineParser.Option replacePatternOpt = parser.addStringOption('p', "pattern");

        try {
            parser.parse(args);
        } catch (CmdLineParser.IllegalOptionValueException e) {
            System.err.println("Option " + e.getOption() + " has illegal value " + e.getValue());
            throw new IllegalArgumentException(e);
        } catch (CmdLineParser.UnknownOptionException e) {
            System.err.println("Unknown option: " + e.getOptionName());
            throw new IllegalArgumentException(e);
        }

        verbose = Optional.ofNullable((Boolean) parser.getOptionValue(verboseOpt)).orElse(false);
        munge = Optional.ofNullable((Boolean) parser.getOptionValue(mungeOpt)).orElse(false);
        digest = Optional.ofNullable((Boolean) parser.getOptionValue(digestOpt)).orElse(false);
        help = Optional.ofNullable((Boolean) parser.getOptionValue(helpOpt)).orElse(false);
        charSet = Optional.ofNullable((String) parser.getOptionValue(charsetOpt)).orElse("UTF-8");
        type = Optional.ofNullable((String) parser.getOptionValue(typeOpt)).orElseThrow(() -> new IllegalArgumentException("Option --type is mandatory"));
        inputDir = Optional.ofNullable((String) parser.getOptionValue(inputDirOpt)).orElse(".");
        outputDir = Optional.ofNullable((String) parser.getOptionValue(outputDirOpt)).orElse(".");
        replacePattern = Optional.ofNullable((String) parser.getOptionValue(replacePatternOpt)).orElse(null);

        if (!type.equalsIgnoreCase(YUICompressor.JS) && !type.equalsIgnoreCase(YUICompressor.CSS)) {
            throw new CmdLineParser.IllegalOptionValueException(typeOpt, "Only js or css are allowed.");
        }
    }

    public static void usage() {
        System.err.println(
                "YUICompressor Version: @VERSION@\n"

                        + "\nUsage: java -jar yuicompressor-@VERSION@.jar [options] \n"
                        + "\n"
                        + "Global Options\n"
                        + "  -h, --help                Displays this information\n"
                        + "  --type <js|css>           Specifies the type of the input file\n"
                        + "  --charset <charset>       Read the input file using <charset>\n"
                        + "  -v, --verbose             Display informational messages and warnings\n"
                        + "  -d, --digest              Append a MD5 checksum (abbrev. to first 6 chars) of the file content to the filename.\n"
                        + "  -p <pattern>              Pattern to replace parts of the input filename for the output filename.\n"
                        + "                              Format: fromPatter$toPattern. See String.replace for details.\n"
                        + "                              Example: '.js:-min.js' : Replace .js with -min.js\n"
                        + "  -i <inputDir>               All files in this directory with the specified type are processed.\n"
                        + "                              All subdirectories are scanned recursively"
                        + "  -o <outputDir>              Processed files are put in this directory within the same subdir structure."
        );
    }

}

