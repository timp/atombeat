/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.test;

import org.dom4j.QName;
import org.orbeon.oxf.xforms.control.XFormsSingleNodeControl;
import org.orbeon.oxf.xforms.control.controls.XFormsInputControl;
import org.xml.sax.helpers.AttributesImpl;

import java.util.LinkedHashMap;
import java.util.Map;

public class XFormsControlsTest extends ResourceManagerTestBase {

    public void testDiffCustomMIPsChanges() {

        final AttributesImpl attributes = new AttributesImpl();
        final XFormsSingleNodeControl control1 = new XFormsInputControl(null, null, null, "input", "input-1") {

            private Map<String, String> customMIPs = new LinkedHashMap<String, String>();
            {
                customMIPs.put("name1", "value1");
                customMIPs.put("name2", "value2");
                customMIPs.put("name3", "value3");
                customMIPs.put("name4", "value4");
            }

            @Override
            public Map<String, String> getCustomMIPs() {
                return customMIPs;
            }
        };

        final XFormsSingleNodeControl control2 = new XFormsInputControl(null, null, null, "input", "input-2") {
            private Map<String, String> customMIPs = new LinkedHashMap<String, String>();
            {
                // leave as is
                customMIPs.put("name1", "value1");
                // remove name2
                // change value
                customMIPs.put("name3", "newvalue3");
                // leave as is
                customMIPs.put("name4", "value4");
            }

            @Override
            public Map<String, String> getCustomMIPs() {
                return customMIPs;
            }
        };

        XFormsSingleNodeControl.diffCustomMIPs(attributes, control1, control2, false, false);
        assertEquals("-name2-value2 -name3-value3 +name3-newvalue3", attributes.getValue("class"));
    }
    
    public void testDiffCustomMIPsNew() {

        final AttributesImpl attributes = new AttributesImpl();

        final XFormsSingleNodeControl control2 = new XFormsInputControl(null, null, null, "input", "input-1") {
            private Map<String, String> customMIPs = new LinkedHashMap<String, String>();
            {
                customMIPs.put("name1", "value1");
                customMIPs.put("name2", "value2");
                customMIPs.put("name3", "value3");
                customMIPs.put("name4", "value4");
            }

            @Override
            public Map<String, String> getCustomMIPs() {
                return customMIPs;
            }
        };

        XFormsSingleNodeControl.diffCustomMIPs(attributes, null, control2, false, false);
        assertEquals("name1-value1 name2-value2 name3-value3 name4-value4", attributes.getValue("class"));
    }

    public void testDiffClassAVT() {
        final AttributesImpl attributes = new AttributesImpl();

        final XFormsSingleNodeControl control1 = new XFormsInputControl(null, null, null, "input", "input-1") {
            @Override
            public String getExtensionAttributeValue(QName attributeName) {
                return "foo bar gaga";
            }
        };

        final XFormsSingleNodeControl control2 = new XFormsInputControl(null, null, null, "input", "input-2") {
            @Override
            public String getExtensionAttributeValue(QName attributeName) {
                return "bar toto";
            }
        };

        XFormsSingleNodeControl.diffClassAVT(attributes, control1, control2, false, false);
        assertEquals("-foo -gaga +toto", attributes.getValue("class"));
    }

    public void testDiffClassAVTNew() {
        final AttributesImpl attributes = new AttributesImpl();

        final XFormsSingleNodeControl control2 = new XFormsInputControl(null, null, null, "input", "input-1") {
            @Override
            public String getExtensionAttributeValue(QName attributeName) {
                return "foo bar";
            }
        };

        XFormsSingleNodeControl.diffClassAVT(attributes, null, control2, false, false);
        assertEquals("foo bar", attributes.getValue("class"));
    }

    // NOTE: started writing this test, but just using an XFormsOutputControl without the context of an XFormsContainingDocument seems a dead-end!
//    public void testOutputControlRewrite() {
//
//        final Document document = Dom4jUtils.readFromURL("oxf:/org/orbeon/oxf/xforms/processor/test-form.xml", false, false);
//        final DocumentWrapper documentWrapper = new DocumentWrapper(document, null, new Configuration());
//        final Element outputElement = (Element) ((NodeWrapper) XPathCache.evaluateSingle(new PipelineContext(), documentWrapper, "(//xhtml:body//xforms:output)[1]", XFormsDocumentAnnotatorContentHandlerTest.BASIC_NAMESPACE_MAPPINGS, null, null, null, null, null)).getUnderlyingNode();
//
//        final PipelineContext pipelineContext = new PipelineContext();
//
//        final XBLContainer container = new XBLContainer("", null) {};
//        final XFormsOutputControl control1 = new XFormsOutputControl(container, null, outputElement, "output", "output-1");
//        control1.setBindingContext(pipelineContext, new XFormsContextStack.BindingContext(null, null, Collections.singletonList(documentWrapper.wrap(outputElement)), 1, "output-1", true, outputElement, null, false, null));
//
//        control1.evaluateIfNeeded(pipelineContext);
//
//        assertEquals("", control1.getExternalValue(pipelineContext));
//    }
}
