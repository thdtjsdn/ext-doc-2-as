package extdoc.jsdoc.docs;

/**
 * User: Andrey Zubkov
 * Date: 03.11.2008
 * Time: 2:23:50
 */
public abstract class DocAttribute<T extends DocAttribute>  extends Doc
                                        implements Comparable<T>{
    public String name;
    public Description description;
    public String className;
    public String shortClassName;
    public boolean isStatic; 
    public boolean isOverride;
    public String visibility;

    public  int compareTo(T anotherAttribute) {
        // name may be null or anotherAttribute may be null
        // safe comparison
        return (name!=null && 
                anotherAttribute!=null &&
                anotherAttribute.name!=null)?
                name.compareTo(anotherAttribute.name):0;
    }

    public void inheritFrom(T superAttribute) {
      isOverride = true;
      visibility = superAttribute.visibility;
    }

    protected static boolean typesEqual(String type1, String type2) {
      return type1 == null ? type2 == null : type1.equals(type2);
    }
}
