/**
 * Copyright (C) 2009 Orbeon, Inc.
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
package org.orbeon.oxf.xforms.control;

import org.dom4j.Element;
import org.orbeon.oxf.util.PropertyContext;
import org.orbeon.oxf.xforms.XFormsContextStack;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.event.XFormsEvents;
import org.orbeon.oxf.xforms.xbl.XBLContainer;

/**
 * Control that represents a custom components.
 *
 * A component control contains a nested container, which handles:
 *
 * o models nested within component (which we are not 100% happy with as models should be allowed in other places)
 * o HOWEVER this might still be all right for models within xbl:implementation if any
 * o event dispatching
 */
public class XFormsComponentControl extends XFormsNoSingleNodeContainerControl {

    private XBLContainer nestedContainer;
    private transient boolean isInitializeModels;

    public XFormsComponentControl(XBLContainer container, XFormsControl parent, Element element, String name, String effectiveId) {
        super(container, parent, element, name, effectiveId);

        // Create container and nested models if any
        nestedContainer = container.createChildContainer(effectiveId);
        nestedContainer.addAllModels();// NOTE: there may or may not be nested models

        // Make sure there is location data
        nestedContainer.setLocationData(XFormsUtils.getNodeLocationData(element));
    }

    @Override
    public void setBindingContext(PropertyContext propertyContext, XFormsContextStack.BindingContext bindingContext, boolean isCreate) {
        final boolean isNewBinding = getBindingContext() == null;
        final boolean isNodesetChange = isNewBinding|| !compareNodesets(getBindingContext().getNodeset(), bindingContext.getNodeset());

        // Set/update binding context on control
        super.setBindingContext(propertyContext, bindingContext, isCreate);

        nestedContainer.setBindingContext(bindingContext);
        nestedContainer.getContextStack().resetBindingContext(propertyContext);

        // Set/update binding context on container
        if (isNewBinding) {
            // Control is newly bound

            if (containingDocument.isRestoringDynamicState(propertyContext)) {
                // Restore models
                nestedContainer.restoreModelsState(propertyContext);
            } else {
                // Start models initialization
                nestedContainer.initializeModels(propertyContext, new String[] {
                        XFormsEvents.XFORMS_MODEL_CONSTRUCT,
                        XFormsEvents.XFORMS_MODEL_CONSTRUCT_DONE
                });
                isInitializeModels = true;
            }
            nestedContainer.getContextStack().resetBindingContext(propertyContext);
        } else if (isNodesetChange) {
            // Control's binding changed

        }
    }

    @Override
    public void childrenAdded(PropertyContext propertyContext) {
        super.childrenAdded(propertyContext);

        if (isInitializeModels) {
            // It doesn't seem to make much sense to dispatch xforms-ready to nested models. If we still did want to do
            // that, we should do it once ALL controls have been initialized. But likely this is not a good idea
            // either.
            isInitializeModels = false;
        }
    }

    public XBLContainer getNestedContainer() {
        return nestedContainer;
    }

    @Override
    public void updateEffectiveId() {

        // This is called iif the iteration index changes

        // Update rest of control tree
        super.updateEffectiveId();

        // Update container with new effective id
        nestedContainer.updateEffectiveId(getEffectiveId());
    }

    @Override
    public void iterationRemoved(PropertyContext propertyContext) {
        // Inform descendants
        super.iterationRemoved(propertyContext);

        // Destroy container and models if any
        nestedContainer.destroy(propertyContext);
    }
}
