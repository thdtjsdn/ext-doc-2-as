package extdoc;

import extdoc.jsdoc.processor.FileProcessor;

/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 2:16:18
 */

public class Main {
    public static void main(String[] args) {
        String xmlFileName = args[0];
        String outputFolderName = args[1];
        String templateFileName = args[2];
        FileProcessor processor = new FileProcessor();
        processor.process(xmlFileName);
        processor.saveToFolder(outputFolderName, templateFileName);
    }
}
