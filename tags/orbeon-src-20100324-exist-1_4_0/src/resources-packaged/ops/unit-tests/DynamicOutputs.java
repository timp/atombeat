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
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.ProcessorOutput;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.xml.ForwardingContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class DynamicOutputs extends ProcessorImpl {

    public DynamicOutputs() {
    }

    public void start(PipelineContext pipelineContext) {
    }

    public ProcessorOutput createOutput(final String name) {

        ProcessorOutput output = new ProcessorImpl.ProcessorOutputImpl(getClass(), name) {
            public void readImpl(PipelineContext context, ContentHandler contentHandler) {
                readInputAsSAX(context, "my-input", new ForwardingContentHandler(contentHandler) {
                    private int level = 0;
                    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
                        if (level++ == 0) {
                            super.startElement("", name, name, new AttributesImpl());
                        } else {
                            super.startElement(namespaceURI, localName, qName, atts);
                        }
                    }

                    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
                        if (--level ==  0) {
                            super.endElement("", name, name);
                        } else
                            super.endElement(namespaceURI, localName, qName);
                    }
                });
            }
        };

        addOutput(name, output);
        return output;
    }
}