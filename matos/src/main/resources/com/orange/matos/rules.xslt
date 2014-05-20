<?xml version="1.0" ?>
<!-- This transformation transform a pair "set of properties","generic template" into a 
    specialized result where 'variables' in the template are substituted with properties.
    The stylesheet defines a small language with :
      - if/then
      - switch/case
      - loops over indexed properties.
  -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:key name="result" match="property" use="@key"/>

    <xsl:template match="/">
		<xsl:for-each select="root/output">
	  		<xsl:apply-templates mode="reading">
	      		<xsl:with-param name="suf"/>
	  		</xsl:apply-templates>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="prop" mode="reading">
		<xsl:param name="suf"/>
		<xsl:choose>
			<xsl:when test="@rel='true'">
        		<xsl:value-of select="key('result',concat(@name,$suf))/@value"/>
			</xsl:when>
        	<xsl:otherwise>
	  			<xsl:value-of select="key('result',@name)/@value"/>
	        </xsl:otherwise>
		</xsl:choose>
    </xsl:template>

    <xsl:template match="if" mode="reading">
		<xsl:param name="suf"/>
		<xsl:variable name="v">
			<xsl:choose>
				<xsl:when test="@rel='true'">
        			<xsl:value-of select="key('result',concat(@name,$suf))/@value"/>
				</xsl:when>
        		<xsl:otherwise>
	  				<xsl:value-of select="key('result',@name)/@value"/>
	        	</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
        <xsl:choose>
          <xsl:when test="$v='true'">
	    	<xsl:apply-templates select="then/*|then/node()" mode="reading">
	      		<xsl:with-param name="suf" select="$suf"/>
	    	</xsl:apply-templates>
          </xsl:when>
          <xsl:when test="$v='false'">
	    	<xsl:apply-templates select="else/*|else/node()" mode="reading">
	      		<xsl:with-param name="suf" select="$suf"/>
	    	</xsl:apply-templates>
          </xsl:when>
    	</xsl:choose>
    </xsl:template>

    <xsl:template match="switch" mode="reading">
		<xsl:param name="suf"/>
		<xsl:variable name="v">
			<xsl:choose>
				<xsl:when test="@rel='true'">
        			<xsl:value-of select="key('result',concat(@name,$suf))/@value"/>
				</xsl:when>
        		<xsl:otherwise>
	  				<xsl:value-of select="key('result',@name)/@value"/>
	        	</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:apply-templates select="case[@value=$v]/*|case[@value=$v]/node()" mode="reading">
	  		<xsl:with-param name="suf" select="$suf"/>
		</xsl:apply-templates>
    </xsl:template>

    <xsl:template match="loop" mode="reading">
	  <xsl:param name="suf"/>
          <xsl:call-template name="looper">
	      <xsl:with-param name="i" select="0"/>
	      <xsl:with-param name="n" select="key('result',@index)/@value"/>
	      <xsl:with-param name="body" select="*"/>
	      <xsl:with-param name="suf" select="$suf"/>
          </xsl:call-template>        
    </xsl:template>

    <xsl:template match="@* | node()" mode="reading">
        <xsl:param name="suf"/>
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" mode="reading">
	   			<xsl:with-param name="suf" select="$suf"/>
	    	</xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="looper">
		<xsl:param name="i"/>
   		<xsl:param name="n"/>
       	<xsl:param name="body"/>
       	<xsl:param name="suf"/>
       	<xsl:if test="$i &lt; $n">
	  		<xsl:apply-templates select="$body" mode="reading">
	    		<xsl:with-param name="suf" select="concat($suf,'.',$i)"/>
	  		</xsl:apply-templates>
      		<xsl:call-template name="looper">
	      		<xsl:with-param name="i" select="$i+1"/>
	      		<xsl:with-param name="n" select="$n"/>
	      		<xsl:with-param name="body" select="$body"/>
	      		<xsl:with-param name="suf" select="$suf"/>
        	</xsl:call-template>
       	</xsl:if>
    </xsl:template>
</xsl:stylesheet>
