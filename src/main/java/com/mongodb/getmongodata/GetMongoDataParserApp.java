package com.mongodb.getmongodata;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class GetMongoDataParserApp {
    
    private static Options options;

    @SuppressWarnings("static-access")
    private static CommandLine initializeAndParseCommandLineOptions(String[] args) {
        options = new Options();
        options.addOption(new Option("help", "print this message"));
        options.addOption(OptionBuilder.withArgName("getMongoData log file").hasArgs().withLongOpt("file")
                .isRequired(true).create("f"));
        options.addOption(OptionBuilder.withArgName("output file").hasArg().withLongOpt("output")
                .isRequired(false).create("o"));
        options.addOption(OptionBuilder.withArgName("connection uri (to store output)").hasArg().withLongOpt("uri")
                .isRequired(false).create("c"));
        options.addOption(OptionBuilder.withArgName("stats field prefix").hasArg().withLongOpt("prefix")
                .isRequired(false).create("p"));
        options.addOption(OptionBuilder.withArgName("namespace exclusion filter").hasArgs().withLongOpt("exclude")
                .isRequired(false).create("x"));
        options.addOption(OptionBuilder.withArgName("skip writing sample documents").withLongOpt("nosample")
                .isRequired(false).create("n"));

        CommandLineParser parser = new GnuParser();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
            if (line.hasOption("help")) {
                printHelpAndExit(options);
            }
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            printHelpAndExit(options);
        } catch (Exception e) {
            e.printStackTrace();
            printHelpAndExit(options);
        }

        return line;
    }

    private static void printHelpAndExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("logParser", options);
        System.exit(-1);
    }

    public static void main(String[] args) throws Exception {
        CommandLine line = initializeAndParseCommandLineOptions(args);
        //String filename = (String)line.getOptionValue("f");
        String[] fileNames = line.getOptionValues("f");
        System.out.println("fileNames: " + fileNames);
        File[] files = new File[fileNames.length];
        int i = 0;
        for (String fn : fileNames) {
            files[i++] = new File(fn);
        }
        String output = line.getOptionValue("o");
        String connectionUri = line.getOptionValue("c");
        String prefix = line.getOptionValue("p");
        String[] filters = line.getOptionValues("x");
        boolean skipSamples = line.hasOption("n");
        if (fileNames.length > 0 && fileNames[0].endsWith(".json")) {
        	GetMongoDataParserJson parser = new GetMongoDataParserJson(files[0]);
            parser.parse();
        	
        } else {
        	GetMongoDataParser parser = new GetMongoDataParser(files, output, connectionUri, filters, skipSamples, prefix);
            parser.parse();
        }
        
    }

}
