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
package org.orbeon.oxf.processor.pipeline;

import org.dom4j.*;
import org.orbeon.oxf.cache.OutputCacheKey;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.common.ValidationException;
import org.orbeon.oxf.debugger.api.Breakpoint;
import org.orbeon.oxf.debugger.api.BreakpointKey;
import org.orbeon.oxf.debugger.api.Debuggable;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.*;
import org.orbeon.oxf.processor.generator.DOMGenerator;
import org.orbeon.oxf.processor.pipeline.ast.*;
import org.orbeon.oxf.processor.pipeline.choose.AbstractChooseProcessor;
import org.orbeon.oxf.processor.pipeline.choose.ConcreteChooseProcessor;
import org.orbeon.oxf.processor.pipeline.foreach.AbstractForEachProcessor;
import org.orbeon.oxf.processor.pipeline.foreach.ConcreteForEachProcessor;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.util.PipelineUtils;
import org.orbeon.oxf.xml.SchemaRepository;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import org.orbeon.oxf.xml.dom4j.ExtendedLocationData;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.xml.sax.ContentHandler;

import java.net.MalformedURLException;
import java.util.*;

/**
 * <b>Lifecycle</b>
 * <ol>
 * <li>Call createInput and createOutput methods to connect the pipeline
 * processor to its config in any order. No verification is done at
 * this point.
 * <li>refreshSocketInfo() can be called at any point. If the config is set,
 * it will read the config and update the socket info.
 * <li>ASTWhen start() is called, the processor is really executed: each
 * processor at the end of the pipeline is started and the outputs
 * of those processors is stored in the SAXStore.
 * <li>ASTWhen a read() is called on an output: if the processors has not
 * been started yet, the start method is called. Then the
 * corresponding SAXStore is replayed.
 * </ol>
 * <p/>
 * <b>Threading</b>
 * <p>This processor is not only not thread safe, but it can't even be
 * reused: if there is one data output (with a 1 cardinality), one can't call
 * read multiple times and get the same result. Only the first call to read
 * on the data output will succeed.
 */
public class PipelineProcessor extends ProcessorImpl implements Debuggable {

    public static final String PIPELINE_NAMESPACE_URI = "http://www.orbeon.com/oxf/pipeline";
    public static final Namespace PIPELINE_NAMESPACE = new Namespace("p", PIPELINE_NAMESPACE_URI);
    private PipelineConfig configFromAST;

    public PipelineProcessor() {
        addInputInfo(new ProcessorInputOutputInfo(INPUT_CONFIG, PIPELINE_NAMESPACE_URI));
    }

    public PipelineProcessor(PipelineConfig pipelineConfig) {
        configFromAST = pipelineConfig;
    }

    public PipelineProcessor(ASTPipeline astPipeline) {
        this(createConfigFromAST(astPipeline));
    }

