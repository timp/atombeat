<!--
  Copyright (C) 2010 Orbeon, Inc.

  This program is free software; you can redistribute it and/or modify it under the terms of the
  GNU Lesser General Public License as published by the Free Software Foundation; either version
  2.1 of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU Lesser General Public License for more details.

  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  -->
<p:config xmlns:p="http://www.orbeon.com/oxf/pipeline"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oxf="http://www.orbeon.com/oxf/processors"
    xmlns:xforms="http://www.w3.org/2002/xforms"
    xmlns:xxforms="http://orbeon.org/oxf/xml/xforms"
    xmlns:xhtml="http://www.w3.org/1999/xhtml">

    <p:param type="input" name="data"/>
    <p:param type="input" name="model-data"/>
    <p:param type="input" name="instance"/>
    <p:param type="output" name="xformed-data"/>

    <!-- Get request information -->
    <p:processor name="oxf:request">
        <p:input name="config">
            <config>
                <include>/request/container-type</include>
                <include>/request/request-path</include>
                <include>/request/context-path</include>
            </config>
        </p:input>
        <p:output name="data" id="request-info"/>
    </p:processor>

    <!-- Annotate XForms elements and generate XHTML if necessary -->
    <!-- TODO: put here processor detecting XForms model -->
    <p:choose href="#data">
        <!-- ========== Test for NG XForms engine ========== -->
        <!-- NOTE: in the future, we may want to support "XForms within XML" so this test will have to be modified -->
        <p:when test="/xhtml:html/xhtml:head/xforms:model"><!-- TODO: test on result of processor above -->
            <!-- Handle widgets -->

            <!--<p:processor name="oxf:sax-logger">-->
                <!--<p:input name="data" href="#data"/>-->
                <!--<p:output name="data" id="data2"/>-->
            <!--</p:processor>-->

            <!-- Apply XForms preprocessing if needed -->
            <p:choose href="aggregate('null')"><!-- dummy test input -->
                <p:when test="p:property('oxf.epilogue.xforms.preprocessing') = true()">

                    <!-- Get preprocessing URI from property -->
                    <p:processor name="oxf:identity">
                        <p:input name="data" href="aggregate('config', aggregate('null')#xpointer(p:property('oxf.epilogue.xforms.preprocessing.uri')))"/>
                        <p:output name="data" id="preprocessing-config"/>
                    </p:processor>

                    <p:processor name="oxf:url-generator">
                        <p:input name="config" href="#preprocessing-config"/>
                        <p:output name="data" id="preprocessing"/>
                    </p:processor>

                    <!-- Apply preprocessing step -->
                    <!-- TODO: detection of XSLT or XPL? -->
                    <p:processor name="oxf:pipeline">
                        <p:input name="data" href="#data"/>
                        <p:input name="config" href="#preprocessing"/>
                        <p:output name="data" id="preprocessed-view"/>
                    </p:processor>
                </p:when>
                <p:otherwise>
                    <!-- No preprocessing -->
                    <p:processor name="oxf:identity">
                        <p:input name="data" href="#data"/>
                        <p:output name="data" id="preprocessed-view"/>
                    </p:processor>
                </p:otherwise>
            </p:choose>

            <!-- Apply XForms widgets if needed -->
            <p:choose href="aggregate('null')"><!-- dummy test input -->
                <p:when test="p:property('oxf.epilogue.xforms.widgets') = true()">

                    <!-- Get widgets URI from property -->
                    <p:processor name="oxf:identity">
                        <p:input name="data" href="aggregate('config', aggregate('null')#xpointer(p:property('oxf.epilogue.xforms.widgets.uri')))"/>
                        <p:output name="data" id="widgets-config"/>
                    </p:processor>

                    <p:processor name="oxf:url-generator">
                        <p:input name="config" href="#widgets-config"/>
                        <p:output name="data" id="aggregate"/>
                    </p:processor>

                    <!-- Apply widgets -->
                    <p:processor name="oxf:unsafe-xslt">
                        <!--<p:input name="data" href="#data2"/>-->
                        <p:input name="data" href="#preprocessed-view"/>
                        <p:input name="config" href="#aggregate"/>
                        <p:output name="data" id="widgeted-view"/>
                        <!-- This is here just so that we can reload the form when the properties or the resources change -->
                        <p:input name="properties-local" href="oxf:/config/properties-local.xml"/>
                    </p:processor>
                </p:when>
                <p:otherwise>
                    <!-- No widgets -->
                    <p:processor name="oxf:identity">
                        <p:input name="data" href="#preprocessed-view"/>
                        <p:output name="data" id="widgeted-view"/>
                    </p:processor>
                </p:otherwise>
            </p:choose>

            <!-- Get current namespace to enable caching per portlet -->
            <p:processor name="oxf:request">
                <p:input name="config">
                    <config>
                        <include>/request/container-namespace</include>
                    </config>
                </p:input>
                <p:output name="data" id="namespace"/>
            </p:processor>

            <!-- Native XForms Initialization -->
            <p:processor name="oxf:xforms-to-xhtml">
                <p:input name="annotated-document" href="#widgeted-view"/>
                <!--<p:input name="annotated-document" href="#widgeted-view" schema-href="oxf:/ops/xforms/schema/orbeon.rng"/>-->
                <p:input name="data" href="#model-data"/>
                <!-- This input adds a dependency on the container namespace. Keep it for portlets. -->
                <p:input name="namespace" href="#namespace"/>
                <p:input name="instance" href="#instance"/>
                <p:output name="document" id="xhtml-data"/>
            </p:processor>

            <!--<p:processor name="oxf:sax-logger">-->
                <!--<p:input name="data" href="#xhtml-data"/>-->
                <!--<p:output name="data" id="xhtml-data2"/>-->
            <!--</p:processor>-->

            <!-- XInclude processing to add error dialog configuration and more -->
            <p:processor name="oxf:xinclude">
                <p:input name="config" href="#xhtml-data"/>
                <p:output name="data" ref="xformed-data"/>
            </p:processor>
        </p:when>
        <p:otherwise>
            <!-- ========== No XForms ========== -->
            <p:processor name="oxf:identity">
                <p:input name="data" href="#data"/>
                <p:output name="data" ref="xformed-data"/>
            </p:processor>
        </p:otherwise>
    </p:choose>

</p:config>
