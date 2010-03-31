<!--
  Copyright (C) 2009 Orbeon, Inc.

  This program is free software; you can redistribute it and/or modify it under the terms of the
  GNU Lesser General Public License as published by the Free Software Foundation; either version
  2.1 of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU Lesser General Public License for more details.

  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  -->
<!--
    Embeddable theme for Form Runner.
-->
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:f="http://orbeon.org/oxf/xml/formatting"
    xmlns:context="java:org.orbeon.oxf.pipeline.StaticExternalContext">

    <xsl:template match="/">
        <xhtml:div class="orbeon-portlet-div">
            <!-- Handle head elements except scripts -->
            <xsl:for-each select="/xhtml:html/xhtml:head/(xhtml:meta | xhtml:link | xhtml:style)">
                <xsl:element name="xhtml:{local-name()}" namespace="{namespace-uri()}">
                    <xsl:copy-of select="@*"/>
                    <xsl:apply-templates/>
                </xsl:element>
            </xsl:for-each>
            <!-- Try to get a title and set it on the portlet -->
            <xsl:if test="normalize-space(/xhtml:html/xhtml:head/xhtml:title)">
                <xsl:value-of select="context:setTitle(normalize-space(/xhtml:html/xhtml:head/xhtml:title))"/>
            </xsl:if>
            <xhtml:div class="orbeon-portlet-content">
                <xsl:apply-templates select="/xhtml:html/xhtml:body/node()"/>
            </xhtml:div>
            <!-- Scripts at the bottom of the page. This is not valid HTML, but it is a recommended practice for
                 performance as of early 2008. See http://developer.yahoo.com/performance/rules.html#js_bottom -->
            <xsl:for-each select="/xhtml:html/xhtml:head/xhtml:script">
                <xsl:element name="xhtml:{local-name()}" namespace="{namespace-uri()}">
                    <xsl:copy-of select="@*"/>
                    <xsl:apply-templates/>
                </xsl:element>
            </xsl:for-each>
        </xhtml:div>
    </xsl:template>

    <!-- Remember that we are embeddable -->
    <xsl:template match="xhtml:form">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xhtml:input type="hidden" name="orbeon-embeddable" value="true"/>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- If the field is a checkbox, add "[]", remove it. This is to support PHP-based proxies, which might add the brackets. -->
    <xsl:template match="xhtml:input[@type = 'checkbox']">
        <xsl:copy>
            <xsl:attribute name="name" select="concat(@name, '[]')"/>
            <xsl:apply-templates select="@* except @name | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Simply copy everything that's not matched -->
    <xsl:template match="@*|node()" priority="-2">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