    public ProcessorOutput createOutput(final String name) {

        ProcessorOutput output = new ProcessorImpl.ProcessorOutputImpl(getClass(), name) {

            public void readImpl(final PipelineContext context, final ContentHandler contentHandler) {
                final ProcessorInput bottomInput = getInput(context);
                if (bottomInput.getOutput() == null)
                    throw new ValidationException("Pipeline output '" + name +
                            "' is not connected to a processor output in pipeline",
                            PipelineProcessor.this.getLocationData());
                executeChildren(context, new Runnable() {
                    public void run() {
                        readInputAsSAX(context, bottomInput, contentHandler);
                    }
                });
            }

            /**
             * If the config is already in cache and the pipeline constructed,
             * we return the key of the bottomInput corresponding to this
             * output.
             */
            public OutputCacheKey getKeyImpl(final PipelineContext context) {
                if (configFromAST == null && !isInputInCache(context, INPUT_CONFIG))
                    return null;
                final ProcessorInput bottomInput = getInput(context);
                final OutputCacheKey[] bottomInputKey = new OutputCacheKey[1];
                executeChildren(context, new Runnable() {
                    public void run() {
                        bottomInputKey[0] = (bottomInput != null)
                                ? getInputKey(context, bottomInput) : null;
                    }
                });
                return bottomInputKey[0];
            }

            /**
             * Similar to getKey (above), but for the validity.
             */
            public Object getValidityImpl(final PipelineContext context) {
                if (configFromAST == null && !isInputInCache(context, INPUT_CONFIG))
                    return null;
                final ProcessorInput bottomInput = getInput(context);
                final Object[] bottomInputValidity = new Object[1];
                executeChildren(context, new Runnable() {
                    public void run() {
                        bottomInputValidity[0] = (bottomInput != null)
                                ? getInputValidity(context, bottomInput) : null;
                    }
                });
                return bottomInputValidity[0];
            }

            private ProcessorInput getInput(PipelineContext context) {
                State state = (State) getState(context);
                if (!state.started)
                    start(context);
                final ProcessorInput bottomInput = (ProcessorInput) state.nameToBottomInputMap.get( name );
                if (bottomInput == null) {
                    throw new ValidationException("There is no <param type=\"output\" name=\""
                            + name + "\"/>", getLocationData());
                }
                return bottomInput;
            }
        };
        addOutput(name, output);
        final BreakpointKey bptKey
            = configFromAST == null ? null : configFromAST.getOutputBreakpointKey( name );
        if ( bptKey != null ) {
            output.setBreakpointKey( bptKey );
        }
        return output;
    }

