package org.orbeon.oxf.processor.pipeline.foreach;

import org.orbeon.oxf.processor.*;
import org.orbeon.oxf.processor.generator.DOMGenerator;
import org.orbeon.oxf.processor.pipeline.ast.*;
import org.orbeon.oxf.processor.pipeline.PipelineProcessor;
import org.orbeon.oxf.processor.pipeline.TeeProcessor;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import org.orbeon.oxf.xml.EmbeddedDocumentContentHandler;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.util.PooledXPathExpression;
import org.orbeon.oxf.util.XPathCache;
import org.orbeon.oxf.cache.OutputCacheKey;
import org.orbeon.oxf.cache.Cacheable;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.dom4j.DocumentWrapper;
import org.orbeon.saxon.Configuration;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.*;

/**
 * Copyright (C) 2007 Orbeon, Inc.
* <p/>
* This program is free software; you can redistribute it and/or modify it under the terms of the
* GNU Lesser General Public License as published by the Free Software Foundation; either version
* 2.1 of the License, or (at your option) any later version.
* <p/>
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
* <p/>
* The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
*/
public class ConcreteForEachProcessor extends ProcessorImpl {

    private final Processor forEachBlockProcessor;
    private final ProcessorOutput iterationOutput;
    private final String select;
    private final Map namespaceContext;
    private String rootLocalName;
    private String rootQName;
    private String rootNamespaceURI;

    public ConcreteForEachProcessor(ASTForEach forEachAST, Object validity) {
        final String[] refsWithNoId = getRefsWithNoId(forEachAST);
        final String idOrRef = forEachAST.getId() != null ? forEachAST.getId() : forEachAST.getRef();

        // Create pipeline to represent the nested pipeline block within p:for-each
        {
            final ASTPipeline astPipeline = new ASTPipeline();
            astPipeline.setValidity(validity);
            astPipeline.getStatements().addAll(forEachAST.getStatements());
            astPipeline.setNode(forEachAST.getNode());
            for (int i = 0; i < refsWithNoId.length; i++) {
                astPipeline.addParam(new ASTParam(ASTParam.INPUT, refsWithNoId[i]));
                if (!refsWithNoId[i].equals(AbstractForEachProcessor.FOR_EACH_CURRENT_INPUT))
                    addInputInfo(new ProcessorInputOutputInfo(refsWithNoId[i]));
            }
            if (idOrRef != null) {
                astPipeline.addParam(new ASTParam(ASTParam.OUTPUT, idOrRef));
                addOutputInfo(new ProcessorInputOutputInfo(idOrRef));
            }
            if (logger.isDebugEnabled()) {
                final ASTDocumentHandler astDocumentHandler = new ASTDocumentHandler();
                astPipeline.walk(astDocumentHandler);
                logger.debug("Iteration pipeline:\n" + Dom4jUtils.domToString(astDocumentHandler.getDocument()));
            }
            forEachBlockProcessor = new PipelineProcessor(astPipeline);
        }

        // Connect nested pipeline block inputs to inputs of p:for-each processor
        for (int i = 0; i < refsWithNoId.length; i++) {
            if (!refsWithNoId[i].equals(AbstractForEachProcessor.FOR_EACH_CURRENT_INPUT)) {
                final ProcessorInput pipelineInput = forEachBlockProcessor.createInput(refsWithNoId[i]);
                pipelineInput.setOutput(new ForwardingProcessorOutput(refsWithNoId[i]));
            }
        }

        // Connect special "$current" input which produces the document selected by the iteration
        final ProcessorInput iterationInput = forEachBlockProcessor.createInput(AbstractForEachProcessor.FOR_EACH_CURRENT_INPUT);
        final ProcessorOutput currentOutput = new IterationProcessorOutput(AbstractForEachProcessor.class, AbstractForEachProcessor.FOR_EACH_CURRENT_INPUT);
        currentOutput.setInput(iterationInput);
        iterationInput.setOutput(currentOutput);

        // Create output for the iteration
        iterationOutput = forEachBlockProcessor.createOutput(idOrRef);

        select = forEachAST.getSelect();
        namespaceContext = Dom4jUtils.getNamespaceContextNoDefault((Element) forEachAST.getNode());
        if (forEachAST.getRoot() != null) {
            rootQName = forEachAST.getRoot();
            int columnPosition = rootQName.indexOf(':');
            if (columnPosition == -1) {
                // No prefix
                rootLocalName = rootQName;
                rootNamespaceURI = "";
            } else {
                // Extract prefix, find namespace URI
                final String prefix = rootQName.substring(0, columnPosition);
                rootNamespaceURI = (String) namespaceContext.get(prefix);
                if (rootNamespaceURI == null)
                    throw new ValidationException("Prefix '" + prefix + "' used in root attribute is undefined", forEachAST.getLocationData());
                rootLocalName = rootQName.substring(columnPosition + 1);
            }
        }
    }

