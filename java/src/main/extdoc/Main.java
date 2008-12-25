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
        options.addOption("h","help", false, "Show detailed help.");        
        options.addOption("p","project", true, "Project XML file.");
        options.addOption("o","output", true, "Directory where documentation should be created.");
        options.addOption("t","template", true, "XML File containing template informaiton.");

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse( options, args);
            if(cmd.hasOption("help")){
                showHelp();
            }else if(cmd.hasOption("project") &&
                    cmd.hasOption("output") &&
                    cmd.hasOption("template")){
                FileProcessor processor = new FileProcessor();
                processor.process(cmd.getOptionValue("project"));
                processor.saveToFolder(cmd.getOptionValue("output"),
                        cmd.getOptionValue("template"));
            }else{
                wrongCli("required arguments missing");                                
            }
        } catch (ParseException e) {
            wrongCli(e.getMessage());
        }
    }
}
