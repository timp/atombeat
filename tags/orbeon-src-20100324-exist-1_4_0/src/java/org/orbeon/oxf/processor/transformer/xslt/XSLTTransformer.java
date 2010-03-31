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
package org.orbeon.oxf.processor.transformer.xslt;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.orbeon.oxf.cache.CacheKey;
import org.orbeon.oxf.cache.InternalCacheKey;
import org.orbeon.oxf.cache.ObjectCache;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.*;
import org.orbeon.oxf.processor.generator.URLGenerator;
import org.orbeon.oxf.processor.transformer.TransformerURIResolver;
import org.orbeon.oxf.processor.transformer.URIResolverListener;
import org.orbeon.oxf.properties.PropertySet;
import org.orbeon.oxf.properties.PropertyStore;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.util.StringBuilderWriter;
import org.orbeon.oxf.xml.*;
import org.orbeon.oxf.xml.dom4j.ConstantLocator;
import org.orbeon.oxf.xml.dom4j.ExtendedLocationData;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.FeatureKeys;
import org.orbeon.saxon.event.ContentHandlerProxyLocator;
import org.orbeon.saxon.event.Emitter;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.instruct.TerminationException;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.IndependentContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;
import org.xml.sax.*;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.TransformerHandler;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * NOTE: This class requires a re-rooted Saxon to be present. Saxon is used to detect stylesheet dependencies, and the
 * processor also has support for outputting Saxon line numbers. But the processor must remain able to run other
 * transformers like Xalan.
 */
public abstract class XSLTTransformer extends ProcessorImpl {

    private static Logger logger = Logger.getLogger(XSLTTransformer.class);

    public static final String XSLT_URI = "http://www.w3.org/1999/XSL/Transform";
    public static final String XSLT_TRANSFORMER_CONFIG_NAMESPACE_URI = "http://orbeon.org/oxf/xml/xslt-transformer-config";
    public static final String XSLT_PREFERENCES_CONFIG_NAMESPACE_URI = "http://orbeon.org/oxf/xml/xslt-preferences-config";

    private static final String OUTPUT_LOCATION_MODE_PROPERTY = "location-mode";
    private static final String OUTPUT_LOCATION_NONE = "none";
    private static final String OUTPUT_LOCATION_DUMB = "dumb";
    private static final String OUTPUT_LOCATION_SMART = "smart";
    private static final String OUTPUT_LOCATION_MODE_DEFAULT = OUTPUT_LOCATION_NONE;

    // This input determines the JAXP transformer factory class to use
    private static final String INPUT_TRANSFORMER = "transformer";
    // This input determines attributes to set on the TransformerFactory
    private static final String INPUT_ATTRIBUTES = "attributes";

