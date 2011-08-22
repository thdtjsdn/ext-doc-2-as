<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY nbsp "&#160;">
]>        
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ext="http://www.extjs.com">
<xsl:output method="text" indent="no"/>
<xsl:template match="/docClass">
package <xsl:value-of select="cfgPackageName"/> {
<xsl:for-each select="cfgImports">
import <xsl:value-of select="className"/>;
</xsl:for-each>
<xsl:if test="not(cfgParentClass)">
import joo.JavaScriptObject;</xsl:if>

/**
 * <xsl:value-of select="description" disable-output-escaping="yes"/>
 *
 * &lt;p>This class <xsl:if test="xtype">represents the xtype '<xsl:value-of select="xtype"/>' and </xsl:if>serves as a
 * typed config object for constructor of class <xsl:value-of select="shortClassName"/>.&lt;/p>
 *
 * &lt;p>Copyright &amp;#169; 2011 Sencha Inc.&lt;/p>
 * @see <xsl:value-of select="as3ClassName"/>
 */
[ExtConfig(target="<xsl:value-of select="as3ClassName"/>"<xsl:if test="xtype">, xtype="<xsl:value-of select="xtype"/>"</xsl:if>)]
public class <xsl:value-of select="cfgShortClassName"/> extends <xsl:choose><xsl:when test="cfgParentClass"><xsl:value-of select="cfgParentClass"/></xsl:when><xsl:otherwise>joo.JavaScriptObject</xsl:otherwise></xsl:choose> {

  public function <xsl:value-of select="cfgShortClassName"/>(config:Object = null) {

    super(config);
  }

  <!--Configs-->
  <xsl:if test="cfgs">
    <xsl:for-each select="cfgs">
      <xsl:if test="/docClass/className=className">
  /**
<xsl:value-of select="description/longDescr" disable-output-escaping="yes"/>
   */      
  <xsl:if test="isOverride='true'">override </xsl:if>public native function get <xsl:value-of select="name"/>():<xsl:value-of select="as3Type"/>;

  /**
   * @private
   */
   <xsl:if test="isOverride='true'">override </xsl:if>public native function set <xsl:value-of select="name"/>(value:<xsl:value-of select="as3Type"/>):void;
      </xsl:if>
    </xsl:for-each>
  </xsl:if>

}
}
    </xsl:template>

</xsl:stylesheet>
