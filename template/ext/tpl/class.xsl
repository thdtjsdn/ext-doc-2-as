<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY nbsp "&#160;">
]>        
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="text" indent="no"/>
<xsl:template match="/docClass">package <xsl:value-of select="as3PackageName"/> {
<xsl:for-each select="imports">import <xsl:value-of select="className"/>;
</xsl:for-each>

  <!--Events-->
  <xsl:choose>
      <xsl:when test="events">
                <xsl:for-each select="events">
                  <xsl:if test="/docClass/className=className">
  /**
   * <xsl:value-of select="description/longDescr" disable-output-escaping="yes"/>
   * Listeners will be called with the following arguments:
   * &lt;ul>
                    <xsl:if test="count(params)=0">
   *       &lt;li>None.&lt;/li>
   *   </xsl:if>
                    <xsl:for-each select="params">
   *       &lt;li>
   *           &lt;code><xsl:value-of select="name"/>:<xsl:choose><xsl:when test="as3Type='*'">&amp;#42;</xsl:when><xsl:otherwise><xsl:value-of select="as3Type"/></xsl:otherwise></xsl:choose>&lt;/code>
                      <xsl:value-of select="description" disable-output-escaping="yes"/>
   *       &lt;/li>
                    </xsl:for-each>
   * &lt;/ul>
   */
   [Event(name="<xsl:value-of select="name"/>")]
                  </xsl:if>
                </xsl:for-each>
      </xsl:when>
  </xsl:choose>

/**
 * <xsl:value-of select="description" disable-output-escaping="yes"/>
 *
 * &lt;p>Copyright &amp;#169; 2011 Sencha Inc.&lt;/p>
 *
<xsl:if test="xtype">
 * &lt;p>This component is created by the xtype '<xsl:value-of select="xtype"/>' / the EXML element &amp;lt;<xsl:value-of select="xtype"/>>.&lt;/p></xsl:if>
  <xsl:if test="as3SingletonName">
 * &lt;p>This <xsl:value-of select="as3Type"/> defines the type of the singleton <xsl:value-of select="as3ShortSingletonName"/>.&lt;/p>
 * @see <xsl:value-of select="as3SingletonPackageName"/>.#<xsl:value-of select="as3ShortSingletonName"/><xsl:text> </xsl:text><xsl:value-of select="as3SingletonName"/></xsl:if>
<xsl:if test="cfgClassName">
 * @see <xsl:value-of select="cfgClassName"/></xsl:if>
 * @see http://dev.sencha.com/deploy/ext-3.3.1/docs/source/<xsl:value-of select="href"/> Ext JS source
 */
public <xsl:value-of select="as3Type"/><xsl:text> </xsl:text><xsl:value-of select="as3ShortClassName"/><xsl:if test="parentClass"> extends <xsl:value-of select="as3ParentClass"/></xsl:if> {
<xsl:if test="as3Type='class'">
  /**<xsl:choose>
<xsl:when test="singleton='true'">
   * @private</xsl:when><xsl:otherwise>
   * <xsl:value-of select="constructorDescription/longDescr" disable-output-escaping="yes"/></xsl:otherwise></xsl:choose>
   * <xsl:call-template name="method-params-details"/>
<xsl:if test="cfgClassName">
   * @see <xsl:value-of select="cfgClassName"/></xsl:if>
   */
  public function <xsl:value-of select="as3ShortClassName"/><xsl:call-template name="method-params"/> {
    super(<xsl:value-of select="superCallParams"/>);
  }
</xsl:if>

  <!--Properties-->
  <xsl:if test="properties">
    <xsl:for-each select="properties">
      <xsl:if test="/docClass/className=className">
  /**
        <xsl:if test="hide='true'">
   * @private
        </xsl:if>
<xsl:value-of select="description/longDescr" disable-output-escaping="yes"/>
   * @see http://dev.sencha.com/deploy/ext-3.3.1/docs/source/<xsl:value-of select="href"/> Ext JS source
   */
<xsl:choose><xsl:when test="isConstant='true' and /docClass/as3Type='class'">
  <xsl:value-of select="visibility"/> <xsl:call-template name="check-if-static"/> const <xsl:value-of select="name"/>:<xsl:value-of select="as3Type"/>;
</xsl:when>
<xsl:otherwise>
  <xsl:if test="isOverride='true'">override </xsl:if><xsl:if test="/docClass/as3Type='class'"><xsl:value-of select="visibility"/><xsl:call-template name="check-if-static"/> native </xsl:if>function get <xsl:value-of select="name"/>():<xsl:value-of select="as3Type"/>;
<xsl:if test="isReadOnly='false'">
  /**
   * @private
   */
  <xsl:if test="isOverride='true'">override </xsl:if><xsl:if test="/docClass/as3Type='class'"><xsl:value-of select="visibility"/><xsl:call-template name="check-if-static"/> native </xsl:if>function set <xsl:value-of select="name"/>(value:<xsl:value-of select="as3Type"/>):void;
</xsl:if>
</xsl:otherwise>
</xsl:choose>
      </xsl:if>
    </xsl:for-each>
  </xsl:if>

  <!--Methods-->
  <xsl:if test="methods">
    <xsl:for-each select="methods">
      <xsl:if test="/docClass/className=className">
  /**
   * <xsl:value-of select="description/longDescr" disable-output-escaping="yes"/>
   * <xsl:call-template name="method-params-details"/>
   * @see http://dev.sencha.com/deploy/ext-3.3.1/docs/source/<xsl:value-of select="href"/> Ext JS source
   */
   <xsl:if test="isOverride='true'">override </xsl:if><xsl:if test="/docClass/as3Type='class'"><xsl:value-of select="visibility"/><xsl:call-template name="check-if-static"/> native </xsl:if>function <xsl:value-of select="name"/>
        <xsl:call-template name="method-params"/>:<xsl:value-of select="as3ReturnType"/>;
      </xsl:if>
    </xsl:for-each>
  </xsl:if>
}
}
    </xsl:template>

    <!-- Method Parameters in short-->
    <xsl:template name="method-params">(<xsl:for-each select="params">
      <xsl:if test="rest='true'">...</xsl:if>
      <xsl:value-of select="name"/>:<xsl:value-of select="as3Type"/>
      <xsl:if test="optional='true'"> = <xsl:choose>
          <xsl:when test="as3Type='Number'">undefined</xsl:when>
          <xsl:when test="as3Type='Boolean'">false</xsl:when>
          <xsl:otherwise>null</xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <xsl:if test="position()!=last()">, </xsl:if>
    </xsl:for-each>)
    </xsl:template>

    <!-- Method Parameters processing-->
    <xsl:template name="method-params-details">
      <xsl:for-each select="params">
   * @param <xsl:value-of select="name"/><xsl:text> </xsl:text><xsl:value-of select="description" disable-output-escaping="yes"/>
      </xsl:for-each>
      <xsl:if test="as3ReturnType!='void'">
   * @return <xsl:value-of select="returnDescription"/>
      </xsl:if>
    </xsl:template>

    <!-- Shows <static> if item is static-->
    <xsl:template name="check-if-static">
        <xsl:if test="isStatic='true' and not(/docClass/as3SingletonName)"> static</xsl:if>    
    </xsl:template>

    <!-- Recursive template generates "n" number of space elements -->
    <xsl:template name="spacer">        
        <xsl:param name="n"/>
        <xsl:if test="$n&gt;0">
            <xsl:text>&nbsp;&nbsp;</xsl:text>
            <xsl:call-template name="spacer">
                <xsl:with-param name="n" select="$n - 1"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
