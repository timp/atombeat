/**
 *  Copyright (C) 2005 Orbeon, Inc.
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
package org.orbeon.oxf.processor.converter;

import org.apache.commons.fileupload.FileItem;
import org.dom4j.Element;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.*;
import org.orbeon.oxf.processor.generator.URLGenerator;
import org.orbeon.oxf.processor.serializer.BinaryTextContentHandler;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.xml.XMLUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * This converter converts from a binary document or a text document to a parsed XML document.
 */
public class ToXMLConverter extends ProcessorImpl {
    public ToXMLConverter() {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_DATA));
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG));
        addOutputInfo(new ProcessorInputOutputInfo(OUTPUT_DATA));
    }

    public ProcessorOutput createOutput(String name) {
        ProcessorOutput output = new ProcessorImpl.CacheableTransformerOutputImpl(getClass(), name) {
            public void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {

                // Read config input
                final Config config = (Config) readCacheInputAsObject(pipelineContext, getInputByName(INPUT_CONFIG), new CacheableInputReader() {
                    public Object read(org.orbeon.oxf.pipeline.api.PipelineContext context, ProcessorInput input) {
                        final Config result = new Config();

                        final Element configElement = readInputAsDOM4J(context, input).getRootElement();

                        // Validation setting
                        result.validating = ProcessorUtils.selectBooleanValue(configElement, "/config/validating", URLGenerator.DEFAULT_VALIDATING);

                        // XInclude handling
                        result.handleXInclude = ProcessorUtils.selectBooleanValue(configElement, "/config/handle-xinclude", URLGenerator.DEFAULT_HANDLE_XINCLUDE);

                        return result;
                    }
                });

                try {
                    // Get FileItem
                    final FileItem fileItem = NetUtils.prepareFileItem(pipelineContext, NetUtils.REQUEST_SCOPE);

                    // TODO: Can we avoid writing to a FileItem?

                    // Read to OutputStream
                    readInputAsSAX(pipelineContext, INPUT_DATA, new BinaryTextContentHandler(null, fileItem.getOutputStream(), true, false, null, false, false, null, false));

                    // Create parser
                    final XMLReader reader = XMLUtils.newXMLReader(config.validating, config.handleXInclude);

                    // Run parser on InputStream
                    //inputSource.setSystemId();
                    reader.setContentHandler(contentHandler);
                    reader.parse(new InputSource(fileItem.getInputStream()));

                } catch (Exception e) {
                    throw new OXFException(e);
                }
            }
        };
        addOutput(name, output);
        return output;
    }

    private static class Config {
        public boolean validating;
        public boolean handleXInclude;
    }
}
