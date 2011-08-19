package extdoc.jsdoc.docs;

/**
 * User: Andrey Zubkov
 * Date: 27.10.2008
 * Time: 23:03:55
 */
public class DocCfg extends DocAttribute<DocCfg> {
    public String type;
    public String as3Type;
    public boolean optional;

  @Override
  public void inheritFrom(DocCfg superAttribute) {
    super.inheritFrom(superAttribute);
    if (!typesEqual(type, superAttribute.type)) {
      if (!hide) {
        System.err.println("  Wrong override type: cfg " + className + "#" + name);
      }
      type = superAttribute.type;
    }
  }
}