    public ProcessorOutput createOutput(final String name) {
        final ProcessorOutput output = new ProcessorOutputImpl(getClass(), name) {
            public void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {
                try {
                    final State state = (State) getState(pipelineContext);

                    // Open document
                    contentHandler.startDocument();
                    contentHandler.startElement(rootNamespaceURI, rootLocalName, rootQName, new AttributesImpl());

                    // Read n times from iterationOutput
                    PooledXPathExpression expression = null;
                    int iterationCount = 0;
                    try {
                        expression = createExpression(pipelineContext);

                        for (Iterator i = new ElementIterator(expression); i.hasNext(); iterationCount++) {
                            final Element currentElement = (Element) i.next();

                            // Create DOMGenerator
                            final String systemId = Dom4jUtils.makeSystemId(currentElement);
                            final DOMGenerator domGenerator = new DOMGenerator
                                    (currentElement, "for each input", DOMGenerator.ZeroValidity, systemId);
                            domGenerator.createOutput(OUTPUT_DATA);
                            state.domGenerator = domGenerator;

                            // Run iteration
                            forEachBlockProcessor.reset(pipelineContext);
                            iterationOutput.read(pipelineContext, new EmbeddedDocumentContentHandler(contentHandler));
                        }
                    } catch (XPathException e) {
                        throw new OXFException(e);
                    } finally {
                        // Clear state to allow gc as the state might be referenced for a while
                        if (state != null) state.domGenerator = null;
                        // Return expression
                        if (expression != null) expression.returnToPool();
                    }

                    // Notify input Tee processors that we are done
                    commitInputs(pipelineContext, iterationCount);

                    // Close document
                    contentHandler.endElement(rootNamespaceURI, rootLocalName, rootQName);
                    contentHandler.endDocument();
                } catch (SAXException e) {
                    throw new OXFException(e);
                }
            }

            /**
             * For each is not cacheable. We used to have code here that combines the keys for
             * the outputs of the different executions of the for-each. The problem is when we
             * have a block in the for-each that has an output and a serializer. In that case,
             * getting the output key runs the serializer.
             *
             * So if we get the key here, we'll run the serializer n times, which is
             * unexpected. Maybe an an API that combines reading the output and reading the
             * key/validity would solve this problem.
             */
            protected OutputCacheKey getKeyImpl(PipelineContext pipelineContext) {
                return null;
            }

            /**
             * For each is not cachable. See comment in getKeyImpl().
             */
            protected Object getValidityImpl(PipelineContext pipelineContext) {
                return null;
            }

        };
        addOutput(name, output);
        return output;
    }