    public static PipelineConfig createConfigFromAST(ASTPipeline astPipeline) {

        // Perform sanity check on the connection in the pipeline
        astPipeline.getIdInfo();

        // Create new configuration object
        PipelineConfig config = new PipelineConfig();
        PipelineBlock block = new PipelineBlock();

        // Create socket info for each param
        for (Iterator i = astPipeline.getParams().iterator(); i.hasNext();) {
            ASTParam param = (ASTParam) i.next();

            // Create internal top output/bottom input for this param
            if (param.getType() == ASTParam.INPUT) {
                InternalTopOutput internalTopOutput = new InternalTopOutput(param.getName(), param.getLocationData());
                block.declareOutput(param.getNode(), param.getName(), internalTopOutput);
                config.declareTopOutput(param.getName(), internalTopOutput);
                setDebugAndSchema(internalTopOutput, param);
            } else {
                ProcessorInput internalBottomInput = new InternalBottomInput(param.getName());
                block.declareBottomInput(param.getNode(), param.getName(), internalBottomInput);
                config.declareBottomInput(param.getName(), internalBottomInput);
                setDebugAndSchema(internalBottomInput, param);
            }

            // Create socket
            // FIXME: when we implement the full delegation model, we'll have
            // here to create of pass the input/output information.
        }

        // Internally connect all processors / choose / for-each
        for (Iterator i = astPipeline.getStatements().iterator(); i.hasNext();) {
            Object statement = i.next();
            Processor processor = null;
            boolean foundOutput = false;

            if (statement instanceof ASTProcessorCall) {
                ASTProcessorCall processorCall = (ASTProcessorCall) statement;

                final LocationData processorLocationData = processorCall.getLocationData();
                final String processorNameOrURI = (processorCall.getName() != null ? Dom4jUtils.qNameToExplodedQName(processorCall.getName()) : processorCall.getURI());

                if (processorCall.getEncapsulation() == null) {
                    // Direct call
                    if (processorCall.getProcessor() == null) {
                        ProcessorFactory processorFactory = ProcessorFactoryRegistry.lookup(processorCall.getName());
                        if (processorFactory == null)
                            processorFactory = ProcessorFactoryRegistry.lookup(processorCall.getURI());
                        if (processorFactory == null) {
                            throw new ValidationException("Cannot find processor factory with name \"" + processorNameOrURI + "\"", processorLocationData);
                        }
                        processor = processorFactory.createInstance();
                    } else {
                        processor = processorCall.getProcessor();
                    }
                } else if ("ejb".equals(processorCall.getEncapsulation())) {
                    // Call through EJB proxy
                    ProxyProcessor proxyProcessor = new ProxyProcessor();
                    proxyProcessor.setJNDIName(processorCall.getURI());
                    proxyProcessor.setInputs(processorCall.getInputs());
                    proxyProcessor.setOutputs(processorCall.getOutputs());
                    processor = proxyProcessor.createInstance();
                }

                // Set info on processor
                processor.setId(processorCall.getId());
                processor.setLocationData(new ExtendedLocationData(processorLocationData,
                        "executing processor", (Element) processorCall.getNode(),
                        new String[] { "name", processorNameOrURI }, true));

                // Process outputs
                for (Iterator j = processorCall.getOutputs().iterator(); j.hasNext();) {
                    foundOutput = true;
                    ASTOutput output = (ASTOutput) j.next();
                    final String nm = output.getName();
                    if ( nm == null )
                        throw new OXFException("Name attribute is mandatory on output");
                    final String id = output.getId();
                    final String ref = output.getRef();
                    if ( id == null && ref == null )
                        throw new OXFException("Either one of id or ref must be specified on output " + nm );

                    final LocationData locDat = output.getLocationData();
                    final BreakpointKey bptKey
                        = locDat == null ? null : new BreakpointKey( locDat );
                    if ( bptKey != null ) config.setOutputBreakpointKey( id == null ? ref : id, bptKey );

                    ProcessorOutput pout = processor.createOutput( nm );
                    if ( id != null)
                        block.declareOutput(output.getNode(), id, pout);
                    if ( ref != null)
                        block.connectProcessorToBottomInput
                                (output.getNode(), nm, ref, pout);
                    setDebugAndSchema(pout, output);
                    setBreakpointKey(pout, output);
                }

                // Make sure at least one of the outputs is connected
                if (!foundOutput && processor.getOutputsInfo().size() > 0)
                    throw new ValidationException("The processor output must be connected", processorLocationData);

                // Process inputs
                for (Iterator j = processorCall.getInputs().iterator(); j.hasNext();) {
                    ASTInput input = (ASTInput) j.next();

                    final ProcessorInput pin;
                    LocationData inputLocationData = input.getLocationData();
                    if (input.getHref() != null && input.getTransform() == null) {
                        // We just reference a URI
                        pin = block.connectProcessorToHref(input.getNode(), processor, input.getName(), input.getHref());
                    } else {
                        // We have some inline XML in the <input> tag
                        final Node inlineNode = input.getContent();

                        // Create inline document
                        final Document inlineDocument;
                        {
                            final int nodeType = inlineNode.getNodeType();
                            if (nodeType == Node.ELEMENT_NODE) {
                                final Element element = (Element) inlineNode;
                                inlineDocument = Dom4jUtils.createDocumentCopyParentNamespaces(element);
                            } else if (nodeType == Node.DOCUMENT_NODE) {
                                inlineDocument = (Document) inlineNode;
                            } else {
                                throw new OXFException("Invalid type for inline document: " + inlineNode.getClass().getName());
                            }
                        }

                        // Create generator for the inline document
                        final DOMGenerator domGenerator;
                        {
                            final Object validity = astPipeline.getValidity();
                            final LocationData pipelineLocationData = astPipeline.getLocationData();
                            String systemId = (pipelineLocationData == null) ? DOMGenerator.DefaultContext : pipelineLocationData.getSystemID();
                            if (systemId == null)
                                systemId = DOMGenerator.DefaultContext;
                            domGenerator = PipelineUtils.createDOMGenerator(inlineDocument, "inline input", validity, systemId);
                        }

                        final ProcessorOutput domProcessorDataOutput = domGenerator.createOutput(OUTPUT_DATA);

                        // Check if there is an inline transformation
                        final QName transform = input.getTransform();
                        if (transform != null) {
                            //XPathUtils.selectBooleanValue(inlineDocument, "/*/@*[local-name() = 'version' and namespace-uri() = 'http://www.w3.org/1999/XSL/Transform'] = '2.0'").booleanValue()
                            // Instanciate processor
                            final Processor transformProcessor;
                            {
                                final ProcessorFactory processorFactory = ProcessorFactoryRegistry.lookup(transform);
                                if (processorFactory == null) {
                                    throw new ValidationException("Cannot find processor factory with JNDI name \""
                                            + processorCall.getURI() + "\"", inputLocationData);
                                }
                                transformProcessor = processorFactory.createInstance();
                            }

                            // Set info on processor
                            //processor.setId(processorCall.getId()); // what id, if any?
                            transformProcessor.setLocationData(inputLocationData);

                            // Connect config input
                            final ProcessorInput transformConfigInput = transformProcessor.createInput(INPUT_CONFIG);
                            domProcessorDataOutput.setInput(transformConfigInput);
                            transformConfigInput.setOutput(domProcessorDataOutput);

                            // Connect transform processor data input
                            pin = block.connectProcessorToHref(input.getNode(), transformProcessor, INPUT_DATA, input.getHref());

                            // Connect transform processor data output
                            final ProcessorOutput transformDataOutput = transformProcessor.createOutput(OUTPUT_DATA);
                            final ProcessorInput processorDataInput = processor.createInput(input.getName());
                            transformDataOutput.setInput(processorDataInput);
                            processorDataInput.setOutput(transformDataOutput);
                        } else {
                            // It is regular static text: connect directly
                            pin = processor.createInput(input.getName());
                            domProcessorDataOutput.setInput(pin);
                            pin.setOutput(domProcessorDataOutput);
                        }
                    }
                    setDebugAndSchema(pin, input);
                    setBreakpointKey(pin, input);
                }

            } else if (statement instanceof ASTChoose) {

                // Instantiate processor
                ASTChoose choose = (ASTChoose) statement;
                AbstractProcessor chooseAbstractProcessor = new AbstractChooseProcessor(choose, astPipeline.getValidity());
                ConcreteChooseProcessor chooseProcessor =
                        (ConcreteChooseProcessor) chooseAbstractProcessor.createInstance();
                processor = chooseProcessor;

                // Connect special $data input (document on which the decision is made, or iterated on)
                ProcessorInput pin = block.connectProcessorToHref(choose.getNode(), processor,
                        AbstractChooseProcessor.CHOOSE_DATA_INPUT, choose.getHref());
                setDebugAndSchema(pin, choose);

                // Go through inputs/outputs and connect to the rest of the pipeline
                for (Iterator j = processor.getInputsInfo().iterator(); j.hasNext();) {
                    // We reference a previously declared output
                    String inputName = ((ProcessorInputOutputInfo) j.next()).getName();
                    if (!inputName.equals(AbstractChooseProcessor.CHOOSE_DATA_INPUT)) {
                        ASTHrefId hrefId = new ASTHrefId();
                        hrefId.setId(inputName);
                        block.connectProcessorToHref(choose.getNode(), processor, inputName, hrefId);
                    }
                }
                for (Iterator j = processor.getOutputsInfo().iterator(); j.hasNext();) {
                    String outputName = ((ProcessorInputOutputInfo) j.next()).getName();
                    foundOutput = true;
                    ProcessorOutput pout = processor.createOutput(outputName);
                    if (chooseProcessor.getOutputsById().contains(outputName))
                        block.declareOutput(choose.getNode(), outputName, pout);
                    if (chooseProcessor.getOutputsByParamRef().contains(outputName))
                        block.connectProcessorToBottomInput(choose.getNode(), outputName, outputName, pout);
                }

            } else if (statement instanceof ASTForEach) {

                // Instantiate processor
                final ASTForEach forEach = (ASTForEach) statement;
                final LocationData forEachLocationData = forEach.getLocationData();
                final AbstractProcessor forEachAbstractProcessor = new AbstractForEachProcessor(forEach, astPipeline.getValidity());
                final ConcreteForEachProcessor forEachProcessor =
                        (ConcreteForEachProcessor) forEachAbstractProcessor.createInstance();
                processor = forEachProcessor;

                // Connect special $data input (document on which the decision is made, or iterated on)
                final ProcessorInput pin = block.connectProcessorToHref(forEach.getNode(), processor,
                        AbstractForEachProcessor.FOR_EACH_DATA_INPUT, forEach.getHref());
                setDebugAndSchema(pin, forEach, forEachLocationData,
                        forEach.getInputSchemaUri(), forEach.getInputSchemaHref(), forEach.getInputDebug());

                // Go through inputs and connect to the rest of the pipeline
                for (Iterator j = processor.getInputsInfo().iterator(); j.hasNext();) {
                    // We reference a previously declared output
                    final String inputName = ((ProcessorInputOutputInfo) j.next()).getName();
                    if (!inputName.equals(AbstractForEachProcessor.FOR_EACH_DATA_INPUT)) {
                        final ASTHrefId hrefId = new ASTHrefId();
                        hrefId.setId(inputName);
                        // NOTE: Force creation of a tee so that inputs of p:for-each are not read multiple times
                        block.connectProcessorToHref(forEach.getNode(), processor, inputName, hrefId, true);
                    }
                }

                // Connect output
                final String outputName = forEach.getId() != null ? forEach.getId() : forEach.getRef();
                if (outputName != null) {
                    foundOutput = true;
                    final ProcessorOutput forEachOutput = processor.createOutput(outputName);
                    if (forEach.getId() != null)
                        block.declareOutput(forEach.getNode(), forEach.getId(), forEachOutput);
                    if (forEach.getRef() != null)
                        block.connectProcessorToBottomInput(forEach.getNode(), forEach.getId(), forEach.getRef(), forEachOutput);
                    setDebugAndSchema(processor.getOutputByName(outputName), forEach, forEachLocationData,
                            forEach.getOutputSchemaUri(), forEach.getOutputSchemaHref(), forEach.getOutputDebug());
                }
            }

            // Remember all processors and processor with no output (need to be started)
            if (processor != null) {
                config.addProcessor(processor);
                if (!foundOutput) {
                    config.addProcessorToStart(processor);
                }
            }
        }

        // Check that all bottom inputs are connected
        for (Iterator i = astPipeline.getParams().iterator(); i.hasNext();) {
            ASTParam param = (ASTParam) i.next();
            if (param.getType() == ASTParam.OUTPUT) {
                if (!block.isBottomInputConnected(param.getName()))
                    throw new ValidationException("No processor in pipeline is connected to pipeline output '"
                            + param.getName() + "'", param.getLocationData());
            }
        }

        // Add processors created for connection reasons
        for (Iterator i = block.getCreatedProcessors().iterator(); i.hasNext();)
            config.addProcessor((Processor) i.next());

        return config;
    }

