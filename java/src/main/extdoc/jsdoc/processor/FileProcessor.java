package extdoc.jsdoc.processor;

import extdoc.jsdoc.docs.*;
import extdoc.jsdoc.tags.*;
import extdoc.jsdoc.tags.impl.Comment;
import extdoc.jsdoc.tplschema.*;
import extdoc.jsdoc.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.Tidy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import java.util.regex.Pattern;



/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 4:41:12
 */
public class FileProcessor{

  private static final Tidy TIDY;

  static {
        TIDY = new Tidy();
        TIDY.setCharEncoding(Configuration.UTF8);
        TIDY.setAltText("");
        TIDY.setBreakBeforeBR(false);
        TIDY.setDropEmptyParas(true);
        TIDY.setDropFontTags(true);
        TIDY.setEncloseText(false);
        TIDY.setEncloseBlockText(false);
        TIDY.setFixComments(true);
        TIDY.setHideEndTags(false);
        TIDY.setIndentAttributes(false);
        TIDY.setMakeClean(true);
        TIDY.setNumEntities(true);
        TIDY.setQuiet(true);
        TIDY.setQuoteAmpersand(true);
        TIDY.setShowWarnings(false);
        TIDY.setSmartIndent(false);
        TIDY.setSpaces(0);
        TIDY.setXHTML(true);
        TIDY.setXmlOut(true);
        TIDY.setXmlSpace(false);
        TIDY.setXmlPi(false);
    }

  public static String tidy(String dirtyHtml) {
    String wrappedHtml = "<html xmlns:ext=\"http://extjs.com/ext3\"><body>"+dirtyHtml+"</body></html>";
    StringWriter result = new StringWriter();
    try {
      Document document = TIDY.parseDOM(new ByteArrayInputStream(wrappedHtml.getBytes("ISO-8859-1")), null);
      // "unpack" first paragraph:
      Node body = document.getElementsByTagName("body").item(0);
      Node firstNode = body.getFirstChild();
      if (firstNode != null && "p".equalsIgnoreCase(firstNode.getNodeName())) {
        NodeList childNodes = firstNode.getChildNodes();
        Node newFirstChild = firstNode.getNextSibling();
        for (int i = 0; i < childNodes.getLength(); i++) {
          body.insertBefore(childNodes.item(i).cloneNode(true), newFirstChild);
        }
        // Add new-line, or ASDoc will not recognize the ".":
        body.insertBefore(document.createTextNode("\n"), newFirstChild);
        body.removeChild(firstNode);
      }
      DOMSource domSource = new DOMSource(document.getDocumentElement());
      Transformer serializer = TransformerFactory.newInstance().newTransformer();
      serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      serializer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD XHTML 1.0 Transitional//EN");
      serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
      serializer.transform(domSource, new StreamResult(result));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      // should not happen for ISO-8859-1:
      throw new RuntimeException(e);
    }
    String xml = result.toString();
    if (xml.contains("<body/>")) {
      return "";
    }
    int bodyStart = xml.indexOf("<body");
    int bodyEnd = xml.indexOf("</body>");
    if(bodyEnd == -1)  {
      xml += "</body></html>";
      bodyEnd = xml.indexOf("</body>");
    }
    if (bodyStart==-1 || bodyEnd==-1) {
      // should not happen:
      throw new RuntimeException("No body element found in "+xml);
    }
    xml = xml.substring(xml.indexOf('>', bodyStart) + 1, bodyEnd);
    xml = xml.replaceAll("@", "&#64;");
    xml = xml.replaceAll("\\*", "&#42;");
    xml = xml.replaceAll(" ext:(member|cls)=\"[^\"]+\"", "");
    xml = xml.replaceAll("\r\n", "\n");
    return xml;
  }

    private final Logger logger;

    private final Handler logHandler;

    private Context context = new Context();

    private final static String OUT_FILE_EXTENSION = "as";
    private final static boolean GENERATE_DEBUG_XML = false;
    private final static String COMPONENT_NAME = "ext.Component";
    private final static String DEFAULT_TYPE = "Object";

    private static final String START_LINK = "{@link";    

    private static enum LinkStates {READ, LINK}

    private static final String
        MEMBER_REFERENCE_TPL =
            "<a href=\"output/{0}.html#{0}-{1}\">{2}</a>";

    private static final String
        CLASS_REFERENCE_TPL =
            "<a href=\"{0}.html\">{1}</a>";

    private static final int DESCR_MAX_LENGTH = 117;

    private static final String DEFAULT_MATCH = "*.js";
    private static final boolean DEFAULT_SKIPHIDDEN = true;

    private static final String ENCODING = "UTF8";

    public FileProcessor() {
        logger = Logger.getLogger("extdoc.jsdoc.processor");
        logger.setUseParentHandlers(false);
        logHandler = new ConsoleHandler();
        logHandler.setFormatter(new Formatter() {
            public String format(LogRecord record) {
                return record.getMessage() + "\n";
            }
        });
        logger.addHandler(logHandler);
    }

    public void setVerbose(){
        logger.setLevel(Level.FINE);
        logHandler.setLevel(Level.FINE);
    }

    public void setQuiet(){
        logger.setLevel(Level.OFF);
    }

    /**
     * Processes link content (between "{" and "}")
     * @param text Content, ex: "Ext.DomQuery#select"
     * @return Array of 2 Strings: long and short versions
     */
    private String[] processLink(String text) {
         StringUtils.ClsAttrName res = StringUtils.processLink(text);
         String longText, shortText;
         if (res.attr.isEmpty()) {
             // class reference
             String cls = res.cls;
             String name = res.name.isEmpty() ? res.cls : res.name;
             longText = MessageFormat.format(CLASS_REFERENCE_TPL, cls, name);
             shortText = name;
         } else {
             // attribute reference
             String cls = res.cls.isEmpty() ? context.getCurrentClass().className
                     : res.cls;
             String attr = res.attr;
             String name;
             if (res.name.isEmpty()) {
                 if (res.cls.isEmpty()) {
                     name = res.attr;
                 } else {
                     name = cls + '.' + res.attr;
                 }
             } else {
                 name = res.name;
             }
             longText = MessageFormat.format(MEMBER_REFERENCE_TPL, cls, attr,
                     name);
             shortText = name;
         }
         return new String[] { longText, shortText };
     }



