/**
 *  Copyright (C) 2004 Orbeon, Inc.
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
package org.orbeon.oxf.processor.transformer;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.Processor;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.generator.URLGenerator;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.xml.ForwardingXMLReader;
import org.orbeon.oxf.xml.ProcessorOutputXMLReader;
import org.orbeon.oxf.xml.TeeContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * URI resolver used by transformation processors, including XSLT, XQuery, and XInclude.
 *
 * This resolver is able to handle "input:*" URLs as well as regular URLs.
 */
public class TransformerURIResolver implements URIResolver {

    private ProcessorImpl processor;
    private PipelineContext pipelineContext;
    private String prohibitedInput;
    private boolean handleXInclude;
    private String mode;

    /**
     * Create a URI resolver.
     *
     * @param processor         processor of which inputs will be read for "input:*"
     * @param pipelineContext   pipeline context
     * @param prohibitedInput   name of an input which triggers and exception if read (usually "data" or "config")
     * @param handleXInclude    true if, when reading a regular URL (i.e. not "input:*"), XInclude processing must be done by the parser
     */
    public TransformerURIResolver(ProcessorImpl processor, PipelineContext pipelineContext, String prohibitedInput, boolean handleXInclude) {
        this(processor, pipelineContext, prohibitedInput, handleXInclude, null);
    }

    /**
     * Create a URI resolver with a mode.
     *
     * @param processor         processor of which inputs will be read for "input:*"
     * @param pipelineContext   pipeline context
     * @param prohibitedInput   name of an input which triggers and exception if read (usually "data" or "config")
     * @param handleXInclude    true if, when reading a regular URL (i.e. not "input:*"), XInclude processing must be done by the parser
     * @param mode              "xml", "html", "text" or "binary"
     */
    public TransformerURIResolver(ProcessorImpl processor, PipelineContext pipelineContext, String prohibitedInput, boolean handleXInclude, String mode) {
        this.processor = processor;
        this.pipelineContext = pipelineContext;
        this.prohibitedInput = prohibitedInput;
        this.handleXInclude = handleXInclude;
        this.mode = mode;
    }

    /**
     * Create a URI resolver. Use this constructor when using this from outside a processor.
     *
     * @param handleXInclude    true if, when reading a regular URL (i.e. not "input:*"), XInclude processing must be done by the parser
     */
    public TransformerURIResolver(boolean handleXInclude) {
        this(null, new PipelineContext(), null, handleXInclude);
    }

    public Source resolve(String href, String base) throws TransformerException {
        try {
            // Create XML reader for URI
            final String systemId;
            XMLReader xmlReader;
            {
                final String inputName = ProcessorImpl.getProcessorInputSchemeInputName(href);
                if (prohibitedInput != null && prohibitedInput.equals(inputName)) {
                    // Don't allow a prohibited input (usually INPUT_DATA) to be read this way. We do this to prevent that input to read twice from XSLT.
                    throw new OXFException("Can't read '" + prohibitedInput + "' input. If you are calling this from XSLT, use a '/' expression in XPath instead.");
                } else if (inputName != null) {
                    // Resolve to input of current processor
                    if (processor == null)
                        throw new OXFException("Can't read URL '" + href + "'.");

                    xmlReader = new ProcessorOutputXMLReader(pipelineContext, processor.getInputByName(inputName).getOutput());
                    systemId = href;
                } else {
                    // Resolve to regular URI
                    final URL url = URLFactory.createURL(base, href);
                    // NOTE: below, we disable use of the URLGenerator's local cache, so that we don't check validity
                    // with HTTP and HTTPS. When would it make sense to use local caching?
                    final String protocol = url.getProtocol();
                    final boolean cacheUseLocalCache = !(protocol.equals("http") || protocol.equals("https"));
                    final Processor urlGenerator = new URLGenerator(url, null, false, null, false, false, false, handleXInclude, mode, null, null, cacheUseLocalCache);
                    xmlReader = new ProcessorOutputXMLReader(pipelineContext, urlGenerator.createOutput(ProcessorImpl.OUTPUT_DATA));
                    systemId = url.toExternalForm();
                }
            }

            // Also send data to listener, if there is one
            final URIResolverListener uriResolverListener =
                    (URIResolverListener) pipelineContext.getAttribute(PipelineContext.XSLT_STYLESHEET_URI_LISTENER);
            if (uriResolverListener != null) {
                xmlReader = new ForwardingXMLReader(xmlReader) {

                    private ContentHandler originalHandler;

                    public void setContentHandler(ContentHandler handler) {
                        originalHandler = handler;
                        List contentHandlers = Arrays.asList(new Object[]{uriResolverListener.getContentHandler(), handler});
                        super.setContentHandler(new TeeContentHandler(contentHandlers));
                    }

                    public ContentHandler getContentHandler() {
                        return originalHandler;
                    }
                };
            }

            // Create SAX Source based on XML Reader
            return new SAXSource(xmlReader, new InputSource(systemId)); // set system id so that we can get it on the Source object from outside

        } catch (IOException e) {
            throw new OXFException(e);
        }
    }

    protected ProcessorImpl getProcessor() {
        return processor;
    }

    protected PipelineContext getPipelineContext() {
        return pipelineContext;
    }

    protected boolean isHandleXInclude() {
        return handleXInclude;
    }

    /**
     * Make sure this resolver no longer keeps references to foreign objects.
     *
     * This is useful when a resolver is used for example by a Saxon PreparedStylesheet and we can't remove the
     * reference PreparedStylesheet has on the resolver.
     */
    public void destroy() {
        this.processor = null;
        this.pipelineContext = null;
        this.prohibitedInput = null;
    }
}
