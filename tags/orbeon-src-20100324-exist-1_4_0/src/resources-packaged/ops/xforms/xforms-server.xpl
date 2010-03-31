<!--
    Copyright (C) 2005 Orbeon, Inc.

    This program is free software; you can redistribute it and/or modify it under the terms of the
    GNU Lesser General Public License as published by the Free Software Foundation; either version
    2.1 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
-->
<p:config xmlns:p="http://www.orbeon.com/oxf/pipeline"
          xmlns:oxf="http://www.orbeon.com/oxf/processors"
          xmlns:xs="http://www.w3.org/2001/XMLSchema"
          xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- Extract request headers and method -->
    <p:processor name="oxf:request">
        <p:input name="config">
            <config stream-type="xs:anyURI">
                <include>/request/headers/header[name = 'content-type']</include>
                <include>/request/method</include>
            </config>
        </p:input>
        <p:output name="data" id="request-metadata"/>
        <!--<p:output name="data" id="request-metadata" debug="xxxparams"/>-->
    </p:processor>

    <p:choose href="#request-metadata">
        <p:when test="/request/method = 'POST' and not(starts-with(/request/headers/header[name = 'content-type']/value, 'multipart/form-data'))">
            <!-- This is a regular AJAX request -->

            <!-- Extract request body -->
            <p:processor name="oxf:request">
                <p:input name="config">
                    <config stream-type="xs:anyURI">
                        <include>/request/body</include>
                    </config>
                </p:input>
                <p:output name="data" id="request-body"/>
            </p:processor>

            <!-- Dereference URI and return XML -->
            <p:processor name="oxf:url-generator">
                <p:input name="config"
                         href="aggregate('config', aggregate('url', #request-body#xpointer(string(/request/body))))"/>
                <p:output name="data" id="xforms-request"/>
            </p:processor>

            <!-- Run XForms Server -->
            <p:processor name="oxf:xforms-server">
                <p:input name="request" href="#xforms-request" schema-href="xforms-server-request.rng"/>
                <p:output name="response" id="xforms-response"/>
                <!--<p:input name="request" href="#xforms-request" schema-href="xforms-server-request.rng" debug="xxxrequest"/>-->
                <!--<p:output name="response" id="xforms-response" debug="xxxresponse"/>-->
            </p:processor>

            <!-- Generate response -->
            <p:choose href="#xforms-response">
                <p:when test="/document[starts-with(@content-type, 'text/html')]">
                    <!-- HTML response for Ajax portlets -->
                    <p:processor name="oxf:http-serializer">
                        <p:input name="config">
                            <config>
                                <cache-control>
                                    <use-local-cache>false</use-local-cache>
                                </cache-control>
                            </config>
                        </p:input>
                        <p:input name="data" href="#xforms-response"/>
                    </p:processor>
                </p:when>
                <p:when test="/document[not(starts-with(@content-type, 'text/html'))]">
                    <!-- Other non-XML response (unsupported) -->
                    <p:processor name="oxf:html-converter">
                        <p:input name="config">
                            <config>
                                <encoding>utf-8</encoding>
                                <indent>false</indent>
                            </config>
                        </p:input>
                        <p:input name="data">
                            <!-- TODO: using a hardcoded inline response is not the best way -->
                            <div>
                                <p>Invalid portlet response!</p>
                            </div>
                        </p:input>
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
                </p:when>
                <p:otherwise>
                    <!-- Regular Ajax response -->
                    <p:processor name="oxf:xml-converter">
                        <p:input name="config">
                            <config>
                                <encoding>utf-8</encoding>
                                <indent>false</indent>
                            </config>
                        </p:input>
                        <!-- Do schema validation here -->
                        <p:input name="data" href="#xforms-response" schema-href="xforms-server-response.rng" />
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
        </p:when>
        <p:when test="/request/method = 'GET'">
            <!-- Handle combined resources -->
            <p:processor name="oxf:xforms-resource-server"/>
        </p:when>
        <p:otherwise>
            <!-- This is (probably) a form submission that requires a pseudo-AJAX response -->

            <!-- Extract parameters -->
            <p:processor name="oxf:request">
                <p:input name="config">
                    <config stream-type="xs:anyURI">
                        <include>/request/parameters</include>
                    </config>
                </p:input>
                <!--<p:output name="data" id="request-params" debug="xxxrequest-params"/>-->
                <p:output name="data" id="request-params"/>
            </p:processor>

            <!-- Create XForms Server request -->
            <p:processor name="oxf:xslt">
                <p:input name="data" href="#request-params"/>
                <p:input name="config">
                    <xxforms:event-request xsl:version="2.0" xmlns:xxforms="http://orbeon.org/oxf/xml/xforms">
                        <xxforms:static-state>
                            <xsl:value-of select="/*/parameters/parameter[name = '$static-state']/value"/>
                        </xxforms:static-state>
                        <xxforms:dynamic-state>
                            <xsl:value-of select="/*/parameters/parameter[name = '$dynamic-state']/value"/>
                        </xxforms:dynamic-state>
                        <xsl:variable name="files" select="/*/parameters/parameter[filename]"/>
                        <xsl:if test="$files">
                            <xxforms:files>
                                <xsl:copy-of select="$files"/>
                            </xxforms:files>
                        </xsl:if>
                        <xxforms:action/>
                        <xsl:variable name="server-events" select="/*/parameters/parameter[name = '$server-events']/value"/>
                        <xsl:if test="not($server-events = '')">
                            <xxforms:server-events>
                                <xsl:value-of select="$server-events"/>
                            </xxforms:server-events>
                        </xsl:if>
                    </xxforms:event-request>
                </p:input>
                <p:output name="data" id="xforms-request"/>
            </p:processor>

            <!-- Run XForms Server -->
            <p:processor name="oxf:xforms-server">
                <p:input name="request" href="#xforms-request" schema-href="xforms-server-request.rng"/>
                <p:output name="response" id="xforms-response" schema-href="xforms-server-response.rng"/>
                <!--<p:input name="request" href="#xforms-request" schema-href="xforms-server-request.rng" debug="xxxrequest"/>-->
                <!--<p:output name="response" id="xforms-response" schema-href="xforms-server-response.rng" debug="xxxresponse"/>-->
            </p:processor>

            <!-- Create XForms Server response -->
            <p:processor name="oxf:unsafe-xslt">
                <p:input name="data" href="#xforms-response"/>
                <p:input name="config">
                    <xsl:stylesheet version="2.0" xmlns:saxon="http://saxon.sf.net/">
                        <xsl:output name="xml" method="xml" omit-xml-declaration="yes" indent="no"/>
                        <xsl:template match="/">
                            <html>
                                <body>
                                    <xsl:value-of select="saxon:serialize(/, 'xml')"/>
                                </body>
                            </html>
                        </xsl:template>
                    </xsl:stylesheet>

                </p:input>
                <p:output name="data" id="html-response"/>
                <!--<p:output name="data" id="html-response" debug="xxxhtml-response"/>-->
            </p:processor>

            <!-- Generate response -->
            <p:processor name="oxf:html-converter">
                <p:input name="config">
                    <config>
                        <public-doctype>-//W3C//DTD HTML 4.01//EN</public-doctype>
                        <system-doctype>http://www.w3.org/TR/html4/strict.dtd</system-doctype>
                        <version>4.01</version>
                        <encoding>utf-8</encoding>
                        <indent>false</indent>
                    </config>
                </p:input>
                <p:input name="data" href="#html-response"/>
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
