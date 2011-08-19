package extdoc.jsdoc.docs;

/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 15:14:17
 */
public class DocProperty extends DocAttribute<DocProperty> {
    public String type;
    public String as3Type;
    public boolean isConstant;
    public boolean isReadOnly;

  @Override
  public void inheritFrom(DocProperty superAttribute) {
    super.inheritFrom(superAttribute);
    if (!typesEqual(type, superAttribute.type)) {
      if (!hide) {
        System.err.println("  Wrong override type: property " + className + "#" + name);
      }
      type = superAttribute.type;
    }
  }
}