    private Description inlineLinks(String content){
        return inlineLinks(content, false);
    }    

    /**
     * Replaces inline tag @link to actual html links and returns short and/or
     * long versions.
     *
     * @param cnt
     *            description content
     * @param alwaysGenerateShort
     *            forces to generate short version for Methods and events
     * @return short and long versions
     */
    private Description inlineLinks(String cnt, boolean alwaysGenerateShort) {
        if (cnt == null) {
            return null;
        }
        LinkStates state = LinkStates.READ;
        StringBuilder sbHtml = new StringBuilder();
        StringBuilder sbText = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < cnt.length(); i++) {
            char ch = cnt.charAt(i);
            switch (state) {
            case READ:
                if (StringUtils.endsWith(buffer, START_LINK)) {
                    String substr = buffer.substring(0, buffer.length()
                            - START_LINK.length());
                    sbHtml.append(substr);
                    sbText.append(substr);
                    buffer.setLength(0);
                    state = LinkStates.LINK;
                    break;
                }
                buffer.append(ch);
                break;
            case LINK:
                if (ch == '}') {
                    String[] str = processLink(buffer.toString());
                    sbHtml.append(str[0]);
                    sbText.append(str[1]);
                    buffer.setLength(0);
                    state = LinkStates.READ;
                    break;
                }
                buffer.append(ch);
                break;
            }
        }


        // append remaining
        sbHtml.append(buffer);
        sbText.append(buffer);

        String sbString = sbText.toString().replaceAll("<\\S*?>","");        

