package extdoc.jsdoc.tags.impl;

import extdoc.jsdoc.tags.Tag;

import java.util.regex.Pattern;

/**
 * User: Andrey Zubkov
 * Date: 30.10.2008
 * Time: 23:33:11
 */
class TagImpl implements Tag {

    private String name;

    private String text;

    public TagImpl(String name, String text) {
        this.name = name;
        this.text = text;
    }

    public String name() {
        return name;
    }

    public String text() {
        return text;
    }

    String[] divideAtWhite(String text, int parts) {
        String[] str = new String[parts];
        int c = 0;
        int start = 0;
        boolean skipWhite = true;
        for(int i=0;i<text.length();i++){
            char ch = text.charAt(i);
            boolean isWhite = Character.isWhitespace(ch);
            if (isWhite){
                if(!skipWhite){
                    str[c] = text.substring(start, i);
                    start = i;
                    c++;
                }
                start++;                
            }else if(c >= parts-1){
                break;
            }
            skipWhite = isWhite;
        }
        str[c] = text.substring(start, text.length());
        return str;
    }

    String removeBrackets(String text){
        if (text==null) return text;
        int len = text.length();
        int start = text.charAt(0)=='{'?1:0;
        int end = text.charAt(len-1)=='}'?len-1:len;
        return text.substring(start, end);
    }

    private static final String[] OPTIONAL = new String[]{"(Optional)", "(optional)", "Optional."};

    boolean isOptional(String text){
      if (text != null) {
        for (String anOPTIONAL : OPTIONAL) {
          if (text.startsWith(anOPTIONAL)) {
            return true;
          }
        }
      }
      return false;
    }

    String cutOptional(String text){
      if (text != null) {
        for (String anOPTIONAL : OPTIONAL) {
          if (text.startsWith(anOPTIONAL)) {
            return text.substring(anOPTIONAL.length()).trim();
          }
        }
      }
      return text;
    }

}
