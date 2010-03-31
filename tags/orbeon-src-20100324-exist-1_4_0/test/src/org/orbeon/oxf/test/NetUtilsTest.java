/**
 *  Copyright (C) 2009 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.test;

import org.dom4j.Document;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorUtils;
import org.orbeon.oxf.processor.test.TestExternalContext;
import org.orbeon.oxf.util.HttpServletRequestStub;
import org.orbeon.oxf.util.ISODateUtils;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.XFormsConstants;
import org.orbeon.oxf.xml.XMLConstants;

import javax.servlet.http.HttpServletRequest;

public class NetUtilsTest extends ResourceManagerTestBase {

    private PipelineContext pipelineContext;
    private ExternalContext externalContext;
    private ExternalContext.Request request;
    private ExternalContext.Response response;

    protected void setUp() throws Exception {

        pipelineContext = new PipelineContext();

        final Document requestDocument = ProcessorUtils.createDocumentFromURL("oxf:/org/orbeon/oxf/test/if-modified-since-request.xml", null);
        externalContext = new TestExternalContext(pipelineContext, requestDocument);
        pipelineContext.setAttribute(PipelineContext.EXTERNAL_CONTEXT, externalContext);
        request = externalContext.getRequest();
        response = externalContext.getResponse();
    }

    public void testCheckIfModifiedSince() {

        // Get long value for If-Modified-Since present in request
        final String ifModifiedHeaderString = "Thu, 28 Jun 2007 14:17:36 GMT";
        final long ifModifiedHeaderLong = ISODateUtils.parseRFC1123Date(ifModifiedHeaderString);

        final HttpServletRequest httpServletRequest = new HttpServletRequestStub() {
            public String getMethod() {
                return "GET";
            }

            public String getHeader(String s) {
                if (s.equalsIgnoreCase("If-Modified-Since")) {
                    return ifModifiedHeaderString;
                } else {
                    return null;
                }
            }
        };

        assertEquals(NetUtils.checkIfModifiedSince(httpServletRequest, ifModifiedHeaderLong -1), false);
        assertEquals(NetUtils.checkIfModifiedSince(httpServletRequest, ifModifiedHeaderLong), false);
        // For some reason the code checks that there is more than one second of difference
        assertEquals(NetUtils.checkIfModifiedSince(httpServletRequest, ifModifiedHeaderLong + 1001), true);
    }

    public void testProxyURI() {
        assertEquals("/xforms-server/dynamic/87c938edbc170d5038192ca5ab9add97", NetUtils.proxyURI(pipelineContext, "/foo/bar.png", null, null, -1));
        assertEquals("/xforms-server/dynamic/674c2ff956348155ff60c01c0c0ec2e0", NetUtils.proxyURI(pipelineContext, "http://example.org/foo/bar.png", null, null, -1));
    }

    public void testConvertUploadTypes() {

        final String testDataURI = "oxf:/org/orbeon/oxf/test/anyuri-test-content.xml";
        final String testDataString = "You have to let people challenge your ideas.";
        final String testDataBase64 = "WW91IGhhdmUgdG8gbGV0IHBlb3BsZSBjaGFsbGVuZ2UgeW91ciBpZGVhcy4=";

        // anyURI -> base64
        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataURI,
                XMLConstants.XS_ANYURI_EXPLODED_QNAME, XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME),
                testDataBase64);

        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataURI,
                XMLConstants.XS_ANYURI_EXPLODED_QNAME, XFormsConstants.XFORMS_BASE64BINARY_EXPLODED_QNAME),
                testDataBase64);

        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataURI,
                XFormsConstants.XFORMS_ANYURI_EXPLODED_QNAME, XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME),
                testDataBase64);

        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataURI,
                XFormsConstants.XFORMS_ANYURI_EXPLODED_QNAME, XFormsConstants.XFORMS_BASE64BINARY_EXPLODED_QNAME),
                testDataBase64);

        // base64 -> anyURI
        String result = XFormsUtils.convertUploadTypes(pipelineContext, testDataBase64,
                XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME, XMLConstants.XS_ANYURI_EXPLODED_QNAME);
        assertTrue(result.startsWith("file:/"));
        assertEquals(testDataBase64, NetUtils.anyURIToBase64Binary(result));

        result = XFormsUtils.convertUploadTypes(pipelineContext, testDataBase64,
                XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME, XFormsConstants.XFORMS_ANYURI_EXPLODED_QNAME);
        assertTrue(result.startsWith("file:/"));
        assertEquals(testDataBase64, NetUtils.anyURIToBase64Binary(result));

        result = XFormsUtils.convertUploadTypes(pipelineContext, testDataBase64,
                XFormsConstants.XFORMS_BASE64BINARY_EXPLODED_QNAME, XMLConstants.XS_ANYURI_EXPLODED_QNAME);
        assertTrue(result.startsWith("file:/"));
        assertEquals(testDataBase64, NetUtils.anyURIToBase64Binary(result));

        result = XFormsUtils.convertUploadTypes(pipelineContext, testDataBase64,
                XFormsConstants.XFORMS_BASE64BINARY_EXPLODED_QNAME, XFormsConstants.XFORMS_ANYURI_EXPLODED_QNAME);
        assertTrue(result.startsWith("file:/"));
        assertEquals(testDataBase64, NetUtils.anyURIToBase64Binary(result));

        // All the following tests must not change the input
        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataString,
                XMLConstants.XS_ANYURI_EXPLODED_QNAME, XFormsConstants.XFORMS_ANYURI_EXPLODED_QNAME),
                testDataString);

        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataString,
                XFormsConstants.XFORMS_ANYURI_EXPLODED_QNAME, XMLConstants.XS_ANYURI_EXPLODED_QNAME),
                testDataString);

        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataBase64,
                XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME, XFormsConstants.XFORMS_BASE64BINARY_EXPLODED_QNAME),
                testDataBase64);

        assertEquals(XFormsUtils.convertUploadTypes(pipelineContext, testDataBase64,
                XFormsConstants.XFORMS_BASE64BINARY_EXPLODED_QNAME, XMLConstants.XS_BASE64BINARY_EXPLODED_QNAME),
                testDataBase64);
    }
}