               Description description = new Description();
        description.longDescr = tidy(sbHtml.toString());
        if (alwaysGenerateShort) {
            description.hasShort = true;
            description.shortDescr = sbString.length() > DESCR_MAX_LENGTH ? new StringBuilder()
                    .append(sbString.substring(0, DESCR_MAX_LENGTH)).append(
                            "...").toString()
                    : sbString;
        } else {
            description.hasShort = sbString.length() > DESCR_MAX_LENGTH;
            description.shortDescr = description.hasShort ? new StringBuilder()
                    .append(sbString.substring(0, DESCR_MAX_LENGTH)).append(
                            "...").toString() : null;
        }
        return description;
    }  


    /**
     * Read params from list of param tags and add them to list of params Just
     * simplifies param processing for class, method and event
     *
     * @param paramTags
     *            tags
     * @param params
     *            target list of params
     */
    private void readParams(List<ParamTag> paramTags, List<Param> params) {
            Boolean optional = false;
            for (ParamTag paramTag : paramTags) {
                    Param param = new Param();
                    param.name = replaceKeyword(paramTag.getParamName());
                    param.type = paramTag.getParamType();
                    Description descr = inlineLinks(paramTag.getParamDescription());
                    param.description = descr != null ? descr.longDescr : null;
                    param.rest = paramTag.isRest();
                    // all parameters following an optional one have to be optional:
                    optional = param.optional = !param.rest && (optional || paramTag.isOptional());
                    params.add(param);
            }
    }


    private void injectCustomTags(Doc doc, Comment comment) {
        for (extdoc.jsdoc.schema.Tag customTag : context.getCustomTags()) {
            Tag tag = comment.tag('@' + customTag.getName());
            if (tag != null) {
                DocCustomTag t = new DocCustomTag();
                String title = customTag.getTitle();
                String format = customTag.getFormat();
                t.title = title;
                t.value = format != null ? MessageFormat.format(format, tag.text()) : tag.text();
                doc.customTags.add(t);
            }
        }
    }

   

    /**
     * Process class 
     * @param comment Comment
     */
    private void processClass(Comment comment){

        DocClass cls = new DocClass();
        
        ClassTag classTag = comment.tag("@class");
        Tag singletonTag = comment.tag("@singleton");
        ExtendsTag extendsTag = comment.tag("@extends");
        Tag constructorTag = comment.tag("@constructor");
        List<ParamTag> paramTags = comment.tags("@param");
        Tag namespaceTag = comment.tag("@namespace");
        Tag xtypeTag = comment.tag("@xtype");

        cls.className = classTag.getClassName();
        if (xtypeTag != null) {
          String[] parts = xtypeTag.text().split("\n", 2);
          cls.xtype = parts[0];
          if (parts.length > 1) {
            classTag.addClassDescription(parts[1]);
          }
        }
        boolean found = false;
        for (DocClass d : context.getClasses()) {
            if (d.className.equals(cls.className)) {
                context.setCurrentClass(d);
                cls = d;
                found = true;
                break;
            }
        }

        if (cls.packageName == null) {
              if (namespaceTag != null) {
                  cls.packageName = namespaceTag.text();
                  cls.shortClassName = StringUtils
                          .separateByLastDot(cls.className)[1];
              } else {
                  String[] str = StringUtils.separatePackage(cls.className);
                  cls.packageName = str[0];
                  cls.shortClassName = str[1];
              }
          }
          if (!found) {
              context.addDocClass(cls);
          }

        cls.definedIn.add(context.getCurrentFile().fileName);
        if (!cls.singleton) {
              cls.singleton = singletonTag != null;
        }
        if (cls.parentClass == null) {
            cls.parentClass = (extendsTag != null) ? extendsTag.getClassName() : null;
        }       

        // Skip private classes
        if (/*comment.hasTag("@private") ||*/ comment.hasTag("@ignore")) {
            cls.hide = true;
        }

        // process inline links after class added to context
       // DEFCT17
       if (!cls.hasConstructor) {
           cls.hasConstructor = constructorTag != null;
           if (constructorTag != null) {
               cls.constructorDescription = inlineLinks(constructorTag.text(),
                       true);
               readParams(paramTags, cls.params);
           } else if (cls.xtype != null) {
             System.err.println("generating constructor for " + cls.className);
             Description constructorDescription = new Description();
             constructorDescription.longDescr = "Create a new " + cls.shortClassName + ".";
             cls.constructorDescription = constructorDescription;
             Param param = new Param();
             param.name = "config";
             param.type = "Object";
             param.description = "The config object";
             param.optional = true;
             cls.params.add(param);
           }
       }

        if (cls.description == null) {
            String description = classTag.getClassDescription();
            if (description == null && extendsTag != null) {
                description = extendsTag.getClassDescription();
            }
            Description descr = inlineLinks(description);
            cls.description = descr != null ? descr.longDescr : null;
        }
        // Process cfg declared inside class definition
        // goes after global className set
        List<CfgTag> innerCfgs = comment.tags("@cfg");
        for (CfgTag innerCfg : innerCfgs) {
            DocCfg cfg = getDocCfg(innerCfg);
            context.addDocCfg(cfg);
        }

        injectCustomTags(cls, comment);
    }

    /**
     * Helper method to process cfg in separate comment and in class
     * definition
     * @return cfg
     */
    private DocCfg getDocCfg(CfgTag tag){
        DocCfg cfg = new DocCfg();
        cfg.name = replaceKeyword(tag.getCfgName());
        String cfgType = tag.getCfgType();
        if (cfgType != null) {
            cfg.type = cfgType;
            cfg.description = inlineLinks(tag.getCfgDescription());
            cfg.optional = tag.isOptional();
        }
        cfg.className = context.getCurrentClass().className;
        cfg.shortClassName =
                context.getCurrentClass().shortClassName;
        return cfg;
    }


    /**
     * Process cfg
     * @param comment Comment
     */
    private void processCfg(Comment comment){
        // Skip private
        if (/*comment.hasTag("@private")
                ||*/ comment.hasTag("@ignore")) return;
        CfgTag tag = comment.tag("@cfg");
        DocCfg cfg = getDocCfg(tag);
        if (cfg.type == null) {
            TypeTag typeTag = comment.tag("@type");
            if (typeTag != null) {
                cfg.type = typeTag.getType();
                cfg.description = inlineLinks(typeTag.getDescription());
            }
        }
        cfg.hide = comment.tag("@hide")!=null;
        injectCustomTags(cfg, comment);
        context.addDocCfg(cfg);
    }

    private static final String READ_ONLY = "Read-only.";

    /**
     * Process property 
     * @param comment Comment
     * @param extraLine first word form the line after comment
     */
    private void processProperty(Comment comment,String extraLine){
        // Skip private
        if (/*comment.hasTag("@private") ||*/ comment.hasTag("@ignore")) {
            return;
        }
        if (!comment.hasTag("@property") && (extraLine.equals("function") || extraLine.equals("var") || extraLine.equals("for") || extraLine.equals("if"))) {
            // wrong guess of type "property":
            System.out.println("ignoring JSDoc comment " + comment.getDescription() + "\n");
            return;
        }

        
        DocProperty property = new DocProperty();

        PropertyTag propertyTag = comment.tag("@property");
        TypeTag typeTag = comment.tag("@type");
        Tag staticTag = comment.tag("@static");
        Tag protectedTag = comment.tag("@protected");

        String name = extraLine;
        String description = comment.getDescription();
        if (propertyTag!=null){
            String propertyName = propertyTag.getPropertyName();
            if (propertyName!=null && propertyName.length()>0){
                name = propertyName;
            }
            String propertyDescription = propertyTag.getPropertyDescription();
            if (propertyDescription!=null && propertyDescription.length()>0){
                description = propertyDescription;
            }
        }
        property.name = replaceKeyword(StringUtils.separateByLastDot(name)[1]);
        if (property.name.length() == 0) {
          //System.err.println("Ignoring empty property in file " + context.getCurrentFile());
          return;
        }
        if (property.name.equals(property.name.toUpperCase())) {
          // heuristic: all-upper-case identifiers are constants
          property.isConstant = true;
          property.isReadOnly = true;
        }
        if (description != null) {
            if (description.contains(READ_ONLY)) {
                description = description.replace(READ_ONLY, "");
                property.isReadOnly = true;
            }
        }
        property.isStatic = staticTag!=null;
        property.visibility = protectedTag!=null ? "protected" : "public";
        property.type = typeTag!=null?typeTag.getType():DEFAULT_TYPE;
        property.description = inlineLinks(description);
        property.className = context.getCurrentClass().className;
        property.shortClassName = context.getCurrentClass().shortClassName;
        property.hide = comment.tag("@hide")!=null;
        injectCustomTags(property, comment);
        context.addDocProperty(property);
    }

    /**
     * Process method 
     * @param comment Comment
     * @param extraLine first word form the line after comment
     */
    private void processMethod(Comment comment, String extraLine){
        // Skip private
        if (/*comment.hasTag("@private") ||*/ comment.hasTag("@ignore")) {
            return;
        }

        if (extraLine.equals("function")) {
            return;
        }

        DocMethod method = new DocMethod();

        Tag methodTag = comment.tag("@method");
        Tag staticTag = comment.tag("@static");
        Tag protectedTag = comment.tag("@protected");
        List<ParamTag> paramTags = comment.tags("@param");
        ReturnTag returnTag = comment.tag("@return");
        MemberTag memberTag = comment.tag("@member");

        // should be first because @member may redefine class
        DocClass doc = context.getCurrentClass();
        method.className = doc!=null?doc.className:null;
        method.shortClassName = doc!=null?doc.shortClassName:null;
        String[] parts = StringUtils.separatePackage(extraLine);
        method.name = replaceKeyword(parts[1]);
        if (methodTag!=null){
            if (!methodTag.text().isEmpty()){
                method.name = replaceKeyword(methodTag.text());
            }
        }
        if (memberTag!=null){
            String name = memberTag.getMethodName();
            if (name!=null){
                method.name = replaceKeyword(name);
            }
            method.className = memberTag.getClassName();
            method.shortClassName =
                    StringUtils.separatePackage(method.className)[1];
        }
        if (method.name == null || method.name.length() == 0) {
            return;
        }
        method.isStatic = staticTag != null || memberTag!=null && memberTag.text().equals(parts[0]);
        method.visibility = protectedTag!=null ? "protected" : "public";

        // renaming if static
//        if(method.isStatic){
//            method.name = new StringBuilder()
//                    .append(shortClassName)
//                    .append('.')
//                    .append(separateByLastDot(extraLine)[1])
//                    .toString();
//        }

        method.description = inlineLinks(comment.getDescription(), true);
        if (returnTag!=null){
            String returnType = returnTag.getReturnType();
            method.returnType = "this".equals(returnType) ? method.className : returnType;
            method.returnDescription =returnTag.getReturnDescription();
            if (method.returnDescription != null) {
                method.returnDescription = inlineLinks(method.returnDescription).longDescr;
            }
        }
        readParams(paramTags, method.params);
        method.hide = comment.tag("@hide")!=null;
        injectCustomTags(method, comment);
        context.addDocMethod(method);
    }

    /**
     * Process event
     * @param comment Comment
     */
    private void processEvent(Comment comment){
        // Skip private
        if (/*comment.hasTag("@private")  ||*/ comment.hasTag("@ignore")) {
            return;
        }

        DocEvent event = new DocEvent();
        EventTag eventTag = comment.tag("@event");
        List<ParamTag> paramTags = comment.tags("@param");
        event.name = eventTag.getEventName();
        event.description = inlineLinks(eventTag.getEventDescription(), true);
        readParams(paramTags, event.params);
        event.className = context.getCurrentClass().className;
        event.shortClassName = context.getCurrentClass().shortClassName;
        event.hide = comment.tag("@hide")!=null;
        injectCustomTags(event, comment);
        context.addDocEvent(event);
    }

    enum CommentType{
        CLASS, CFG, PROPERTY, METHOD, EVENT
    }

    static CommentType resolveCommentType(Comment comment){
        return resolveCommentType(comment, "", "");
    }

    static CommentType resolveCommentType(Comment comment, String extraLine, String extra2Line){
        if(comment.hasTag("@class")){
            return CommentType.CLASS;
        }else if(comment.hasTag("@event")){
            return CommentType.EVENT;
        }else if(comment.hasTag("@cfg")){
            return CommentType.CFG;
        }else if(comment.hasTag("@param")
                || comment.hasTag("@return")
                || comment.hasTag("@method")){
            return CommentType.METHOD;
        }else if (comment.hasTag("@type")
                || comment.hasTag("@property")){
            return CommentType.PROPERTY;                    
        }else if(extra2Line.equals("function") || extra2Line.equals("Ext.emptyFn")){
            return CommentType.METHOD;
        }else{
            //System.err.println("+++++++++++++++ guessed property: extraLine: '" + extraLine + "', extra2Line: '"+extra2Line+"'");
            return CommentType.PROPERTY;
        }
    }


    /**
     *  Determine type of comment and process it
     * @param content text inside / ** and * /
     * @param extraLine first word form the line after comment 
     */
    private void processComment(String content, String extraLine, String extra2Line){
        if (content==null) return;
        Comment comment = new Comment(content);
        CommentType commentType = resolveCommentType(comment, extraLine, extra2Line);
        if (context.getCurrentClass() == null && commentType != CommentType.CLASS) {
          // ignore comments that come before a @class annotation!
          return;
        }
        switch (commentType){
            case CLASS:
                processClass(comment);
                break;
            case CFG:
                processCfg(comment);
                break;
            case PROPERTY:
                processProperty(comment, extraLine);
                break;
            case METHOD:
                processMethod(comment, extraLine);
                break;
            case EVENT:
                processEvent(comment);
                break;
        }
    }

    private enum State {CODE, COMMENT}
    private enum ExtraState {SKIP, SPACE, READ, SPACE2, READ2}

    private static final String START_COMMENT = "/**";
    private static final String END_COMMENT = "*/";


    /**
     * Checks if char is white space in terms of extra line of code after
     * comments
     * @param ch character
     * @return true if space or new line or * or / or ' etc...
     */
    private boolean isWhite(char ch){
        return !Character.isLetterOrDigit(ch) && ch!='.' && ch!='_';
    }
    /**
     * Processes one file with state machine
     *
     * @param fileName
     *            Source Code file name
     */
    private void processFile(String fileName) {
        try {
            File file = new File(new File(fileName).getAbsolutePath());
            context.setCurrentFile(file);
            context.position = 0;
            logger.fine(MessageFormat.format("Processing: {0}", context
                    .getCurrentFile().fileName));
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader
                            (new FileInputStream(file), ENCODING));
            int numRead;
            State state = State.CODE;
            ExtraState extraState = ExtraState.SKIP;
            StringBuilder buffer = new StringBuilder();
            StringBuilder extraBuffer = new StringBuilder();
            StringBuilder extra2Buffer = new StringBuilder();
            String comment = null;
            char ch;
            while ((numRead = reader.read()) != -1) {
                context.position++;
                ch = (char) numRead;
                buffer.append(ch);
                switch (state) {
                case CODE:
                    switch (extraState) {
                    case SKIP:
                        break;
                    case SPACE:
                        if (isWhite(ch)) {
                            break;
                        }
                        extraState = ExtraState.READ;
                        /* fall through */
                    case READ:
                        if (isWhite(ch)) {
                            extraState = ExtraState.SPACE2;
                            break;
                        }
                        extraBuffer.append(ch);
                        break;
                    case SPACE2:
                        if (isWhite(ch)) {
                            break;
                        }
                        extraState = ExtraState.READ2;
                        /* fall through */
                    case READ2:
                        if (isWhite(ch)) {
                            extraState = ExtraState.SKIP;
                            break;
                         }
                         extra2Buffer.append(ch);
                         break;
                     }
                     if (StringUtils.endsWith(buffer, START_COMMENT)) {
                         if (comment != null) {
                             // comment is null before the first comment starts
                             // so we do not process it
                             processComment(comment, extraBuffer.toString(),
                                     extra2Buffer.toString());
                         }
                         context.lastCommentPosition = context.position - 2;
                         extraBuffer.setLength(0);
                         extra2Buffer.setLength(0);
                         buffer.setLength(0);
                         state = State.COMMENT;
                     }
                     break;
                 case COMMENT:
                     if (StringUtils.endsWith(buffer, END_COMMENT)) {
                         comment = buffer.substring(0, buffer.length()
                                 - END_COMMENT.length());
                         buffer.setLength(0);
                         state = State.CODE;
                         extraState = ExtraState.SPACE;
                     }
                     break;
                 }
             }
             processComment(comment, extraBuffer.toString(), extra2Buffer
                     .toString());
             reader.close();
         } catch (IOException e) {
             e.printStackTrace();
         }
    }

    private void createClassHierarchy(){
        for(DocClass docClass: context.getClasses()){
            for(DocClass cls: context.getClasses()){
                if(docClass.className.equals(cls.parentClass)){
                    ClassDescr subClass = new ClassDescr();
                    subClass.className = cls.className;
                    subClass.shortClassName = cls.shortClassName;
                    docClass.subClasses.add(subClass);
                    cls.parent = docClass;
                }
            }
            for(DocCfg cfg: context.getCfgs()){
                if(docClass.className.equals(cfg.className)){
                    docClass.cfgs.add(cfg);
                }
            }
            for(DocProperty property: context.getProperties()){
                if(docClass.className.equals(property.className)){
                    docClass.properties.add(property);
                }
            }
            for(DocMethod method: context.getMethods()){
                if(docClass.className.equals(method.className)){
                    docClass.methods.add(method);
                }
            }
            for(DocEvent event: context.getEvents()){
                if(docClass.className.equals(event.className)){
                    docClass.events.add(event);
                }
            }
        }
    }

    private <T extends DocAttribute> T findOverride(T doc, List<T> docs){
        if (doc.name == null || doc.name.isEmpty())
            return null;
        String docClassName = doc.className;
        String docName = StringUtils.separateByLastDot(doc.name)[1];
        for(T attr:docs){
            if (!docClassName.equals(attr.className)) {
                String attrName = StringUtils.separateByLastDot(attr.name)[1];
                if (docName.equals(attrName)) {
                    return attr;
                }
            }
        }
        return null;
    }

    private <T extends Doc> void removeHidden
                                                                                        (List<T> docs){
        for(ListIterator<T> it = docs.listIterator(); it.hasNext();){
            if (it.next().hide)
                it.remove();
        }
    }

    private <T extends DocAttribute> void addInherited (List<T> childDocs, List<T> parentDocs){
        for(T attr: parentDocs) {
            if (!attr.isStatic) {
              T override = findOverride(attr, childDocs);
              if (override == null) {
                  childDocs.add(attr);
              } else {
                  override.inheritFrom(attr);
              }
            }
        }
    }


    private void computeAS3Names(){
        for(DocClass cls: context.getClasses()){
          cls.as3PackageName = cls.packageName.toLowerCase();
          cls.as3ShortClassName = cls.shortClassName;
          // exceptional type name mappings:
          if ("Error".equals(cls.as3ShortClassName)) {
            // naming a class like a built-in top-level class is a bad idea in AS3...
            cls.as3ShortClassName = "ExtError";
          } else if ("Ext".equals(cls.as3ShortClassName)) {
            cls.as3PackageName = "ext"; // move singleton into ext package.
          }
          cls.as3ClassName = cls.as3PackageName.length() == 0 ? cls.as3ShortClassName : cls.as3PackageName + "." + cls.as3ShortClassName;
          if (cls.singleton) {
            cls.as3SingletonName = cls.as3ClassName;
            cls.as3SingletonPackageName = cls.as3PackageName;
            cls.as3ShortSingletonName = cls.as3ShortClassName;
            if (mayBeAS3Interface(cls)) {
              cls.as3ShortClassName = "I" + cls.as3ShortClassName;
              cls.as3Type = "interface";
            } else {
              cls.as3ShortClassName += "Class";
            }
            cls.as3ClassName = cls.as3PackageName + "." + cls.as3ShortClassName;
          }
        }

        for(DocClass cls: context.getClasses()){
          // super class
          cls.as3ParentClass = cls.parent != null ? cls.parent.as3ClassName
            : cls.parentClass != null ? asAS3Type(cls.parentClass)
            : null;
          // suppress explicit "extends Object" (breaks interfaces!)
          if ("Object".equals(cls.as3ParentClass)) {
            cls.as3ParentClass = null;
            cls.parentClass = null;
            cls.parent = null;
          }

          if (isEmptySingletonType(cls)) {
            // move singleton without additional properties or methods to super class:
            cls.parent.as3SingletonName = cls.as3SingletonName;
            cls.parent.as3SingletonPackageName = cls.as3SingletonPackageName;
            cls.parent.as3ShortSingletonName = cls.as3ShortSingletonName;
          }

          // config class
          boolean hasCfgs = !cls.cfgs.isEmpty() || cls.xtype != null;

          if (hasCfgs) {
            cls.cfgPackageName = "ext.config";
              // set config class name etc.:
            cls.cfgShortClassName = cls.xtype != null ? cls.xtype : cls.shortClassName.toLowerCase();
            cls.cfgClassName = cls.cfgPackageName + "." + cls.cfgShortClassName;
          }

          // events
          for (DocEvent event : cls.events) {
            for (Param param : event.params) {
              param.as3Type = asAS3Type(param.type);
            }
          }
          // constructor
          for (Param param : cls.params) {
            param.as3Type = hasCfgs && "config".equals(param.name)
              ? cls.cfgClassName // tweak config parameter's type
              : asAS3Type(param.type);
          }
          // properties
          for (DocProperty property : cls.properties) {
            property.as3Type = asAS3Type(property.type);
          }
          // methods
          for (DocMethod method : cls.methods) {
            for (Param param : method.params) {
              param.as3Type = asAS3Type(param.type);
            }
            method.as3ReturnType = asAS3Type(method.returnType);
          }
          // cfg properties
          for (DocCfg cfg : cls.cfgs) {
            cfg.as3Type = asAS3Type(cfg.type);
          }

        }

        // Now that we know all config classes, determine config super class:
        for(DocClass cls: context.getClasses()){
          if (cls.cfgClassName != null) {
            DocClass parentClass = cls.parent;
            while (parentClass != null) {
              if (parentClass.cfgClassName != null) {
                cls.cfgParentClass = parentClass.cfgClassName;
                break;
              }
              parentClass = parentClass.parent;
            }
          }
        }

    }

  private boolean isEmptySingletonType(DocClass cls) {
    return cls.singleton && cls.parent != null && cls.properties.isEmpty() && cls.methods.isEmpty();
  }

  private boolean mayBeAS3Interface(DocClass cls) {
    return (cls.parent == null || "Object".equals(cls.parentClass));
  }

  private void setImports(){
        for(DocClass cls: context.getClasses()){
            setImports(cls);
        }
    }

  private void setImports(DocClass cls) {
    // super class
    addImport(cls, cls.as3ParentClass);
    // config class
    addImport(cls, cls.cfgClassName);
    // events
    for (DocEvent event : cls.events) {
      for (Param param : event.params) {
        addImport(cls, param.as3Type);
      }
    }
    // constructor
    for (Param param : cls.params) {
      addImport(cls, param.as3Type);
    }
    // properties
    for (DocProperty property : cls.properties) {
      addImport(cls, property.as3Type);
    }
    // methods
    for (DocMethod method : cls.methods) {
      for (Param param : method.params) {
        addImport(cls, param.as3Type);
      }
      addImport(cls, method.as3ReturnType);
    }

    if (cls.cfgClassName != null) {
      // super class
      addCfgImport(cls, cls.cfgParentClass);
      // cfg properties
      for (DocCfg cfg : cls.cfgs) {
        if (cfg.type != null) {
          addCfgImport(cls, cfg.as3Type);
        }
      }
    }
  }

  private void addImport(DocClass cls, String className) {
    addImport(cls.imports, cls.as3PackageName, className);
  }

  private void addCfgImport(DocClass cls, String cfgClassName) {
    addImport(cls.cfgImports, cls.cfgPackageName, cfgClassName);
  }

  private void addImport(Set<ClassDescr> imports, String currentPackage, String importClassName) {
      if (importClassName == null) {
        return;
      }
      String[] parts = StringUtils.separateByLastDot(importClassName);
      String packageName = parts[0];
      String shortClassName = parts[1];
      if (packageName.length() > 0 && !currentPackage.equals(packageName)) {
        ClassDescr classDescr = new ClassDescr();
        classDescr.shortClassName = shortClassName;
        classDescr.className = packageName + "." + classDescr.shortClassName;
        imports.add(classDescr);
      }
    }

    private void injectInherited(){
        for(DocClass cls: context.getClasses()){
            DocClass parent = cls.parent;
            while(parent!=null){
                ClassDescr superClass = new ClassDescr();
                superClass.className = parent.className;
                superClass.shortClassName = parent.shortClassName;
                cls.superClasses.add(superClass);
                if (parent.className.equals(COMPONENT_NAME)){
                    cls.component = true;
                }
                addInherited(cls.cfgs, parent.cfgs);
                addInherited(cls.properties, parent.properties);
                addInherited(cls.methods, parent.methods);
                addInherited(cls.events, parent.events);
                parent = parent.parent;
            }
            removeHidden(cls.cfgs);
            removeHidden(cls.properties);
            removeHidden(cls.methods);
            removeHidden(cls.events);

            // sorting
            Collections.sort(cls.cfgs);
            Collections.sort(cls.properties);
            Collections.sort(cls.methods);
            Collections.sort(cls.events);

            setSuperCallParams(cls);
            // *** experimental: add all cfgs as properties, if not already present:
            addCfgsToProperties(cls);
            
            Collections.reverse(cls.superClasses);
            Collections.sort(cls.subClasses);

        }
        removeHidden(context.getClasses());
    }

  private void addCfgsToProperties(DocClass cls) {
    for (DocCfg docCfg : cls.cfgs) {
      if (!"Function".equals(docCfg.type)) {
        DocProperty cfgProperty = new DocProperty();
        if (!containsAttributeWithSameName(cls.properties, docCfg) && !containsAttributeWithSameName(cls.methods, docCfg)) {
          // if no property of that name exists, create one from the cfg:
          cfgProperty.name = docCfg.name;
          cfgProperty.type = docCfg.type;
          cfgProperty.isReadOnly = true;
          cfgProperty.isOverride = docCfg.isOverride;
          cfgProperty.description = docCfg.description;
          cfgProperty.className = docCfg.className;
          cfgProperty.shortClassName = docCfg.shortClassName;
          cfgProperty.as3Type = docCfg.as3Type;
          cfgProperty.visibility = "public";
          cls.properties.add(cfgProperty);
        }
      }
    }
  }

  private static boolean containsAttributeWithSameName(List<? extends DocAttribute> properties, DocAttribute attribute) {
    String name = attribute.name;
    String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    for (DocAttribute d : properties) {
      if (d.name.equals(name)) {
        //System.err.println("Class " + d.className + ": @cfg " + name + " already exists as property.");
        return true;
      }
      if (d instanceof DocMethod) {
        DocMethod m = (DocMethod)d;
        if (m.params.size() == 0 && !"void".equals(m.as3ReturnType) && getterName.equals(m.name)) {
          System.err.println("Class " + d.className + ": @cfg " + attribute + " not converted to getter, because method " + getterName + "() is present.");
          return true;
        }
      }
    }
    return false;
  }

  private void setSuperCallParams(DocClass cls) {
    if (cls.parent != null) {
      List<Param> params = cls.parent.params;
      StringBuilder builder = new StringBuilder();
      for (Param param : params) {
        if (builder.length() > 0) {
          builder.append(", ");
        }
        builder.append("Number".equalsIgnoreCase(param.type) ? "undefined" : "Boolean".equalsIgnoreCase(param.type) ? "false" : "null");
      }
      cls.superCallParams = builder.toString();
    }
  }

  private void createPackageHierarchy(){
        for(DocClass cls: context.getClasses()){
            context.addClassToTree(cls);
        }
        context.sortTree();
    }

    private void showStatistics(){
        logger.fine("*** STATISTICS ***") ;
        for (Map.Entry<String, Integer> e : Comment.allTags.entrySet()){
            logger.fine(e.getKey() + ": " + e.getValue());
        }
    }

    private Pattern filePattern 
            = Pattern.compile(StringUtils.wildcardToRegex(DEFAULT_MATCH));
    private boolean skipHidden = DEFAULT_SKIPHIDDEN;

    private void processDir(String dirName){
        File file = new File(dirName);
        if (file.exists()){
            if (!(skipHidden && file.isHidden())){
                if (file.isDirectory()){
                    String[] children = file.list();
                    for(String child : children){
                        processDir(dirName+File.separator+child);
                    }
                }else{
                    if(filePattern.matcher(file.getName()).matches()){
                        processFile(dirName);
                    }
                }
            }
        }else{
            // file not exists
            logger.warning(
                    MessageFormat.format("File {0} not found", dirName));
        }
    }

    public void process(String fileName, String[] extraSrc){
        try {

            // process project file
            if(fileName!=null){
                File xmlFile = new File(new File(fileName).getAbsolutePath());
                FileInputStream fileInputStream = new FileInputStream(xmlFile);
                JAXBContext jaxbContext =
                        JAXBContext.newInstance("extdoc.jsdoc.schema");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                extdoc.jsdoc.schema.Doc doc =
                        (extdoc.jsdoc.schema.Doc) unmarshaller.
                                unmarshal(fileInputStream);
                extdoc.jsdoc.schema.Tags tags = doc.getTags();
                if (tags!=null){
                    context.setCustomTags(doc.getTags().getTag());
                }
                extdoc.jsdoc.schema.Sources srcs = doc.getSources();
                if (srcs!=null){
                    List<extdoc.jsdoc.schema.Source> sources = srcs.getSource();
                    if(sources!=null){
                        for(extdoc.jsdoc.schema.Source src: sources){
                            String m = src.getMatch();
                            Boolean sh = src.isSkipHidden();
                            skipHidden = sh!=null?sh:DEFAULT_SKIPHIDDEN;
                            filePattern = Pattern.compile(
                                    StringUtils.wildcardToRegex(
                                            m!=null?m:DEFAULT_MATCH)); 
                            processDir(xmlFile.getParent()+
                                    File.separator+
                                    src.getSrc());
                        }
                    }
                }
                fileInputStream.close();
            }
            
            // process source files from command line
            if(extraSrc!=null){
                for(String src : extraSrc){
                    processDir(src);
                }
            }

            showStatistics();
            createClassHierarchy();
            injectInherited();
            createPackageHierarchy();
            computeAS3Names();
            setImports();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


     private void copyDirectory(File sourceLocation , File targetLocation)
        throws IOException {

        // skip hidden
        if (sourceLocation.isHidden()) return;
         
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs();
            }

            String[] children = sourceLocation.list();
            for (String child : children) {
                copyDirectory(new File(sourceLocation, child),
                        new File(targetLocation, child));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    private static final String WRAPPER_CODE_MARKER =
            "###SOURCE###";

    private void readWrapper(String wrapper, StringBuilder prefix,
                             StringBuilder suffix){
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader
                            (new FileInputStream(wrapper), ENCODING));
            int numRead;
            while((numRead=reader.read())!=-1 &&
                    !StringUtils.endsWith(prefix, WRAPPER_CODE_MARKER)){
                prefix.append((char)numRead);
            }
            int len = prefix.length();
            prefix.delete(len-WRAPPER_CODE_MARKER.length(),len);
            suffix.append((char)numRead);
            while((numRead=reader.read())!=-1){
                suffix.append((char)numRead);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void copySourceFiles(String targetDir, String wrapper) {
        new File(targetDir).mkdirs();
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        readWrapper(wrapper, prefix, suffix);
        for (DocFile docFile : context.getDocFiles()) {
            try {
                File dst = new File(new StringBuilder().append(targetDir)
                        .append(File.separator).append(docFile.targetFileName)
                        .toString());
                StringBuilder buffer = new StringBuilder();
               BufferedReader reader =
                    new BufferedReader(new InputStreamReader
                            (new FileInputStream(docFile.file), ENCODING));
                // current character
                int numRead;
                // position in file
                int position = 0;
                // current doc
                ListIterator<Doc> it = docFile.docs.listIterator();
                Doc doc = it.hasNext() ? it.next() : null;
                buffer.append(prefix);
                while ((numRead = reader.read()) != -1) {
                    position++;
                    char ch = (char) numRead;
                    if (doc != null && position == doc.positionInFile) {
                        buffer.append(MessageFormat.format(
                                "<div id=\"{0}\"></div>", doc.id));
                        doc = it.hasNext() ? it.next() : null;
                    }
                    buffer.append(ch);
                }
                buffer.append(suffix);
                Writer out =
                        new BufferedWriter(new OutputStreamWriter
                                (new FileOutputStream(dst), ENCODING));
                out.write(buffer.toString());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void saveToFolder(String folderName, String templateFileName){
        new File(folderName).mkdirs();
        try {

            File templateFile =
                    new File(new File(templateFileName).getAbsolutePath());
            String templateFolder = templateFile.getParent();

            // Read template.xml
            JAXBContext jaxbTplContext =
                    JAXBContext.newInstance("extdoc.jsdoc.tplschema");
            Unmarshaller unmarshaller = jaxbTplContext.createUnmarshaller();
            Template template = (Template) unmarshaller.
                        unmarshal(new FileInputStream(templateFile));
            ClassTemplate classTemplate = template.getClassTemplate();
            String classTplFileName = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(classTemplate.getTpl())
                    .toString();
            String classTplTargetDir = new StringBuilder()
                    .append(folderName)
                    .append(File.separator)
                    .append(classTemplate.getTargetDir())
                    .toString();
            ConfigClassTemplate configClassTemplate = template.getConfigClassTemplate();
            String configTplFileName = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(configClassTemplate.getTpl())
                    .toString();
            SingletonTemplate singletonTemplate = template.getSingletonTemplate();
            String singletonTplFileName = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(singletonTemplate.getTpl())
                    .toString();

            logger.info("*** COPY RESOURCES ***") ;
            new File(classTplTargetDir).mkdirs();

            // Copy resources
            Resources resources = template.getResources();

            List<Copy> dirs = resources.getCopy();

            for(Copy dir : dirs){
                String src = new StringBuilder()
                    .append(templateFolder)
                    .append(File.separator)
                    .append(dir.getSrc())
                    .toString();
                String dst = new StringBuilder()
                    .append(folderName)
                    .append(File.separator)
                    .append(dir.getDst())
                    .toString();
                copyDirectory(new File(src), new File(dst));
            }


            logger.info("*** COPY SOURCE FILES ***");
            String sourceTargetDir = new StringBuilder()
                    .append(folderName)
                    .append(File.separator)
                    .append(template.getSource().getTargetDir())
                    .toString();
             logger.info(MessageFormat.format("Target folder: {0}",
                     sourceTargetDir));
            String wrapperFile = templateFolder + File.separator +
                    template.getSource().getWrapper(); 
            copySourceFiles(sourceTargetDir, wrapperFile);



            // Marshall and transform classes
            JAXBContext jaxbContext =
                    JAXBContext.newInstance("extdoc.jsdoc.docs");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT,
                    true
            );
            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);

            TransformerFactory factory = TransformerFactory.newInstance();
            Templates transformation = 
                    factory
                            .newTemplates (new StreamSource(classTplFileName)) ;
            Transformer transformer = transformation.newTransformer();

            Templates configTransformation = 
                    factory
                            .newTemplates (new StreamSource(configTplFileName)) ;
            Transformer configTransformer = configTransformation.newTransformer();

            Templates singletonTransformation = 
                    factory
                            .newTemplates (new StreamSource(singletonTplFileName)) ;
            Transformer singletonTransformer = singletonTransformation.newTransformer();

            DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();

            logger.info("*** SAVING FILES ***") ;
            for(DocClass docClass: context.getClasses()){
              if (!isEmptySingletonType(docClass)) {
                generateClass(classTplTargetDir, marshaller, transformer, docBuilder, docClass, docClass.as3ClassName);
                if (docClass.as3SingletonName != null) {
                  generateClass(classTplTargetDir, marshaller, singletonTransformer, docBuilder, docClass, docClass.as3SingletonName);
                }
                if (docClass.cfgClassName != null) {
                  generateClass(classTplTargetDir, marshaller, configTransformer, docBuilder, docClass, docClass.cfgClassName);
                }
              }
            }

        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  private void generateClass(String classTplTargetDir, Marshaller marshaller, Transformer transformer, DocumentBuilder docBuilder, DocClass docClass, String className) throws JAXBException, TransformerException {
    logger.fine("Saving: " + className);
    String targetFileName = new StringBuilder()
            .append(classTplTargetDir)
            .append(File.separator)
            .append(className.replace('.', File.separatorChar))
            .append('.')
            .append(OUT_FILE_EXTENSION)
            .toString();
    Document doc = docBuilder.newDocument();
    marshaller.marshal(docClass, doc);
    File targetFile = new File(targetFileName);
    targetFile.getParentFile().mkdirs();
    if (GENERATE_DEBUG_XML){
        marshaller.marshal(docClass, new File(targetFileName+"_"));
    }
    Result fileResult = new StreamResult(targetFile);
    transformer.transform(new DOMSource(doc), fileResult);
    transformer.reset();
  }

  public static String replaceKeyword(String ide) {
        if (ide == null) {
            return null;
        }
        // correct wrong Ext documentation, where parameters are sometimes directly followed by a dot:
        if (ide.endsWith(".")) {
            ide = ide.substring(0, ide.length()-1);
        }
        // rename keyword-named identifiers by postfixing with "_":
        return "package".equals(ide) || "class".equals(ide) || "is".equals(ide) || "as".equals(ide) || "this".equals(ide)
            ? ide + "_"
            : ide;
    }

    private static final String[] BUILT_IN_TYPES = new String[]{"Number", "String", "Boolean", "Object", "RegExp"};
    private static final String[] JS_TYPES = new String[]{"HTMLElement", "Node", "Event"};

    public String asAS3Type(String type) {
      if (type == null) {
        return "void";
      }
      DocClass docClass = context.resolveClass(type);
      if (docClass != null) {
        return docClass.as3ClassName;
      }
      if (type.contains("|") || type.contains("/") || "mixed".equalsIgnoreCase(type) || "null".equals(type)) {
        return "*";
      }
      if ("Hash".equals(type) || "StyleSheet".equals(type) || "CSSRule".equals(type)) {
        return "Object";
      }
      if ("DOMElement".equals(type) || "XMLElement".equals(type)) {
        return "js.Element";
      }
      if ("XMLDocument".equals(type)) {
        return "js.Document";
      }
      if ("HtmlNode".equals(type)) {
        return "js.Node";
      }
      if ("float".equalsIgnoreCase(type)) {
        return "Number";
      }
      if ("integer".equalsIgnoreCase(type)) {
        return "int";
      }
      if ("Constructor".equals(type)) {
        return "Class";
      }
      for (String builtInType : BUILT_IN_TYPES) {
        if (builtInType.equalsIgnoreCase(type)) {
          return builtInType;
        }
      }
      for (String jsType : JS_TYPES) {
        if (jsType.equalsIgnoreCase(type)) {
          return "js." + jsType;
        }
      }
      if (type.endsWith("[]") || type.charAt(0) == '[' && type.charAt(type.length()-1) == ']') {
        return "Array";
      }
      if ("Layout".equals(type)) {
        return "ext.layout.ContainerLayout";
      }
      if (type.charAt(0) == '(' && type.charAt(type.length()-1) == ')') {
        type = type.substring(1, type.length() - 1);
      }
      if (type.endsWith(".")) {
        type = type.substring(0, type.length() - 1);
      }
      String[] parts = StringUtils.separateByLastDot(type);
      return (parts[0].length() > 0 ? parts[0].toLowerCase() + "." : "" ) + parts[1];
    }

}
