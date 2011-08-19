package extdoc.jsdoc.tags.impl;

import extdoc.jsdoc.tags.TypeTag;

/**
 * User: Andrey Zubkov
 * Date: 01.11.2008
 * Time: 2:07:54
 */
class TypeTagImpl extends TagImpl implements TypeTag {

    private String type;
    private String description;

    public TypeTagImpl(String name, String text) {
        super(name, text);
        String[] str = divideAtWhite(text, 2);
        type = removeBrackets(str[0]);
        // Ext JS documentation bug: sometime, the property description is placed *after* the @type annotation
        description = str[1];
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