    public XSLTTransformer(String schemaURI) {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG, schemaURI));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_TRANSFORMER, XSLT_TRANSFORMER_CONFIG_NAMESPACE_URI));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_ATTRIBUTES, XSLT_PREFERENCES_CONFIG_NAMESPACE_URI));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_DATA));
        addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
    }

    public ProcessorOutput createOutput(String name) {
        ProcessorOutput output = new ProcessorImpl.CacheableTransformerOutputImpl(getClass(), name) {
            public void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {

                // Get URI references from cache
                final KeyValidity configKeyValidity = getInputKeyValidity(pipelineContext, INPUT_CONFIG);
                final URIReferences uriReferences = getURIReferences(pipelineContext, configKeyValidity);

                // Get transformer from cache
                TemplatesInfo templatesInfo = null;
                if (uriReferences != null) {
                    // FIXME: this won't depend on the transformer input.
                    final KeyValidity stylesheetKeyValidity = createStyleSheetKeyValidity(pipelineContext, configKeyValidity, uriReferences);
                    if (stylesheetKeyValidity != null)
                        templatesInfo = (TemplatesInfo) ObjectCache.instance()
                                .findValid(pipelineContext, stylesheetKeyValidity.key, stylesheetKeyValidity.validity);
                }

                // Get transformer attributes if any
                Map<String, Boolean> attributes = null;
                {
                    // Read attributes input only if connected
                    if (getConnectedInputs().get(INPUT_ATTRIBUTES) != null) {
                        // Read input as an attribute Map and cache it
                        attributes = (Map<String, Boolean>) readCacheInputAsObject(pipelineContext, getInputByName(INPUT_ATTRIBUTES), new CacheableInputReader() {
                            public Object read(PipelineContext context, ProcessorInput input) {
                                final Document preferencesDocument = readInputAsDOM4J(context, input);
                                final PropertyStore propertyStore = new PropertyStore(preferencesDocument);
                                final PropertySet propertySet = propertyStore.getGlobalPropertySet();
                                return propertySet.getObjectMap();
                            }
                        });
                    }
                }

                // Output location mode
                final String outputLocationMode = getPropertySet().getString(OUTPUT_LOCATION_MODE_PROPERTY, OUTPUT_LOCATION_MODE_DEFAULT);
                final boolean isDumbOutputLocation = OUTPUT_LOCATION_DUMB.equals(outputLocationMode);
                final boolean isSmartOutputLocation = OUTPUT_LOCATION_SMART.equals(outputLocationMode);
                if (isSmartOutputLocation) {
                    // Create new HashMap as we don't want to change the one in cache
                    attributes = (attributes == null) ? new HashMap<String, Boolean>() : new HashMap<String, Boolean>(attributes);
                    // Set attributes for Saxon source location
                    attributes.put(FeatureKeys.LINE_NUMBERING, Boolean.TRUE);
                    attributes.put(FeatureKeys.COMPILE_WITH_TRACING, Boolean.TRUE);
                }

                // Create transformer if we did not find one in cache
                if (templatesInfo == null) {
                    // Get transformer configuration
                    final Node config = readCacheInputAsDOM4J(pipelineContext, INPUT_TRANSFORMER);
                    final String transformerClass = XPathUtils.selectStringValueNormalize(config, "/config/class");
                    // Create transformer
                    // NOTE: createTransformer() handles its own exceptions
                    templatesInfo = createTransformer(pipelineContext, transformerClass, attributes);
                }

                // At this point, we have a templatesInfo, so run the transformation
                runTransformer(pipelineContext, contentHandler, templatesInfo, attributes, isDumbOutputLocation, isSmartOutputLocation);
            }

            private void runTransformer(PipelineContext pipelineContext, final ContentHandler contentHandler, TemplatesInfo templatesInfo,
                                        Map<String, Boolean> attributes, final boolean dumbOutputLocation, final boolean smartOutputLocation) {

                StringBuilderWriter saxonStringBuilderWriter = null;
                try {
                    // Create transformer handler and set output writer for Saxon
                    final StringErrorListener errorListener = new StringErrorListener(logger);
                    final TransformerHandler transformerHandler = TransformerUtils.getTransformerHandler(templatesInfo.templates, templatesInfo.transformerClass, attributes);

                    final Transformer transformer = transformerHandler.getTransformer();
                    final TransformerURIResolver transformerURIResolver = new TransformerURIResolver(XSLTTransformer.this, pipelineContext, INPUT_DATA, URLGenerator.DEFAULT_HANDLE_XINCLUDE);
                    transformer.setURIResolver(transformerURIResolver);
                    transformer.setErrorListener(errorListener);
                    if (smartOutputLocation)
                        transformer.setOutputProperty(SaxonOutputKeys.SUPPLY_SOURCE_LOCATOR, "yes");

                    // Create writer for transformation errors
                    saxonStringBuilderWriter = createErrorStringBuilderWriter(transformerHandler);

                    // Fallback location data
                    final LocationData processorLocationData = getLocationData();

                    // Output filter to fix-up SAX stream and handle location data if needed
                    final SAXResult saxResult = new SAXResult(new SimpleForwardingContentHandler(contentHandler) {

                        private Locator inputLocator;
                        private OutputLocator outputLocator;
                        private Stack<LocationData> startElementLocationStack;

                        class OutputLocator implements Locator {

                            private LocationData currentLocationData;

                            public String getPublicId() {
                                return (currentLocationData != null) ? currentLocationData.getPublicID() : inputLocator.getPublicId();
                            }

                            public String getSystemId() {
                                return (currentLocationData != null) ? currentLocationData.getSystemID() : inputLocator.getSystemId();
                            }

                            public int getLineNumber() {
                                return (currentLocationData != null) ? currentLocationData.getLine() : inputLocator.getLineNumber();
                            }

                            public int getColumnNumber() {
                                return (currentLocationData != null) ? currentLocationData.getCol() : inputLocator.getColumnNumber();
                            }

                            public void setLocationData(LocationData locationData) {
                                this.currentLocationData = locationData;
                            }
                        }

                        // Saxon happens to issue such prefix mappings from time to time. Those
                        // cause issues later down the chain, and anyway serialize to incorrect XML
                        // if xmlns:xmlns="..." gets generated. This appears to happen when Saxon
                        // uses the Copy() instruction. It may be that the source is then
                        // incorrect, but we haven't traced this further. It may also simply be a
                        // bug in Saxon.
                        public void startPrefixMapping(String s, String s1) throws SAXException {
                            if ("xmlns".equals(s)) {
                                // TODO: This may be an old Saxon bug which doesn't occur anymore. Try to see if it occurs again.
                                throw new IllegalArgumentException("xmlns");
//                                return;
                            }
                            super.startPrefixMapping(s, s1);
                        }

                        public void setDocumentLocator(final Locator locator) {
                            this.inputLocator = locator;
                            if (smartOutputLocation) {
                                this.outputLocator = new OutputLocator();
                                this.startElementLocationStack = new Stack<LocationData>();
                                super.setDocumentLocator(this.outputLocator);
                            } else if (dumbOutputLocation) {
                                super.setDocumentLocator(this.inputLocator);
                            } else {
                                // NOP: don't set a locator
                            }
                        }

                        public void startDocument() throws SAXException {
                            // Try to set fallback Locator
                            if (((outputLocator != null && outputLocator.getSystemId() == null) || (inputLocator != null && inputLocator.getSystemId() == null))
                                    && processorLocationData != null && dumbOutputLocation) {
                                final Locator locator = new ConstantLocator(processorLocationData);
                                super.setDocumentLocator(locator);
                            }
                            super.startDocument();
                        }

                        public void endDocument() throws SAXException {
                            if (getContentHandler() == null) {
                                // Hack to test if Saxon outputs more than one endDocument() event
                                logger.warn("XSLT transformer attempted to call endDocument() more than once.");
                                return;
                            }
                            super.endDocument();
                        }

                        public void startElement(String uri, String localname, String qName, Attributes attributes) throws SAXException {
                            if (outputLocator != null) {
                                final LocationData locationData = findSourceElementLocationData(uri, localname);
                                outputLocator.setLocationData(locationData);
                                startElementLocationStack.push(locationData);
                                super.startElement(uri, localname, qName, attributes);
                                outputLocator.setLocationData(null);
                            } else {
                                super.startElement(uri, localname, qName, attributes);
                            }
                        }


                        public void endElement(String uri, String localname, String qName) throws SAXException {
                            if (outputLocator != null) {
                                // Here we do a funny thing: since Saxon does not provide location data on endElement(), we use that of startElement()
                                final LocationData locationData = startElementLocationStack.peek();
                                outputLocator.setLocationData(locationData);
                                super.endElement(uri, localname, qName);
                                outputLocator.setLocationData(null);
                                startElementLocationStack.pop();
                            } else {
                                super.endElement(uri, localname, qName);
                            }
                        }

                        public void characters(char[] chars, int start, int length) throws SAXException {
                            if (outputLocator != null) {
                                final LocationData locationData = findSourceCharacterLocationData();
                                outputLocator.setLocationData(locationData);
                                super.characters(chars, start, length);
                                outputLocator.setLocationData(null);
                            } else {
                                super.characters(chars, start, length);
                            }
                        }

                        private LocationData findSourceElementLocationData(String uri, String localname) {
                            if (inputLocator instanceof ContentHandlerProxyLocator) {
                                final Stack stack = ((ContentHandlerProxyLocator) inputLocator).getContextItemStack();

                                for (int i = stack.size() - 1; i >= 0; i--) {
                                    final Item currentItem = (Item) stack.get(i);
                                    if (currentItem instanceof NodeInfo) {
                                        final NodeInfo currentNodeInfo = (NodeInfo) currentItem;
                                        if (currentNodeInfo.getNodeKind() == org.w3c.dom.Document.ELEMENT_NODE
                                                && currentNodeInfo.getLocalPart().equals(localname)
                                                && currentNodeInfo.getURI().equals(uri)) {
                                            // Very probable match...
                                            return new LocationData(currentNodeInfo.getSystemId(), currentNodeInfo.getLineNumber(), -1);
                                        }
                                    }
                                }
                            }
                            return null;
                        }

                        private LocationData findSourceCharacterLocationData() {
                            if (inputLocator instanceof ContentHandlerProxyLocator) {
                                final Stack stack = ((ContentHandlerProxyLocator) inputLocator).getContextItemStack();
                                if (stack != null) {
                                    for (int i = stack.size() - 1; i >= 0; i--) {
                                        final Item currentItem = (Item) stack.get(i);
                                        if (currentItem instanceof NodeInfo) {
                                            final NodeInfo currentNodeInfo = (NodeInfo) currentItem;
    //                                        if (currentNodeInfo.getNodeKind() == org.w3c.dom.Document.TEXT_NODE) {
                                                // Possible match
                                                return new LocationData(currentNodeInfo.getSystemId(), currentNodeInfo.getLineNumber(), -1);
    //                                        }
                                        }
                                    }
                                }
                            }
                            return null;
                        }
                    });

                    if (processorLocationData != null) {
                        final String processorSystemId = processorLocationData.getSystemID();
                        //saxResult.setSystemId(sysID); // NOT SURE WHY WE DID THIS
                        // TODO: use source document system ID, not stylesheet system ID
                        transformerHandler.setSystemId(processorSystemId);
                    }
                    transformerHandler.setResult(saxResult);

                    // Execute transformation
                    try {
                        if (XSLTTransformer.this.getConnectedInputs().size() > 3) {
                            // When other inputs are connected, they can be read
                            // with the doc() function in XSLT. Reading those
                            // documents might happen before the whole input
                            // document is read, which is not compatible with
                            // our processing model. So in this case, we first
                            // read the data in a SAX store.
                            final SAXStore dataSaxStore = new SAXStore();
                            readInputAsSAX(pipelineContext, INPUT_DATA, dataSaxStore);
                            dataSaxStore.replay(transformerHandler);
                        } else {
                            readInputAsSAX(pipelineContext, INPUT_DATA, transformerHandler);
                        }
                    } finally {

                        // Log message from Saxon
                        if (saxonStringBuilderWriter != null) {
                            String message = saxonStringBuilderWriter.toString();
                            if (message.length() > 0)
                                logger.info(message);
                        }

                        // Make sure we don't keep stale references to URI resolver objects
                        transformer.setURIResolver(null);
                        transformerURIResolver.destroy();
                    }

                    // Check whether some errors were added
                    if (errorListener.hasErrors()) {
                        final List errors = errorListener.getErrors();
                        if (errors != null) {
                            ValidationException ve = null;
                            for (Iterator i = errors.iterator(); i.hasNext();) {
                                final LocationData currentLocationData = (LocationData) i.next();

                                if (ve == null)
                                    ve = new ValidationException("Errors while executing transformation", currentLocationData);
                                else
                                    ve.addLocationData(currentLocationData);
                            }
                        }
                    }
                } catch (Exception e) {

                    final Throwable rootCause = ValidationException.getRootThrowable(e);
                    if (rootCause instanceof TransformerException) {
                        final TransformerException transformerException = (TransformerException) rootCause;

                        // Add location data of TransformerException if possible
                        final LocationData locationData =
                                (transformerException.getLocator() != null && transformerException.getLocator().getSystemId() != null)
                                    ? new LocationData(transformerException.getLocator())
                                    : (templatesInfo.systemId != null)
                                        ? new LocationData(templatesInfo.systemId, -1, -1)
                                        : null;

                        if (rootCause instanceof TerminationException) {
                            // Saxon-specific exception thrown by xsl:message terminate="yes"
                            final ValidationException customException = new ValidationException("Processing terminated by xsl:message: " + saxonStringBuilderWriter.toString(), locationData);
                            throw new ValidationException(customException, new ExtendedLocationData(locationData, "executing XSLT transformation"));
                        } else {
                            // Other transformation error
                            throw new ValidationException(rootCause, new ExtendedLocationData(locationData, "executing XSLT transformation"));
                        }
                    } else {
                        // Add template location data if possible
                        final LocationData templatesLocationData = (templatesInfo.systemId != null) ? new LocationData(templatesInfo.systemId, -1, -1) : null;
                        throw ValidationException.wrapException(rootCause, new ExtendedLocationData(templatesLocationData, "executing XSLT transformation"));
                    }
                }
            }

            protected boolean supportsLocalKeyValidity() {
                return true;
            }

            protected CacheKey getLocalKey(PipelineContext context) {
                try {
                    KeyValidity configKeyValidity = getInputKeyValidity(context, INPUT_CONFIG);
                    URIReferences uriReferences = getURIReferences(context, configKeyValidity);
                    if (uriReferences == null || uriReferences.hasDynamicDocumentReferences)
                        return null;
                    List<CacheKey> keys = new ArrayList<CacheKey>();
                    keys.add(configKeyValidity.key);
                    List<URIReference> allURIReferences = new ArrayList<URIReference>();
                    allURIReferences.addAll(uriReferences.stylesheetReferences);
                    allURIReferences.addAll(uriReferences.documentReferences);
                    for (Iterator<URIReference> i = allURIReferences.iterator(); i.hasNext();) {
                        URIReference uriReference = i.next();
                        keys.add(new InternalCacheKey(XSLTTransformer.this, "xsltURLReference", URLFactory.createURL(uriReference.context, uriReference.spec).toExternalForm()));
                    }
                    return new InternalCacheKey(XSLTTransformer.this, keys);
                } catch (MalformedURLException e) {
                    throw new OXFException(e);
                }
            }

            protected Object getLocalValidity(PipelineContext context) {
                try {
                    KeyValidity configKeyValidity = getInputKeyValidity(context, INPUT_CONFIG);
                    URIReferences uriReferences = getURIReferences(context, configKeyValidity);
                    if (uriReferences == null || uriReferences.hasDynamicDocumentReferences)
                        return null;
                    List validities = new ArrayList();
                    validities.add(configKeyValidity.validity);
                    List<URIReference> allURIReferences = new ArrayList<URIReference>();
                    allURIReferences.addAll(uriReferences.stylesheetReferences);
                    allURIReferences.addAll(uriReferences.documentReferences);
                    for (Iterator<URIReference> i = allURIReferences.iterator(); i.hasNext();) {
                        URIReference uriReference = i.next();
                        Processor urlGenerator = new URLGenerator(URLFactory.createURL(uriReference.context, uriReference.spec));
                        validities.add(((ProcessorOutputImpl) urlGenerator.createOutput(OUTPUT_DATA)).getValidity(context));
                    }
                    return validities;
                } catch (IOException e) {
                    throw new OXFException(e);
                }
            }

            private URIReferences getURIReferences(PipelineContext context, KeyValidity configKeyValidity) {
                if (configKeyValidity == null)
                    return null;
                return (URIReferences) ObjectCache.instance().findValid(context, configKeyValidity.key, configKeyValidity.validity);
            }

            private KeyValidity createStyleSheetKeyValidity(PipelineContext context, KeyValidity configKeyValidity, URIReferences uriReferences) {
                try {
                    if (configKeyValidity == null)
                        return null;

                    List<CacheKey> keys = new ArrayList<CacheKey>();
                    List<Object> validities = new ArrayList<Object>();
                    keys.add(configKeyValidity.key);
                    validities.add(configKeyValidity.validity);
                    for (Iterator<URIReference> i = uriReferences.stylesheetReferences.iterator(); i.hasNext();) {
                        URIReference uriReference = i.next();
                        URL url = URLFactory.createURL(uriReference.context, uriReference.spec);
                        keys.add(new InternalCacheKey(XSLTTransformer.this, "xsltURLReference", url.toExternalForm()));
                        Processor urlGenerator = new URLGenerator(url);
                        validities.add(((ProcessorOutputImpl) urlGenerator.createOutput(OUTPUT_DATA)).getValidity(context));//FIXME: can we do better? See URL generator.
                    }

                    return new KeyValidity(new InternalCacheKey(XSLTTransformer.this, keys), validities);
                } catch (MalformedURLException e) {
                    throw new OXFException(e);
                }
            }

            /**
             * Reads the input and creates the JAXP Templates object (wrapped in a Transformer object). While reading
             * the input, figures out the direct dependencies on other files (URIReferences object), and stores these
             * two mappings in cache:
             *
             * configKey        -> uriReferences
             * uriReferencesKey -> transformer
             */
            private TemplatesInfo createTransformer(PipelineContext pipelineContext, String transformerClass, Map<String, Boolean> attributes) {
                StringErrorListener errorListener = new StringErrorListener(logger);
                final StylesheetForwardingContentHandler topStylesheetContentHandler = new StylesheetForwardingContentHandler();
                try {
                    // Create transformer
                    final TemplatesInfo templatesInfo = new TemplatesInfo();
                    final List<StylesheetForwardingContentHandler> xsltContentHandlers = new ArrayList<StylesheetForwardingContentHandler>();
                    {
                        // Create SAXSource adding our forwarding content handler
                        final SAXSource stylesheetSAXSource;
                        {
                            xsltContentHandlers.add(topStylesheetContentHandler);
                            XMLReader xmlReader = new ProcessorOutputXMLReader(pipelineContext, getInputByName(INPUT_CONFIG).getOutput()) {
                                public void setContentHandler(ContentHandler handler) {
                                    super.setContentHandler(new TeeContentHandler(Arrays.asList(topStylesheetContentHandler, handler)));
                                }
                            };
                            stylesheetSAXSource = new SAXSource(xmlReader, new InputSource());
                        }

                        // Put listener in context that will be called by URI resolved
                        pipelineContext.setAttribute(PipelineContext.XSLT_STYLESHEET_URI_LISTENER, new URIResolverListener() {
                            public ContentHandler getContentHandler() {
                                StylesheetForwardingContentHandler contentHandler = new StylesheetForwardingContentHandler();
                                xsltContentHandlers.add(contentHandler);
                                return contentHandler;
                            }
                        });
                        final TransformerURIResolver uriResolver
                                = new TransformerURIResolver(XSLTTransformer.this, pipelineContext, INPUT_DATA, URLGenerator.DEFAULT_HANDLE_XINCLUDE);
                        templatesInfo.templates = TransformerUtils.getTemplates(stylesheetSAXSource, transformerClass, attributes, errorListener, uriResolver);
                        uriResolver.destroy();
                        templatesInfo.transformerClass = transformerClass;
                        templatesInfo.systemId = topStylesheetContentHandler.getSystemId();
                    }

                    // Update cache
                    {
                        // Create uriReferences
                        URIReferences uriReferences = new URIReferences();
                        for (Iterator<StylesheetForwardingContentHandler> i = xsltContentHandlers.iterator(); i.hasNext();) {
                            StylesheetForwardingContentHandler contentHandler = i.next();
                            uriReferences.hasDynamicDocumentReferences = uriReferences.hasDynamicDocumentReferences
                                    || contentHandler.getURIReferences().hasDynamicDocumentReferences;
                            uriReferences.stylesheetReferences.addAll
                                    (contentHandler.getURIReferences().stylesheetReferences);
                            uriReferences.documentReferences.addAll
                                    (contentHandler.getURIReferences().documentReferences);
                        }

                        // Put in cache: configKey -> uriReferences
                        final KeyValidity configKeyValidity = getInputKeyValidity(pipelineContext, INPUT_CONFIG);
                        if (configKeyValidity != null)
                            ObjectCache.instance().add(pipelineContext, configKeyValidity.key, configKeyValidity.validity, uriReferences);

                        // Put in cache: (configKey, uriReferences.stylesheetReferences) -> transformer
                        final KeyValidity stylesheetKeyValidity = createStyleSheetKeyValidity(pipelineContext, configKeyValidity, uriReferences);
                        if (stylesheetKeyValidity != null)
                            ObjectCache.instance().add(pipelineContext, stylesheetKeyValidity.key, stylesheetKeyValidity.validity, templatesInfo);
                    }

                    return templatesInfo;

                } catch (TransformerException e) {
                    if (errorListener.hasErrors()) {
                        // Use error messages information and provide location data of first error
                        final ValidationException validationException = new ValidationException(errorListener.getMessages(), errorListener.getErrors().get(0));
                        // If possible add location of top-level stylesheet
                        if (topStylesheetContentHandler.getSystemId() != null)
                            validationException.addLocationData(new ExtendedLocationData(new LocationData(topStylesheetContentHandler.getSystemId(), -1, -1), "creating XSLT transformer"));
                        throw validationException;
                    } else {
                        // No XSLT errors are available
                        final LocationData transformerExceptionLocationData
                            = StringErrorListener.getTransformerExceptionLocationData(e, topStylesheetContentHandler.getSystemId());
                        if (transformerExceptionLocationData.getSystemID() != null)
                            throw ValidationException.wrapException(e, new ExtendedLocationData(transformerExceptionLocationData, "creating XSLT transformer"));
                        else
                            throw new OXFException(e);
                    }

//                    final ExtendedLocationData extendedLocationData
//                            = StringErrorListener.getTransformerExceptionLocationData(e, topStylesheetContentHandler.getSystemId());
//
//                    final ValidationException ve = new ValidationException(e.getMessage() + " " + errorListener.getMessages(), e, extendedLocationData);
//
//                    // Append location data gathered from error listener
//                    if (errorListener.hasErrors()) {
//                        final List errors = errorListener.getErrors();
//                        if (errors != null) {
//                            for (Iterator i = errors.iterator(); i.hasNext();) {
//                                final LocationData currentLocationData = (LocationData) i.next();
//                                ve.addLocationData(currentLocationData);
//                            }
//                        }
//                    }
//                    throw ve;
                } catch (Exception e) {
                    if (topStylesheetContentHandler.getSystemId() != null) {
                        throw ValidationException.wrapException(e, new ExtendedLocationData(topStylesheetContentHandler.getSystemId(), -1, -1, "creating XSLT transformer"));
                    } else {
                        throw new OXFException(e);
                    }
                }
            }

        };
        addOutput(name, output);
        return output;
    }

    private StringBuilderWriter createErrorStringBuilderWriter(TransformerHandler transformerHandler) throws Exception {
        final String transformerClassName = transformerHandler.getTransformer().getClass().getName();

        // NOTE: 2007-07-05 MK suggests that since we depend on Saxon anyway, we shouldn't use reflection
        // here but directly the Saxon classes to avoid the cost of reflection.

        StringBuilderWriter saxonStringBuilderWriter = null;
        if (transformerClassName.equals("org.orbeon.saxon.Controller")) {
            // Built-in Saxon transformer
            saxonStringBuilderWriter = new StringBuilderWriter();
            final Controller saxonController = (Controller) transformerHandler.getTransformer();
            // NOTE: Saxon 9 returns a Receiver (MessageEmitter -> XMLEmitter -> Emitter -> Receiver)
            Emitter messageEmitter = saxonController.getMessageEmitter();
            if (messageEmitter == null) {
                // NOTE: Saxon 9 makes this method private, use setMessageEmitter() instead
                messageEmitter = saxonController.makeMessageEmitter();
            }
            messageEmitter.setWriter(saxonStringBuilderWriter);
        } else if (transformerClassName.equals("net.sf.saxon.Controller")) {
            // A Saxon transformer, we don't know which version
            saxonStringBuilderWriter = new StringBuilderWriter();
            final Transformer saxonController = transformerHandler.getTransformer();
            final Method getMessageEmitter = saxonController.getClass().getMethod("getMessageEmitter");
            Object messageEmitter = getMessageEmitter.invoke(saxonController);
            if (messageEmitter == null) {
                // Try to set a Saxon MessageEmitter

                final String messageEmitterClassName = "net.sf.saxon.event.MessageEmitter";
                final Class messageEmitterClass = Class.forName(messageEmitterClassName);
                messageEmitter = messageEmitterClass.newInstance();

                final Class receiverClass = Class.forName("net.sf.saxon.event.Receiver");
                final Method setMessageEmitter = saxonController.getClass().getMethod("setMessageEmitter", receiverClass);
                setMessageEmitter.invoke(saxonController, messageEmitter);
            }
            final Method setWriter = messageEmitter.getClass().getMethod("setWriter", new Class[]{Writer.class});
            setWriter.invoke(messageEmitter, saxonStringBuilderWriter);
        }
        return saxonStringBuilderWriter;
    }

    /**
     * This forwarding content handler intercepts all the references to external
     * resources from the XSLT stylesheet. There can be external references in
     * an XSLT stylesheet when the &lt;xsl:include&gt; or &lt;xsl:import&gt;
     * elements are used, or when there is an occurrence of the
     * <code>document()</code> function in an XPath expression.
     *
     * @see #getURIReferences()
     */
    private static class StylesheetForwardingContentHandler extends ForwardingContentHandler {

        /**
         * This is context that will resolve any prefix, function, and variable.
         * It is just used to parse XPath expression and get an AST.
         */
        private IndependentContext dummySaxonXPathContext;
        private final NamePool namePool = new NamePool();

        private void initDummySaxonXPathContext() {
            final Configuration config = new Configuration();
            config.setHostLanguage(Configuration.XSLT);
            config.setNamePool(namePool);
            dummySaxonXPathContext = new IndependentContext(config) {
                {
                    // Dummy Function lib that accepts any name
                    setFunctionLibrary(new FunctionLibrary() {
                        public Expression bind(final int nameCode, String uri, String local, final Expression[] staticArgs)  {

                            // TODO: Saxon 9.0 expressions should test "instanceof StringValue" to "instanceof StringLiteral"
                            if ((XMLConstants.XPATH_FUNCTIONS_NAMESPACE_URI.equals(uri) || "".equals(uri))
                                    && ("doc".equals(local) || "document".equals(local))
                                    && (staticArgs != null && staticArgs.length > 0)) {

                                if (staticArgs[0] instanceof StringValue) {
                                    // Found doc() or document() function which contains a static string
                                    final String literalURI = ((StringValue) staticArgs[0]).getStringValue();

                                    // We don't need to worry here about reference to the processor inputs
                                    if (!isProcessorInputScheme(literalURI)) {
                                        final URIReference uriReference = new URIReference();
                                        uriReference.context = systemId;
                                        uriReference.spec = literalURI;
                                        uriReferences.documentReferences.add(uriReference);
                                    }

                                } else {
                                    // Found doc() or document() function which contains something more complex
                                    uriReferences.hasDynamicDocumentReferences = true;
                                }
                            }

                            // NOTE: We used to return new FunctionCall() here, but MK says EmptySequence.getInstance() will work.
                            // TODO: Check if this works in Saxon 9.0. It doesn't work in 8.8, so for now we keep return new FunctionCall().
//                            return EmptySequence.getInstance();
                            return new ContextItemExpression();
                        }

                        public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
                            return true;
                        }

                        public FunctionLibrary copy() {
                            return this;
                        }
                    });


                }

                public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
                    return true;
                }

                public String getURIForPrefix(String prefix) {
                    return namespaces.getURI(prefix);
                }

                public boolean isImportedSchema(String namespace) { return true; }

                // Dummy var decl to allow any name
                public VariableReference bindVariable(final int fingerprint) {
                        return new VariableReference(new VariableDeclaration() {
                            public void registerReference(BindingReference bindingReference) {
                            }

                            public int getNameCode() {
                                return fingerprint;
                            }

                            public String getVariableName() {
                                return "dummy";
                            }
                        });
                }
            };
        }

        private Locator locator;
        private URIReferences uriReferences = new URIReferences();
        private String systemId;
        private final NamespaceSupport3 namespaces = new NamespaceSupport3();

        public StylesheetForwardingContentHandler() {
            super();
            initDummySaxonXPathContext();
        }

