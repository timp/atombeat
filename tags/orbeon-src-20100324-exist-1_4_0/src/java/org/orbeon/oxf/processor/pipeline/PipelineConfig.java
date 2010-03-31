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


import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.debugger.api.BreakpointKey;
import org.orbeon.oxf.util.NullFriendlyStringComparator;
import org.orbeon.oxf.processor.Processor;
import org.orbeon.oxf.processor.ProcessorInput;

import java.util.*;

/**
 * Encapsulates the "configuration" of a pipeline: contains references to
 * the connected processors inside the pipeline and all the information
 * necessary to run the pipeline.
 */
public class PipelineConfig {

    // Maps: (String inputParamName) -> (List[InternalTopOutput internalTopOutput])
    private Map<String, List<PipelineProcessor.InternalTopOutput>> nameToTopOuputMap = new HashMap<String, List<PipelineProcessor.InternalTopOutput>>();
    // Maps: (String outputParamName) -> (ProcessorInput internalBottonInput)
    private Map<String, ProcessorInput> nameToBottomInputMap = new HashMap<String, ProcessorInput>();
    // All internal processors
    private List<Processor> processors = new ArrayList<Processor>();
    // List of Processor objects: we have to call their start() method
    private List<Processor> processorsToStart = new ArrayList<Processor>();
    
    private TreeMap<String,BreakpointKey> outnameToBreakpointKey
        = new java.util.TreeMap<String, BreakpointKey>( NullFriendlyStringComparator.instance );
    
    void setOutputBreakpointKey( final String nm, final BreakpointKey bptKey ) {
        outnameToBreakpointKey.put( nm, bptKey );
    }
    BreakpointKey getOutputBreakpointKey( final String nm ) {
        return outnameToBreakpointKey.get( nm );
    }

    public void declareTopOutput(String name, PipelineProcessor.InternalTopOutput topOutput) {
        List<PipelineProcessor.InternalTopOutput> outputsForName = nameToTopOuputMap.get(name);
        if (outputsForName == null) {
            outputsForName = new ArrayList<PipelineProcessor.InternalTopOutput>();
            nameToTopOuputMap.put(name, outputsForName);
        }
        outputsForName.add(topOutput);
    }

    public Map<String, List<PipelineProcessor.InternalTopOutput>> getNameToOutputMap() {
        return nameToTopOuputMap;
    }

    public void declareBottomInput(String name, org.orbeon.oxf.processor.ProcessorInput bottomInput) {
        if (nameToBottomInputMap.containsKey(name))
            throw new OXFException("Duplicate output parameter with name \"" + name + "\"");
        nameToBottomInputMap.put(name, bottomInput);
    }

    public Map<String, ProcessorInput> getNameToInputMap() {
        return nameToBottomInputMap;
    }

    public void addProcessor(org.orbeon.oxf.processor.Processor processor) {
        processors.add(processor);
    }

    public List<Processor> getProcessors() {
        return processors;
    }

    public void addProcessorToStart(org.orbeon.oxf.processor.Processor processor) {
        processorsToStart.add(processor);
    }

    public List<Processor> getProcessorsToStart() {
        return processorsToStart;
    }
}
