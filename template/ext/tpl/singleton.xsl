<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY nbsp "&#160;">
]>        
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ext="http://www.extjs.com">
<xsl:output method="text" indent="no"/>
<xsl:template match="/docClass">package <xsl:value-of select="as3SingletonPackageName"/> {
<xsl:if test="as3SingletonPackageName!=as3PackageName">import <xsl:value-of select="as3ClassName"/>;
</xsl:if>

/**
 * <xsl:value-of select="description" disable-output-escaping="yes"/>
 * @see <xsl:value-of select="as3ClassName"/>
 * @see http://dev.sencha.com/deploy/ext-3.3.1/docs/source/<xsl:value-of select="href"/> Ext JS source
 */
public const <xsl:value-of select="as3ShortSingletonName"/>:<xsl:value-of select="as3ShortClassName"/>;

}
</xsl:template>
</xsl:stylesheet>
