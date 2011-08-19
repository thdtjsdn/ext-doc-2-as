package extdoc.jsdoc.docs;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: Andrey Zubkov
 * Date: 25.10.2008
 * Time: 5:16:41
 */

@XmlRootElement
public class DocClass extends Doc{
    public String className;
    public String shortClassName;
    public String packageName;
    public List<String> definedIn = new ArrayList<String>();
    public boolean singleton;
    public String as3ClassName;
    public String as3ShortClassName;
    public String as3PackageName;
    public String as3SingletonName;
    public String as3ShortSingletonName;
    public String as3SingletonPackageName;
    public String description;
    public String parentClass;
    public String as3ParentClass;
    public boolean hasConstructor;
    public Description constructorDescription;
    public List<Param> params = new ArrayList<Param>();
    public List<DocCfg> cfgs = new ArrayList<DocCfg>();
    public List<DocProperty> properties = new ArrayList<DocProperty>();
    public List<DocMethod> methods = new ArrayList<DocMethod>();
    public List<DocEvent> events = new ArrayList<DocEvent>();
    public List<ClassDescr> subClasses = new ArrayList<ClassDescr>();
    public List<ClassDescr> superClasses = new ArrayList<ClassDescr>();
    public String cfgClassName;
    public String cfgShortClassName;
    public String cfgPackageName;
    public String cfgParentClass;
    public Set<ClassDescr> imports = new TreeSet<ClassDescr>();
    public Set<ClassDescr> cfgImports = new TreeSet<ClassDescr>();
    public String superCallParams = "";
    @XmlTransient
    public DocClass parent = null;
    public boolean component = false;
    public String xtype = null;
    public String as3Type = "class";
}
