<?xml version="1.0" encoding="utf-8"?>
<!--
    Copyright (C) 2008 Orbeon, Inc.

    This program is free software; you can redistribute it and/or modify it under the terms of the
    GNU Lesser General Public License as published by the Free Software Foundation; either version
    2.1 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
-->
<p:config xmlns:p="http://www.orbeon.com/oxf/pipeline"
        xmlns:odt="http://orbeon.org/oxf/xml/datatypes"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:oxf="http://www.orbeon.com/oxf/processors"
        xmlns:xi="http://www.w3.org/2001/XInclude"
        xmlns:xforms="http://www.w3.org/2002/xforms"
        xmlns:ev="http://www.w3.org/2001/xml-events"
        xmlns:saxon="http://saxon.sf.net/"
        xmlns:pipeline="java:org.orbeon.oxf.processor.pipeline.PipelineFunctionLibrary">

    <!-- app/form (request-parameters.xml) -->
    <p:param type="input" name="instance"/>
    <p:param type="output" name="data"/>

    <!-- Resources -->
    <p:processor name="oxf:unsafe-xslt">
        <p:input name="data" href="resources.xml"/>
        <p:input name="instance" href="#instance"/>
        <p:input name="config">
            <xsl:stylesheet version="2.0">
                <xsl:import href="oxf:/oxf/xslt/utils/copy.xsl"/>

                <xsl:variable name="resources" as="element(resource)+" select="/resources/resource"/>
                <xsl:variable name="instance" as="element(request)" select="doc('input:instance')/request"/>
                <xsl:variable name="app" as="xs:string" select="$instance/app"/>
                <xsl:variable name="form" as="xs:string" select="$instance/form"/>

                <!-- The property names that are overriden -->
                <xsl:variable name="property-names" as="xs:string*" select="pipeline:propertiesStartsWith(concat('oxf.fr.resource.', $app, '.', $form))"/>

                <!-- Get ID of nodes we override followed by their new value -->
                <xsl:variable name="overriden-elements-and-values" as="xs:string*">
                    <xsl:for-each select="$property-names">
                        <xsl:variable name="name" as="xs:string" select="."/>
                        <xsl:variable name="name-without-prefix" as="xs:string" select="substring-after(substring-after(substring-after($name, 'oxf.fr.resource.'), '.'), '.')"/>
                        <xsl:variable name="language" as="xs:string" select="substring-before($name-without-prefix, '.')"/>
                        <xsl:variable name="path" as="xs:string" select="translate(substring-after($name-without-prefix, '.'), '.', '/')"/>
                        <!-- Get resource for selected language -->
                        <xsl:variable name="resource" as="element(resource)?" select="$resources[@xml:lang = $language]"/>
                        <xsl:variable name="node" as="element()?" select="$resource/saxon:evaluate($path)"/>
                        <xsl:choose>
                            <xsl:when test="empty($node)">
                                <xsl:message>Cannot find for resource for property '<xsl:value-of select="$name"/>'</xsl:message>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:sequence select="(generate-id($node), pipeline:property($name))"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:variable>
                <!-- ID of nodes we override -->
                <xsl:variable name="overriden-elements" select="$overriden-elements-and-values[position() mod 2 = 1]"/>
                <xsl:variable name="overriden-values" select="$overriden-elements-and-values[position() mod 2 = 0]"/>

                <!-- Override node -->
                <xsl:template match="*[generate-id() = $overriden-elements]">
                    <xsl:variable name="id" as="xs:string" select="generate-id()"/>
                     <!-- Tricky: index-of() may return more than one positions. We take the first one as
                          propertiesStartsWith() returns more specific items first. -->
                    <xsl:variable name="value" as="xs:string" select="$overriden-values[index-of($overriden-elements, $id)[1]]"/>
                    <xsl:copy>
                        <xsl:value-of select="$value"/>
                    </xsl:copy>
                </xsl:template>
            </xsl:stylesheet>
        </p:input>
        <!-- Dependency on overridden properties so stylesheet runs again when properties change -->
        <p:input name="properties-local" href="oxf:/config/properties-local.xml"/>
        <p:output name="data" ref="data"/>
    </p:processor>

</p:config>
