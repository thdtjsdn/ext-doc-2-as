package extdoc;

import extdoc.jsdoc.processor.FileProcessor;
import org.apache.commons.cli.*;

/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 2:16:18
 */

public class Main {

    private static Options options = new Options();

    private static void wrongCli(String msg){
        System.err.println("Wrong command line arguments: "+ msg);
        showHelp();
    }

    private static void showHelp(){
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java -jar ext-doc.jar", options);
    }

    public static void main(String[] args) {

        options = new Options();

        Option project = OptionBuilder.withArgName("project")
                .hasArg()
                .withDescription("Project XML file.")
                .withLongOpt("project")
                .create('p');


        Option output = OptionBuilder.withArgName("output")
                .hasArg()
                .withDescription("Directory where documentation should be created.")
                .isRequired()
                .withLongOpt("output")
                .create('o');

        Option template = OptionBuilder.withArgName("template")
                .hasArg()
                .withDescription("XML File containing template informaiton")
                .isRequired()
                .withLongOpt("template")
                .create('t');

        Option source = OptionBuilder.withArgName("source")
                .hasArg()
                .withDescription("Source files")
                .hasOptionalArgs()
                .withLongOpt("source")
                .create('s');

        options.addOption(project);
        options.addOption(output);
        options.addOption(template);
        options.addOption(source);

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse( options, args);
            if(cmd.hasOption("project")||cmd.hasOption("source")){
                FileProcessor processor = new FileProcessor();
                processor.process(
                        cmd.getOptionValue("project"),
                        cmd.getOptionValues("source")
                );
                processor.saveToFolder(
                    cmd.getOptionValue("output"),
                    cmd.getOptionValue("template"));
            }else{
                wrongCli("Project XML file or source files should be specified");    
            }
        } catch (ParseException e) {
            wrongCli(e.getMessage());
        }
    }
}