    private PipelineConfig readPipelineConfig(PipelineContext context, ProcessorInput configInput) {
        try {
            // Read config input using PipelineReader
            final ProcessorInput _configInput = configInput;
            PipelineReader pipelineReader = new PipelineReader();
            ProcessorInput pipelineReaderInput = pipelineReader.createInput("pipeline");

            pipelineReaderInput.setOutput(new ProcessorImpl.ProcessorOutputImpl(getClass(), "dummy") {
                public void readImpl(PipelineContext context, ContentHandler contentHandler) {
                    ProcessorImpl.readInputAsSAX(context, _configInput, contentHandler);
                }

                public OutputCacheKey getKeyImpl(PipelineContext context) {
                    return getInputKey(context, _configInput);
                }

                public Object getValidityImpl(PipelineContext context) {
                    return getInputValidity(context, _configInput);
                }

            });
            pipelineReader.start(context);
            return createConfigFromAST(pipelineReader.getPipeline());
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }

    private static void setDebugAndSchema(ProcessorInputOutput processorInputOutput,
                                          ASTNodeContainer astNodeContainer) {

        if (astNodeContainer instanceof ASTDebugSchema) {
            final ASTDebugSchema astDebugSchema = (ASTDebugSchema) astNodeContainer;
            setDebugAndSchema(processorInputOutput, astNodeContainer,
                ((ASTNodeContainer) astNodeContainer).getLocationData(),
                astDebugSchema.getSchemaUri(), astDebugSchema.getSchemaHref(), astDebugSchema.getDebug());
        } else {
            setDebugAndSchema(processorInputOutput, astNodeContainer,
                ((ASTNodeContainer) astNodeContainer).getLocationData(), null, null, null);
        }
    }

    private static void setDebugAndSchema(ProcessorInputOutput processorInputOutput,
                                          ASTNodeContainer astNodeContainer, LocationData locationData,
                                          String schemaUri, String schemaHref, String debug) {
        // Set schema if any
        if (schemaUri != null) {
            processorInputOutput.setSchema(SchemaRepository.instance().getSchemaLocation(schemaUri));
        } else if (schemaHref != null) {
            String url;
            if (locationData != null) {
                try {
                    url = URLFactory.createURL(locationData.getSystemID(), schemaHref).toString();
                } catch (MalformedURLException e) {
                    // Does this happen? If yes, why?
                    // We don't really know. Set let's just throw an exception in this case.
                    // If it never happens, we can then remove this comment.
                    //url = schemaHref;
                    throw new ValidationException("Invalid URL: sytem id is '" + locationData.getSystemID()
                            + "' and schema href is '" + schemaHref + "'", locationData);
                }
            } else {
                url = schemaHref;
            }
            processorInputOutput.setSchema(url);
        }

        // Set debug if any
        if (debug != null)
            processorInputOutput.setDebug(debug);

        // Set location data
        if (locationData != null) {
            if (locationData instanceof ExtendedLocationData) {
                processorInputOutput.setLocationData(locationData);
            } else {
                final String description;
                final String[] params;
                if (processorInputOutput instanceof ProcessorInput) {
                    description = "reading processor input";
                    params = new String[] { "name", processorInputOutput.getName() } ;
                } else if (processorInputOutput instanceof ProcessorOutput) {
                    description = "reading processor output";
                    final String outputId = (astNodeContainer instanceof ASTOutput) ? ((ASTOutput) astNodeContainer).getId() : null;
                    final String outputRef = (astNodeContainer instanceof ASTOutput) ? ((ASTOutput) astNodeContainer).getRef() : null;
                    params = new String[] { "name", processorInputOutput.getName(), "id", outputId, "ref", outputRef } ;
                } else {
                    description = "reading";
                    params = null;
                }

                processorInputOutput.setLocationData(new ExtendedLocationData(locationData,
                        description, (Element) astNodeContainer.getNode(), params, true));
            }
        }
    }

    private static void setBreakpointKey(ProcessorInputOutput processorInputOutput, ASTNodeContainer nodeContainer) {
        LocationData locationData = nodeContainer.getLocationData();

        // Only set if there is a chance we can match on it later
        if (locationData != null && locationData.getSystemID() != null && locationData.getLine() != -1 )
            processorInputOutput.setBreakpointKey(new BreakpointKey(locationData));
    }

    /**
     * Those are the "artificial" outputs sitting at the "top" of the pipeline
     * to which the "top processors" are connected.
     */
    public static class InternalTopOutput extends ProcessorImpl.ProcessorOutputImpl {

        private LocationData locationData;

        public InternalTopOutput(String name, LocationData locationData) {
            super(null, name);
            this.locationData = locationData;
        }

        public void readImpl(final PipelineContext context, final ContentHandler contentHandler) {
            final State state = (State) getParentState(context);
            executeParents(context, new Runnable() {
                public void run() {
                    // NOTE: It is not useful to catch and wrap location data here, as this would
                    // duplicate the work done in ProcessorImpl
                    readInputAsSAX(context, getPipelineInputFromState(state), contentHandler);
                }
            });
        }

        public OutputCacheKey getKeyImpl(final PipelineContext context) {
            final OutputCacheKey[] key = new OutputCacheKey[1];
            final State state = (State) getParentState(context);
            executeParents(context, new Runnable() {
                public void run() {
                    key[0] = getInputKey(context, getPipelineInputFromState(state));
                }
            });
            return key[0];
        }

        public Object getValidityImpl(final PipelineContext context) {
            final Object[] obj = new Object[1];
            final State state = (State) getParentState(context);
            executeParents(context, new Runnable() {
                public void run() {
                    obj[0] = getInputValidity(context, getPipelineInputFromState(state));
                }
            });
            return obj[0];
        }

        private ProcessorInput getPipelineInputFromState(State state) {
            List pipelineInputs = (List) state.pipelineInputs.get(getName());
            if (pipelineInputs == null)
                throw new ValidationException("Pipeline input \"" + getName() + "\" is not connected", locationData);
            return (ProcessorInput) ((List) state.pipelineInputs.get(getName())).get(0);
        }
    }

    public static class InternalBottomInput extends ProcessorImpl.ProcessorInputImpl {
        public InternalBottomInput(String name) {
            super(PipelineProcessor.class, name);
        }
    }

    public void start(final PipelineContext context) {
        // Check that we are not already started
        State state = (State) getState(context);
        if (state.started)
            throw new IllegalStateException("ASTPipeline Processor already started");

        // Create config. We have 2 cases:
        // 1) The config is provided to us as an AST
        // 2) We need to read the config input
        final PipelineConfig config = configFromAST != null ? configFromAST :
                (PipelineConfig) readCacheInputAsObject(context, getInputByName(INPUT_CONFIG),
                        new CacheableInputReader() {
                            public Object read(final PipelineContext context, final ProcessorInput input) {
                                return readPipelineConfig(context, input);
                            }
                        });

        // Reset processors
        executeChildren(context, new Runnable() {
            public void run() {
                for (Iterator i = config.getProcessors().iterator(); i.hasNext();) {
                    ((Processor) i.next()).reset(context);
                }
            }
        });

        // Save inputs in state for InternalTopOutput
        state.pipelineInputs = getConnectedInputs();

        // Bottom inputs: copy in state
        state.nameToBottomInputMap = config.getNameToInputMap();
        state.started = true;

        // Run the processors that are not connected to any pipeline output
        for (Iterator i = config.getProcessorsToStart().iterator(); i.hasNext();) {
            final Processor processor = (Processor) i.next();
            executeChildren(context, new Runnable() {
                public void run() {
                    try {
                        processor.start(context);
                    } catch (Exception e) {
                        throw ValidationException.wrapException(e, processor.getLocationData());
                    }
                }
            });
        }
    }

    public void reset(final PipelineContext context) {
        setState(context, new State());
    }

    /**
     * FIXME - We can't really do this until we have the config. The way we
     * implement this is going to change when we introduce abstract processors.
     */
    public void checkSockets() {
        // nop
    }

    private static class State {
        public Map nameToBottomInputMap = new HashMap();
        public boolean started = false;
        public Map pipelineInputs = new HashMap();
    }

    private List breakpoints;

    public void addBreakpoint(Breakpoint breakpoint) {
        if (breakpoints == null)
            breakpoints = new ArrayList();
        breakpoints.add(breakpoint);
    }

    public List getBreakpoints() {
        return breakpoints;
    }
}