//        public StylesheetForwardingContentHandler(ContentHandler contentHandler) {
//            super(contentHandler);
//            initDummySaxonXPathContext();
//        }

        public URIReferences getURIReferences() {
            return uriReferences;
        }

        public String getSystemId() {
            return systemId;
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
            super.setDocumentLocator(locator);
        }


        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            namespaces.startPrefixMapping(prefix, uri);
            super.startPrefixMapping(prefix, uri);
        }

        public void startElement(String uri, String localname, String qName, Attributes attributes) throws SAXException {
            namespaces.startElement();
            // Save system id
            if (systemId == null && locator != null)
                systemId = locator.getSystemId();

            // Handle possible include
            if (XSLT_URI.equals(uri)) {

                // <xsl:include> or <xsl:import>
                if ("include".equals(localname) || "import".equals(localname)) {
                    final String href = attributes.getValue("href");
                    final URIReference uriReference = new URIReference();
                    uriReference.context = systemId;
                    uriReference.spec = href;
                    uriReferences.stylesheetReferences.add(uriReference);
                } else if ("import-schema".equals(localname)) {
                    final String schemaLocation = attributes.getValue("schema-location");// NOTE: We ignore the @namespace attribute for now
                    final URIReference uriReference = new URIReference();
                    uriReference.context = systemId;
                    uriReference.spec = schemaLocation;
                    uriReferences.stylesheetReferences.add(uriReference);
                }

                // Find XPath expression on current element
                String xpathString;
                {
                    xpathString = attributes.getValue("test");
                    if (xpathString == null)
                        xpathString = attributes.getValue("select");
                }

                // Analyze XPath expression to find dependencies on URIs
                if (xpathString != null) {
                    try {
                        // First, test that one of the strings is present so we don't have to parse unnecessarily

                        // NOTE: 2007-07-05 MK says that there can be spaces and comments between the "doc" and the
                        // "(". Suggestion: "One possibility here if you want to avoid parsing the expression
                        // unnecessarily is to run it through the lexer (net.sf.saxon.expr.Tokenizer). You can just look
                        // at the stream of tokens and look for doc (or a lexical QName whose local part is doc)
                        // followed by "("."

                        // For now, we will probably have many false positive but we just test on "doc". The exact match
                        // is done by parsing the expression below anyway.
                        final boolean containsDocString = xpathString.indexOf("doc") != -1;
                        if (containsDocString) {
                            // The following will call our FunctionLibrary.bind() method, which we use to test for the
                            // presence of the functions.
                            ExpressionTool.make(xpathString, dummySaxonXPathContext, 0, -1, 0);

                            // NOTE: *If* we wanted to use Saxon to parse the whole Stylesheet:
                            
                            // MK: "In Saxon 9.0 there's a method explain() on PreparedStylesheet that writes an XML
                            // representation of the compiled stylesheet to a user-supplied Receiver as a sequence of
                            // events. You could call this with your own Receiver and just watch for the events
                            // representing <functionCall name="doc"><literal>...</literal></functionCall>. But this
                            // depends on compiling the stylesheet first.

                        }
                    } catch (XPathException e) {
                        logger.error("Original exception", e);
                        throw new ValidationException("XPath syntax exception (" + e.getMessage() + ") for expression: "
                                + xpathString, new LocationData(locator));
                    }
                }
            }
            super.startElement(uri, localname, qName, attributes);
        }


        public void endElement(String uri, String localname, String qName) throws SAXException {
            super.endElement(uri, localname, qName);
            namespaces.endElement();
        }

        public void endDocument() throws SAXException {
            super.endDocument();
        }

        public void startDocument() throws SAXException {
            super.startDocument();
        }
    }

    private static class URIReference {
        public String context;
        public String spec;
    }

    private static class URIReferences {
        public List<URIReference> stylesheetReferences = new ArrayList<URIReference>();
        public List<URIReference> documentReferences = new ArrayList<URIReference>();

        /**
         * Is true if and only if an XPath expression with a call to the
         * <code>document()</code> function was found and the value of the
         * attribute to the <code>document()</code> function call cannot be
         * determined without executing the stylesheet. When this happens, the
         * result of the stylesheet execution cannot be cached.
         */
        public boolean hasDynamicDocumentReferences = false;
    }

    private static class TemplatesInfo {
        public Templates templates;
        public String transformerClass;
        public String systemId;
    }
}
