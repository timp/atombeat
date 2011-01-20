<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:a="http://www.w3.org/2005/Atom"
  xmlns:xhtml="http://www.w3.org/1999/xhtml" 
  exclude-result-prefixes="a xhtml">

  <xsl:output method="html" version="4.01" encoding="iso-8859-1"
              doctype-public="-//W3C//DTD HTML 4.01//EN"
              doctype-system="http://www.w3.org/TR/html4/strict.dtd"/>

  <xsl:template match="*"/><!-- Ignore unknown elements -->
  <xsl:template match="*" mode="links"/>
  <xsl:template match="*" mode="categories"/>

  <xsl:template match="/a:feed">
<html>
  <head>
    <title><xsl:value-of select="a:title"/></title>
  </head>
  <body>
    <h1><xsl:apply-templates select="a:title" mode="text-construct"/></h1>
    <p>Feed ID: <xsl:value-of select="a:id"/></p>
    <p>Feed updated: <xsl:value-of select="a:updated"/></p>
    <div class="links">
 Links: <xsl:apply-templates select="a:link" mode="links"/>
    </div>
    <xsl:apply-templates/>
    <xsl:apply-templates select="a:entry" mode="body"/>
  </body>
</html>
  </xsl:template>

  <xsl:template match="/a:entry">
<html>
  <head>
    <title><xsl:value-of select="a:title"/></title>
  </head>
  <body>
    <xsl:apply-templates select="." mode="body"/>
  </body>
</html>
  </xsl:template>

  <xsl:template match="a:entry" mode="body">
    <div class="entry">
      <h2><xsl:apply-templates select="a:title" mode="text-construct"/></h2>
      <div class="id">Entry ID: <xsl:value-of select="a:id"/></div>
      <div class="updated">Entry updated: <xsl:value-of select="a:updated"/></div>
      <div class="links">
	  Links: <xsl:apply-templates select="a:link" mode="links"/>
      </div>
      <div class="categories">
	<xsl:text>Categories: </xsl:text>
	<xsl:apply-templates select="a:category" mode="categories"/>
      </div>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="a:summary">
    <div class="summary">
      <xsl:apply-templates select="." mode="text-construct"/>
    </div>
  </xsl:template>

  <xsl:template match="a:content">
    <div class="content">
      <xsl:apply-templates select="." mode="text-construct"/>
    </div>
  </xsl:template>

  <xsl:template match="a:link" mode="links">
    <a href="{@href}">
      <xsl:value-of select="@rel"/>
      <xsl:if test="not(@rel)">[generic link]</xsl:if>
      <xsl:if test="@type">
	<xsl:text> (</xsl:text><xsl:value-of select="@type"/><xsl:text>): </xsl:text>
      </xsl:if>
      <xsl:value-of select="@title"/>
    </a>
    <xsl:if test="position() != last()">
      <xsl:text> | </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="a:category" mode="categories">
    <xsl:value-of select="@term"/>
    <xsl:if test="position() != last()">
      <xsl:text> | </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[@type='text']|*[not(@type)]" mode="text-construct">
    <xsl:value-of select="node()"/>
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
