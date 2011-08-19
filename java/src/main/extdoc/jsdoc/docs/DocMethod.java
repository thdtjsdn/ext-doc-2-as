package extdoc.jsdoc.docs;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 15:14:28
 */
public class DocMethod extends DocAttribute<DocMethod> {
    public List<Param> params = new ArrayList <Param>();
    public String returnType;
    public String as3ReturnType;
    public String returnDescription;

  @Override
  public void inheritFrom(DocMethod superMethod) {
    super.inheritFrom(superMethod);
    if (!hide && params.size() != superMethod.params.size()) {
      System.err.println("  Wrong override param count: method " + className + "#" + name + "(): " + params.size() + " instead of " + superMethod.params.size());
      if (params.size() > superMethod.params.size()) {
        System.err.println("*********** patching method name from " + name + " to " + name + shortClassName);
        name = name + shortClassName;
        isOverride = false;
        return;
      }
    }
    for (int i = 0, paramsSize = Math.min(params.size(), superMethod.params.size()); i < paramsSize; i++) {
      Param param = params.get(i);
      Param superParam = superMethod.params.get(i);
      if (!hide && !typesEqual(param.type, superParam.type)) {
        System.err.println("  Wrong override param type: method " + className + "#" + name + "(), param " + param.name + ", type " + param.type + " instead of " + superParam.type);
      }
    }
    params = superMethod.params;
    if (!hide && !typesEqual(returnType, superMethod.returnType)) {
      System.err.println("  Wrong override return type: method " + className + "#" + name + "(), type " + returnType + " instead of " + superMethod.returnType);
      returnType = superMethod.returnType;
    }
  }
}
