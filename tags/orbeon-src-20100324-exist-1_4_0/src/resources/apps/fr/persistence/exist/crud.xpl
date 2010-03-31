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
        xmlns:sql="http://orbeon.org/oxf/xml/sql"
        xmlns:odt="http://orbeon.org/oxf/xml/datatypes"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:oxf="http://www.orbeon.com/oxf/processors"
        xmlns:xi="http://www.w3.org/2001/XInclude"
        xmlns:xforms="http://www.w3.org/2002/xforms"
        xmlns:ev="http://www.w3.org/2001/xml-events"
        xmlns:pipeline="java:org.orbeon.oxf.processor.pipeline.PipelineFunctionLibrary">

    <p:param type="input" name="instance"/>

    <!-- NOTE: It's disappointing that we have to use oxf:request/oxf:perl5-matcher rather than using the page flow
         directly, but because we want to support the PUT and POST methods, this is currently the only solution. -->
    <p:processor name="oxf:request">
        <p:input name="config">
            <config stream-type="xs:anyURI">
                <include>/request/request-path</include>
                <include>/request/content-type</include>
                <include>/request/method</include>
                <include>/request/body</include>
            </config>
        </p:input>
        <p:output name="data" id="request"/>
    </p:processor>

    <p:processor name="oxf:perl5-matcher">
        <p:input name="config"><config>/fr/service/exist/crud/([^/]+/[^/]+/(form/[^/]+|data/[^/]+/[^/]+))</config></p:input>
        <p:input name="data" href="#request#xpointer(/request/request-path)"/>
        <p:output name="data" id="matcher-groups"/>
    </p:processor>

    <!-- Discriminate based on the HTTP method and content type -->
    <p:choose href="#request">
        <!-- Handle binary and XML GET -->
        <p:when test="/*/method = 'GET'">

            <!-- Read URL -->
            <p:processor name="oxf:url-generator">
                <p:input name="config" transform="oxf:unsafe-xslt" href="#matcher-groups">
                    <config xsl:version="2.0">
                        <url>
                            <xsl:value-of select="pipeline:rewriteServiceURI(concat(pipeline:property('oxf.fr.persistence.service.exist.uri'), '/', /*/group[1]), true())"/>
                        </url>
                        <!-- Forward the same headers that the XForms engine forwards -->
                        <forward-headers><xsl:value-of select="pipeline:property('oxf.xforms.forward-submission-headers')"/></forward-headers>
                        <!-- Produce binary so we do our own XML parsing -->
                        <mode>binary</mode>
                    </config>
                </p:input>
                <p:output name="data" id="document"/>
            </p:processor>

            <!-- Serialize out as is -->
            <p:processor name="oxf:http-serializer">
                <p:input name="config">
                    <config>
                        <cache-control>
                            <use-local-cache>false</use-local-cache>
                        </cache-control>
                    </config>
                </p:input>
                <p:input name="data" href="#document"/>
            </p:processor>

        </p:when>
        <p:otherwise>
            <!-- Discriminate based on the HTTP method and content type -->
            <p:choose href="#request">

                <!-- Binary PUT -->
                <p:when test="/*/method = 'PUT' and not(/*/content-type = ('application/xml', 'text/xml') or ends-with(/*/content-type, '+xml'))">

                    <p:processor name="oxf:xforms-submission">
                        <p:input name="submission">
                            <!-- NOTE: The <body> element contains the xs:anyURI type -->
                            <xforms:submission ref="/*/body" method="put" replace="none"
                                    serialization="application/octet-stream"
                                    resource="{xxforms:property('oxf.fr.persistence.service.exist.uri')}/{/*/group[1]}">
                                <xforms:action ev:event="xforms-submit-error">
                                    <!-- TODO: Propagate error to caller -->
                                    <xforms:delete while="/*/*" nodeset="/*/*"/>
                                    <xforms:setvalue ref="/*" value="event('response-body')"/>
                                    <xforms:message level="xxforms:log-debug"><xforms:output value="event('response-body')"/></xforms:message>
                                </xforms:action>
                            </xforms:submission>
                        </p:input>
                        <p:input name="request" href="aggregate('root', #request#xpointer(/*/body), #matcher-groups#xpointer(/*/group))"/>
                        <p:output name="response" id="response"/>
                    </p:processor>

                </p:when>
                <!-- DELETE -->
                <p:when test="/*/method = 'DELETE'">

                    <p:processor name="oxf:xforms-submission">
                        <p:input name="submission">
                            <xforms:submission method="delete" replace="none" serialization="none"
                                    resource="{xxforms:property('oxf.fr.persistence.service.exist.uri')}/{/*/group[1]}">
                                <xforms:action ev:event="xforms-submit-error">
                                    <!-- TODO: Propagate error to caller -->
                                    <xforms:delete while="/*/*" nodeset="/*/*"/>
                                    <xforms:setvalue ref="/*" value="event('response-body')"/>
                                    <xforms:message level="xxforms:log-debug"><xforms:output value="event('response-body')"/></xforms:message>
                                </xforms:action>
                            </xforms:submission>
                        </p:input>
                        <p:input name="request" href="#matcher-groups"/>
                        <p:output name="response" id="response"/>
                    </p:processor>

                </p:when>
                <!-- XML PUT -->
                <p:when test="/*/method = 'PUT'">

                    <p:processor name="oxf:xforms-submission">
                        <p:input name="submission">
                            <xforms:submission ref="/*/*[1]" method="put" replace="none"
                                    resource="{xxforms:property('oxf.fr.persistence.service.exist.uri')}/{/*/group[1]}">
                                <xforms:action ev:event="xforms-submit-error">
                                    <!-- TODO: Propagate error to caller -->
                                    <xforms:delete while="/*/*" nodeset="/*/*"/>
                                    <xforms:setvalue ref="/*" value="event('response-body')"/>
                                    <xforms:message level="xxforms:log-debug"><xforms:output value="event('response-body')"/></xforms:message>
                                </xforms:action>
                            </xforms:submission>
                        </p:input>
                        <p:input name="request" href="aggregate('root', #instance, #matcher-groups#xpointer(/*/group))"/>
                        <p:output name="response" id="response"/>
                    </p:processor>
                </p:when>
            </p:choose>

            <!-- Convert and serialize to XML -->
            <p:processor name="oxf:xml-converter">
                <p:input name="config">
                    <config>
                        <indent>false</indent>
                        <encoding>utf-8</encoding>
                    </config>
                </p:input>
                <p:input name="data" href="#response"/>
                <p:output name="data" id="converted"/>
            </p:processor>

            <p:processor name="oxf:http-serializer">
                <p:input name="config">
                    <config>
                        <cache-control>
                            <use-local-cache>false</use-local-cache>
                        </cache-control>
                    </config>
                </p:input>
                <p:input name="data" href="#converted"/>
            </p:processor>
        </p:otherwise>

    </p:choose>

</p:config>