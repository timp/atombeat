/**
 *  Copyright (C) 2008 Orbeon, Inc.
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
package org.orbeon.oxf.xforms.function.xxforms;

import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.transformer.TransformerURIResolver;
import org.orbeon.oxf.xforms.function.XFormsFunction;
import org.orbeon.oxf.xml.ContentHandlerAdapter;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.StringValue;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.transform.sax.SAXSource;

/*
 * NOTE: this class inspired by Saxon's UnparsedText.java.
 */
public class XXFormsDocBase64 extends XFormsFunction {

    private String expressionBaseURI;

    public static final int DOC_BASE64 = 0;
    public static final int DOC_BASE64_AVAILABLE = 1;

    // NOTE: Saxon 9 does this differently, will have to upgrade this when needed
    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        final StringValue result;
        try {
            final StringValue url = (StringValue) argument[0].evaluateItem(context);
            if (url == null) {
                return null;
            }

            result = new StringValue(readFile(url.getStringValue(), expressionBaseURI));
        } catch (XPathException e) {
            if (operation == DOC_BASE64_AVAILABLE) {
                return BooleanValue.FALSE;
            } else {
                throw e;
            }
        }
        if (operation == DOC_BASE64_AVAILABLE) {
            return BooleanValue.TRUE;
        } else {
            return result;
        }
    }

    private CharSequence readFile(String href, String baseURI) throws XPathException {
        try {
            // Obtain a PipelineContext
            final PipelineContext pipelineContext = getOrCreatePipelineContext();

            // Use resolver as it does a series of tasks for us, and use "binary" mode
            final TransformerURIResolver resolver = new TransformerURIResolver(null, pipelineContext, null, false, "binary");

            final FastStringBuffer sb = new FastStringBuffer(1024);

            // Get SAX source using crazy SAX API
            // Source produces a binary document (content Base64-encoded)
            final SAXSource source = (SAXSource) resolver.resolve(href, baseURI);
            final XMLReader xmlReader = source.getXMLReader();
            xmlReader.setContentHandler(new ContentHandlerAdapter() {
                public void characters(char ch[], int start, int length) throws SAXException {
                    // Append Base64-encoded text only
                    sb.append(ch, start, length);
                }
            });
            xmlReader.parse(source.getInputSource());

            // Return content formatted as Base64
            return sb.toString();
        } catch (Exception e) {
            throw new DynamicError(e);
        }
    }
}