    private void commitInputs(PipelineContext pipelineContext, int iterationCount) {
        for (Iterator i = getConnectedInputs().entrySet().iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry) i.next();

            final List inputs = (List) entry.getValue();
            final ProcessorInput input = (ProcessorInput) inputs.get(0);// NOTE: We don't really support multiple inputs with the same name.

            if (!AbstractForEachProcessor.FOR_EACH_DATA_INPUT.equals(input.getName())) {// ignore $data
                final ProcessorOutput output = input.getOutput();
                if (output instanceof TeeProcessor.TeeProcessorOutputImpl) {
                    final TeeProcessor.TeeProcessorOutputImpl teeOutput = (TeeProcessor.TeeProcessorOutputImpl) output;
                    teeOutput.doneReading(pipelineContext);
//                    System.out.println("xxx telling TeeProcessor that we are done reading after " + iterationCount + " iterations for input " + input.getName());
                }
            }
        }
    }

    private class ElementIterator implements Iterator {
        private final Iterator iterator;

        public ElementIterator(PooledXPathExpression expression) throws XPathException {
            this.iterator = expression.iterate();
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Object next() {
            final Object nextObject = iterator.next();
            if (nextObject instanceof Node) {
                final Node nextNode = (Node) nextObject;
                if (nextNode.getNodeType() != Node.ELEMENT_NODE)
                    throw new OXFException("Select expression '" + select
                            + "' did not return a sequence of elements. One node was a '"
                            + nextNode.getNodeTypeName() + "'");
            } else {
                throw new OXFException("Select expression '" + select
                        + "' did not return a sequence of elements. One item was a '"
                        + nextObject.getClass().getName() + "'");
            }

            return nextObject;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private PooledXPathExpression createExpression(PipelineContext pipelineContext) {
        // Read special "$data" input
        final Document dataInput = readInputAsDOM4J(pipelineContext, getInputByName(AbstractForEachProcessor.FOR_EACH_DATA_INPUT));
        return XPathCache.getXPathExpression(pipelineContext,
                new DocumentWrapper(dataInput, null, new Configuration()),
                select, namespaceContext, getLocationData());
    }

    public void start(PipelineContext pipelineContext) {
        final State state = (State) getState(pipelineContext);

        // Read n times from iterationOutput
        PooledXPathExpression expression = null;
        int iterationCount = 0;
        try {
            expression = createExpression(pipelineContext);

            for (Iterator i = new ElementIterator(expression); i.hasNext(); iterationCount++) {
                final Element currentElement = (Element) i.next();

                // Create DOMGenerator
                final String systemId = Dom4jUtils.makeSystemId(currentElement);
                final DOMGenerator domGenerator = new DOMGenerator
                        (currentElement, "for each input", DOMGenerator.ZeroValidity, systemId);
                domGenerator.createOutput(OUTPUT_DATA);
                state.domGenerator = domGenerator;

                // Run iteration
                forEachBlockProcessor.reset(pipelineContext);
                forEachBlockProcessor.start(pipelineContext);
            }

        } catch (XPathException e) {
            throw new OXFException(e);
        } finally {
            // Clear state to allow gc as the state might be referenced for a while
            if (state != null) state.domGenerator = null;
            // Return expression
            if (expression != null) expression.returnToPool();
        }

        // Notify input Tee processors that we are done
        commitInputs(pipelineContext, iterationCount);
    }

    /**
     * Determine all <p:input ref="..."> with no &lt;p:output id="...">.
     * Those are the inputs of this processor.
     */
    private String[] getRefsWithNoId(ASTForEach forEachAST) {
        // All the ids references from <p:input href="...">
        final Set inputRefs = new HashSet();
        // All the ids in <p:output id="...">
        final Set outputIds = new HashSet();

        // Init the 2 sets above
        for (Iterator i = forEachAST.getStatements().iterator(); i.hasNext();) {
            final ASTStatement astStatement = (ASTStatement) i.next();
            final IdInfo idInfo = astStatement.getIdInfo();
            inputRefs.addAll(idInfo.getInputRefs());
            outputIds.addAll(idInfo.getOutputIds());
        }

        final Set refsWithNoId = new HashSet(inputRefs);
        refsWithNoId.removeAll(outputIds);
        return (String[]) refsWithNoId.toArray(new String[refsWithNoId.size()]);
    }

    /**
     * Special processor output which delegates to the p:for-each input
     */
    private class ForwardingProcessorOutput extends ProcessorOutputImpl {
        public ForwardingProcessorOutput(String name) {
            super(ConcreteForEachProcessor.class, name);
        }

        protected void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {
            // Delegate to the p:for-each input
            ConcreteForEachProcessor.this.readInputAsSAX(pipelineContext, getName(), contentHandler);
        }

        protected OutputCacheKey getKeyImpl(PipelineContext pipelineContext) {
            // NOTE: this means that the execution of the pipeline block is not cacheable. Should improve?
            return null;
        }

        protected Object getValidityImpl(PipelineContext pipelineContext) {
            // NOTE: this means that the execution of the pipeline block is not cacheable. Should improve?
            return null;
        }
    }

    /**
     * Reads from the DOM generator stored in state.
     */
    private class IterationProcessorOutput extends ProcessorOutputImpl {

        public IterationProcessorOutput(Class clazz, String name) {
            super(clazz, name);
        }

        protected void readImpl(PipelineContext pipelineContext, ContentHandler contentHandler) {
            final State state = (State) getState(pipelineContext);
            state.domGenerator.getOutputByName(OUTPUT_DATA).read(pipelineContext, contentHandler);
        }

        protected OutputCacheKey getKeyImpl(PipelineContext context) {
            final State state = (State) getState(context);
            return ((Cacheable) state.domGenerator.getOutputByName(OUTPUT_DATA)).getKey(context);
        }

        protected Object getValidityImpl(PipelineContext context) {
            final State state = (State) getState(context);
            return ((Cacheable) state.domGenerator.getOutputByName(OUTPUT_DATA)).getValidity(context);
        }
    }

    /**
     * Runtime state information for p:for-each.
     */
    private static class State {
        DOMGenerator domGenerator;
    }

    public void reset(PipelineContext pipelineContext) {
        setState(pipelineContext, new State());
    }
}
