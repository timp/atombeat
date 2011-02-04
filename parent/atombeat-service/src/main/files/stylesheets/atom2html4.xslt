<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:xhtml="http://www.w3.org/1999/xhtml" 
  xmlns="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="atom xhtml">



  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>



  <xsl:template match="/">
    
    <html>
      <head>
        <title><xsl:value-of select="atom:title"/></title>
        <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/combo?3.3.0/build/cssreset/reset-min.css&amp;3.3.0/build/cssfonts/fonts-min.css&amp;3.3.0/build/cssgrids/grids-min.css&amp;3.3.0/build/cssbase/base-min.css"/>
        <style type="text/css">
      body {
        margin: auto;
        width: 960px;
      }
      #icon {
        float: right;
      }
    </style>
      </head>
      <body>
        <div>
          <div id="icon">
            <a href="http://code.google.com/p/atombeat/"><img src="http://farm6.static.flickr.com/5051/5415906232_a26853fd64_o.png" alt="AtomBeat logo"/></a>
          </div>
          <div id="content">
            <xsl:apply-templates select="atom:feed"/>
            <xsl:apply-templates select="atom:entry"/>
          </div>
        </div>
      </body>
    </html>
    
  </xsl:template>



  <xsl:template match="atom:feed">
    <h1><xsl:apply-templates select="atom:title" mode="text-construct"/></h1>
    <p>
      ID: <xsl:value-of select="atom:id"/><br/>
      Updated: <xsl:value-of select="atom:updated"/><br/>
    </p>
    <xsl:apply-templates select="atom:summary"/>
    <p>
      Links: 
      <ul>
        <xsl:apply-templates select="atom:link" mode="links"/> 
      </ul>
    </p>
    <xsl:apply-templates select="atom:entry"/>
  </xsl:template>



  <xsl:template match="atom:entry">
    <div class="entry">
      <h2><xsl:apply-templates select="atom:title" mode="text-construct"/></h2>
      <p>
        ID: <xsl:value-of select="atom:id"/><br/>
        Published: <xsl:value-of select="atom:published"/><br/>
        Updated: <xsl:value-of select="atom:updated"/><br/>
      </p>
      <xsl:apply-templates select="atom:summary"/>
      <p>
        Links: 
        <ul>
          <xsl:apply-templates select="atom:link" mode="links"/> 
        </ul>
      </p>
      <xsl:apply-templates select="atom:content"/>
    </div>
  </xsl:template>




  <xsl:template match="atom:summary">
    <p>
      <xsl:apply-templates select="." mode="text-construct"/>
    </p>
  </xsl:template>




  <xsl:template match="atom:content">
    <xsl:choose>
      <xsl:when test="@src">
        <xsl:variable name="src" select="@src"/>
        <p>
          Content: <a href="{$src}"><xsl:value-of select="$src"/></a>
          <xsl:if test="@type">
            (<xsl:value-of select="@type"/>)
          </xsl:if>
        </p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="." mode="text-construct"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>




  <xsl:template match="atom:link" mode="links">
    <li>
      <a href="{@href}">
        <xsl:value-of select="@rel"/>
        <xsl:if test="@type">
          <xsl:text> (</xsl:text><xsl:value-of select="@type"/><xsl:text>)</xsl:text>
        </xsl:if>
      </a>
    </li>
  </xsl:template>




  <xsl:template match="*[@type='text']" mode="text-construct">
    <p>
      <xsl:value-of select="node()"/>
    </p>
  </xsl:template>



  <xsl:template match="*[@type='xhtml']" mode="text-construct">
    <xsl:apply-templates select="node()" mode="xhtml2html"/>
  </xsl:template>
  
  
  
  <xsl:template match="*" mode="xhtml2html">
    <xsl:element name="{local-name()}">
      <xsl:apply-templates select="@*|node()" mode="xhtml2html"/>
    </xsl:element>
  </xsl:template>



  <!-- omits comments and PIs -->
  <xsl:template match="@*|text()" mode="xhtml2html">
    <xsl:copy/>
  </xsl:template>
  
  
  
</xsl:stylesheet>
