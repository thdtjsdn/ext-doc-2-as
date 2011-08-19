package extdoc.jsdoc.tags.impl;

import extdoc.jsdoc.tags.ParamTag;

/**
 * User: Andrey Zubkov
 * Date: 31.10.2008
 * Time: 1:04:07
 */
class ParamTagImpl extends TagImpl implements ParamTag {

    private String paramType;
    private String paramName;
    private String paramDescription;
    private boolean optional;
    private boolean rest;

    public ParamTagImpl(String name, String text) {
        super(name, text);
        String[] str = divideAtWhite(text, 3);
        paramType = removeBrackets(str[0]);
        paramName = removeBrackets(str[1]);
        // Ext JS documentation quirks: sometimes, type is completely missing, sometimes, param name comes first:
        if (paramType.equals(str[0])) { // no brackets: first str is the name!
          if (paramName.equals(str[1])) { // no brackets on second str either?
            // add non-bracketed word to description:
            paramDescription = paramName + " " + paramDescription;
            paramType = "*";
          } else { // second str is the type!
            paramType = paramName;
          }
          paramName = str[0];
        }
        optional = isOptional(str[2]);
        paramDescription = optional?cutOptional(str[2]):str[2];
        if (paramName != null && paramName.endsWith("...") || paramType != null && paramType.indexOf("...") != -1) {
          if (paramName.endsWith("...")) {
            paramName = paramName.substring(0, paramName.length() - 3);
          }
          paramType = "Array";
          rest = true;
        }
    }

    public String getParamType() {
        return paramType;
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamDescription() {
        return paramDescription;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isRest() {
        return rest;
    }
}
