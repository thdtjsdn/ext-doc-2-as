package extdoc.jsdoc.docs;

/**
 * User: Andrey Zubkov
 * Date: 03.11.2008
 * Time: 2:23:50
 */
public abstract class DocAttribute extends Doc
                                        implements Comparable<DocAttribute>{
    public String name;
    public Description description;
    public String className;
    public String shortClassName;
    public boolean isStatic; 

    public  int compareTo(DocAttribute anotherAttribute) {
        return name.compareTo(anotherAttribute.name);
    }

}
