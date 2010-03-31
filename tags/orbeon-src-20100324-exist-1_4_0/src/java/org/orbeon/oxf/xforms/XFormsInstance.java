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
package org.orbeon.oxf.xforms;

import org.dom4j.*;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.util.IndentedLogger;
import org.orbeon.oxf.util.PropertyContext;
import org.orbeon.oxf.xforms.control.XFormsControl;
import org.orbeon.oxf.xforms.control.controls.XFormsRepeatControl;
import org.orbeon.oxf.xforms.event.XFormsEvent;
import org.orbeon.oxf.xforms.event.XFormsEventObserver;
import org.orbeon.oxf.xforms.event.XFormsEventTarget;
import org.orbeon.oxf.xforms.event.XFormsEvents;
import org.orbeon.oxf.xforms.event.events.XFormsBindingExceptionEvent;
import org.orbeon.oxf.xforms.event.events.XFormsDeleteEvent;
import org.orbeon.oxf.xforms.event.events.XFormsInsertEvent;
import org.orbeon.oxf.xforms.xbl.XBLBindings;
import org.orbeon.oxf.xforms.xbl.XBLContainer;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom4j.DocumentWrapper;
import org.orbeon.saxon.dom4j.NodeWrapper;
import org.orbeon.saxon.dom4j.TypedDocumentWrapper;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.VirtualNode;
import org.xml.sax.ContentHandler;

import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represent an XForms instance.
 */
public class XFormsInstance implements XFormsEventTarget, XFormsEventObserver {

    private DocumentInfo documentInfo;

    protected String instanceStaticId;
    protected String modelEffectiveId;

    private String sourceURI;
    private String requestBodyHash;

    private boolean readonly;
    private boolean cache;
    private long timeToLive;
    private String username;
    private String password;
    private String validation;
    private boolean handleXInclude;
    private boolean exposeXPathTypes;

    /**
     * Whether the instance was ever replaced. This is useful so that we know whether we can use an instance from the
     * static state or not: if it was ever replaced, then we can't use instance information from the static state.
     */
    private boolean replaced;

    /**
     * Create an XFormsInstance from a container element. The container contains meta-informationa about the instance,
     * such as id, username, URI, etc.
     *
     * <instance readonly="true" cache="true" id="instance-id" model-id="model-id" source-uri="http://..." username="jdoe" password="password">
     *     x7wer...
     * </instance>
     *
     * The instance document may not have been set after this is completed, in case the Element did not contained a
     * serialized document.
     *
     * @param containerElement  container element
     */
    public XFormsInstance(Element containerElement) {

        this.instanceStaticId = containerElement.attributeValue("id");
        this.modelEffectiveId = containerElement.attributeValue("model-id");

        this.sourceURI = containerElement.attributeValue("source-uri");
        this.requestBodyHash = containerElement.attributeValue("request-body-hash");

        this.readonly = "true".equals(containerElement.attributeValue("readonly"));
        this.cache = "true".equals(containerElement.attributeValue("cache"));
        final String timeToLiveAttribute = containerElement.attributeValue("ttl");
        this.timeToLive = (timeToLiveAttribute != null) ? Long.parseLong(timeToLiveAttribute) : -1;

        this.username = containerElement.attributeValue("username");
        this.password = containerElement.attributeValue("password");
        this.validation = containerElement.attributeValue("validation");
        this.handleXInclude = "true".equals(containerElement.attributeValue("xinclude"));
        this.exposeXPathTypes = "true".equals(containerElement.attributeValue("types"));

        this.replaced = "true".equals(containerElement.attributeValue("replaced"));

        // Create and set instance document on current model
        final DocumentInfo documentInfo;
        // Instance is available as serialized XML
        try {
            final String xmlString = containerElement.getStringValue();
            if (xmlString.length() > 0) {
                // Instance document is available in serialized form
                if (!readonly) {
                    if (exposeXPathTypes) {
                        // Make a typed document wrapper
                        documentInfo = new TypedDocumentWrapper((Document) Dom4jUtils.normalizeTextNodes(Dom4jUtils.readDom4j(xmlString, false, false)), null, new Configuration());
                    } else {
                        // Make a non-typed document wrapper
                        documentInfo = new DocumentWrapper((Document) Dom4jUtils.normalizeTextNodes(Dom4jUtils.readDom4j(xmlString, false, false)), null, new Configuration());
                    }
                } else {
                    // Just use TinyTree as is
                    documentInfo = TransformerUtils.stringToTinyTree(xmlString, false);
                }
            } else {
                // Instance document is not available, defer to later initialization
                documentInfo = null;
            }
        } catch (Exception e) {
            throw new OXFException(e);
        }

        this.documentInfo = documentInfo;
    }

    public XFormsInstance(String modelEffectiveId, String instanceStaticId, Document instanceDocument, String instanceSourceURI, String requestBodyHash,
                          String username, String password, boolean cache, long timeToLive, String validation, boolean handleXInclude, boolean exposeXPathTypes) {
        // We normalize the Document before setting it, so that text nodes follow the XPath constraints
        // NOTE: Make a typed document wrapper
        this(modelEffectiveId, instanceStaticId,
                exposeXPathTypes
                        ? new TypedDocumentWrapper((Document) Dom4jUtils.normalizeTextNodes(instanceDocument), null, new Configuration())
                        : new DocumentWrapper((Document) Dom4jUtils.normalizeTextNodes(instanceDocument), null, new Configuration()),
                instanceSourceURI, requestBodyHash, username, password, cache, timeToLive, validation, handleXInclude, exposeXPathTypes);
    }

    protected XFormsInstance(String modelEffectiveId, String instanceStaticId, DocumentInfo instanceDocumentInfo, String instanceSourceURI, String requestBodyHash,
                             String username, String password, boolean cache, long timeToLive, String validation, boolean handleXInclude, boolean exposeXPathTypes) {

        if (cache && instanceSourceURI == null)
            throw new OXFException("Only XForms instances externally loaded through the src attribute may have xxforms:cache=\"true\".");

        this.instanceStaticId = instanceStaticId;
        this.modelEffectiveId = modelEffectiveId;

        this.readonly = !(instanceDocumentInfo instanceof DocumentWrapper);
        this.cache = cache;
        this.timeToLive = timeToLive;

        this.sourceURI = instanceSourceURI;
        this.requestBodyHash = requestBodyHash;

        this.username = username;
        this.password = password;
        this.validation = validation;
        this.handleXInclude = handleXInclude;
        this.exposeXPathTypes = exposeXPathTypes;

        this.documentInfo = instanceDocumentInfo;
    }

    public void updateModelEffectiveId(String modelEffectiveId) {
        this.modelEffectiveId = modelEffectiveId;
    }

    /**
     * Serialize the instance into a containing Element with meta-information.
     *
     * @param serializeInstance     whether the instance document must be serialized
     * @return                      containing Element
     */
    public Element createContainerElement(boolean serializeInstance) {

        // DocumentInfo may wrap an actual TinyTree or a dom4j document
        final Element instanceElement = Dom4jUtils.createElement("instance");

        if (readonly)
            instanceElement.addAttribute("readonly", "true");
        if (cache)
            instanceElement.addAttribute("cache", "true");
        if (timeToLive >= 0)
            instanceElement.addAttribute("ttl", Long.toString(timeToLive));

        instanceElement.addAttribute("id", instanceStaticId);
        instanceElement.addAttribute("model-id", modelEffectiveId);
        if (sourceURI != null)
            instanceElement.addAttribute("source-uri", sourceURI);
        if (requestBodyHash != null)
            instanceElement.addAttribute("request-body-hash", requestBodyHash);
        if (username != null)
            instanceElement.addAttribute("username", username);
        if (password != null)
            instanceElement.addAttribute("password", password);
        if (validation != null)
            instanceElement.addAttribute("validation", validation);
        if (handleXInclude)
            instanceElement.addAttribute("xinclude", "true");
        if (exposeXPathTypes)
            instanceElement.addAttribute("types", "true");

        if (replaced)
            instanceElement.addAttribute("replaced", "true");

        if (serializeInstance) {
            final String instanceString;
            if (getDocument() != null) {
                // This is probably more optimal than going through NodeInfo. Furthermore, there may be an issue with
                // namespaces when using tinyTreeToString(). Bug in the NodeWrapper or dom4j?
                instanceString = TransformerUtils.dom4jToString(getDocument());
            } else {
                instanceString = TransformerUtils.tinyTreeToString(getDocumentInfo());
            }
            instanceElement.addText(instanceString);
        }

        return instanceElement;
    }

    /**
     * Return the model that contains this instance.
     *
     * @param containingDocument    XFormsContainingDocument
     * @return XFormsModel          XFormsModel containing this instance
     */
    public XFormsModel getModel(XFormsContainingDocument containingDocument) {
        return (XFormsModel) containingDocument.getObjectByEffectiveId(modelEffectiveId);
    }

    /**
     * Return the instance DocumentInfo.
     *
     * @return  instance DocumentInfo
     */
    public DocumentInfo getDocumentInfo() {
        return documentInfo;
    }

    /**
     * Return the id of this instance.
     */
    public String getId() {
        return instanceStaticId;
    }

    public String getPrefixedId() {
        return XFormsUtils.getPrefixedId(getEffectiveId());
    }

    public String getEffectiveId() {
        return XFormsUtils.getRelatedEffectiveId(modelEffectiveId, getId());
    }

    public XBLContainer getXBLContainer(XFormsContainingDocument containingDocument) {
        return getModel(containingDocument).getXBLContainer();
    }

    public String getEffectiveModelId() {
        return modelEffectiveId;
    }

    public boolean isReadOnly() {
        return !(documentInfo instanceof VirtualNode);
    }


    public boolean isCache() {
        return cache;
    }


    public long getTimeToLive() {
        return timeToLive;
    }

    public NodeInfo getInstanceRootElementInfo() {
        return XFormsUtils.getChildrenElements(documentInfo).get(0);
    }

    public String getSourceURI() {
        return sourceURI;
    }

    public String getRequestBodyHash() {
        return requestBodyHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getValidation() {
        return validation;
    }

    public boolean isHandleXInclude() {
        return handleXInclude;
    }

    public boolean isExposeXPathTypes() {
        return exposeXPathTypes;
    }

    public boolean isReplaced() {
        return replaced;
    }

    public void setReplaced(boolean replaced) {
        this.replaced = replaced;
    }

    /**
     * Set a value on the instance using a NodeInfo and a value.
     *
     * @param propertyContext       current context
     * @param containingDocument    containing document (for event dispatch), null if no events requested
     * @param eventTarget           event target (for event dispatch), null if no events requested
     * @param nodeInfo              element or attribute NodeInfo to update
     * @param newValue              value to set
     * @param type                  type of the value to set (xs:anyURI or xs:base64Binary), null if none
     */
    public static void setValueForNodeInfo(PropertyContext propertyContext, XFormsContainingDocument containingDocument,
                                     XFormsEventTarget eventTarget, NodeInfo nodeInfo, String newValue, String type) {
        if (!(nodeInfo instanceof NodeWrapper))
            throw new OXFException("Unable to set value of read-only instance.");

        final Node node = (Node) ((NodeWrapper) nodeInfo).getUnderlyingNode();
        if (containingDocument != null && eventTarget != null) {
            // "10.2 The setvalue Element [...] An xforms-binding-exception occurs if the Single Node Binding
            // indicates a node whose content is not simpleContent (i.e., a node that has element children)."
            if (!Dom4jUtils.isSimpleContent(node)) {
                containingDocument.dispatchEvent(propertyContext, new XFormsBindingExceptionEvent(containingDocument, eventTarget));
                return;
            }
        }
        setValueForNode(propertyContext, node, newValue, type);
    }

    /**
     * Set a value on the instance using a Node and a value.
     *
     * @param propertyContext   current context
     * @param node              element or attribute Node to update
     * @param newValue          value to set
     * @param type              type of the value to set (xs:anyURI or xs:base64Binary), null if none
     */
    public static void setValueForNode(PropertyContext propertyContext, Node node, String newValue, String type) {

        // Convert value based on types if possible
        if (type != null) {
            final String nodeType = InstanceData.getType(node);

            if (nodeType != null && !nodeType.equals(type)) {
                // There is a different type already, do a conversion
                newValue = XFormsUtils.convertUploadTypes(propertyContext, newValue, type, nodeType);
            } else if (nodeType == null) {
                // There is no type, convert to default type
                if (!XFormsConstants.DEFAULT_UPLOAD_TYPE_EXPLODED_QNAME.equals(type))
                    newValue = XFormsUtils.convertUploadTypes(propertyContext, newValue, type, XFormsConstants.DEFAULT_UPLOAD_TYPE_EXPLODED_QNAME);
            }
        }

        // Set value
        if (node instanceof Element) {
            // NOTE: Previously, there was a "first text node rule" which ended up causing problems and was removed.
            final Element elementNode = (Element) node;
            elementNode.setText(newValue);
        } else if (node instanceof Attribute) {
            // "Attribute nodes: The string-value of the attribute is replaced with a string corresponding to the new
            // value."
            final Attribute attributeNode = (Attribute) node;
            attributeNode.setValue(newValue);
        } else if (node instanceof Text) {
            // "Text nodes: The text node is replaced with a new one corresponding to the new value."
            final Text textNode = (Text) node;
            textNode.setText(newValue);
        } else {
            // "Namespace, processing instruction, comment, and the XPath root node: behavior is undefined."
            throw new OXFException("Setting value on node other than element, attribute or text is not supported for node type: " + node.getNodeTypeName());
        }
    }

    public static String getValueForNodeInfo(NodeInfo nodeInfo) {

        if (nodeInfo.getNodeKind() == org.w3c.dom.Document.ELEMENT_NODE
                || nodeInfo.getNodeKind() == org.w3c.dom.Document.ATTRIBUTE_NODE
                || nodeInfo.getNodeKind() == org.w3c.dom.Document.TEXT_NODE) {

            // NOTE: In XForms 1.1, all these node types return the string value. Note that previously, there was a
            // "first text node rule" which ended up causing problems and was removed.
            return nodeInfo.getStringValue();
        } else {
            // "Namespace, processing instruction, comment, and the XPath root node: behavior is undefined."
            throw new OXFException("Setting value on node other than element, attribute or text is not supported for node type: " + nodeInfo.getNodeKind());
        }
    }

    public static String getValueForNode(Node node) {

        if (node.getNodeType() == org.w3c.dom.Document.ELEMENT_NODE
                || node.getNodeType() == org.w3c.dom.Document.ATTRIBUTE_NODE
                || node.getNodeType() == org.w3c.dom.Document.TEXT_NODE) {

            // NOTE: In XForms 1.1, all these node types return the string value. Note that previously, there was a
            // "first text node rule" which ended up causing problems and was removed.
            return node.getStringValue();
        } else {
            // "Namespace, processing instruction, comment, and the XPath root node: behavior is undefined."
            throw new OXFException("Setting value on node other than element, attribute or text is not supported for node type: " + node.getNodeTypeName());
        }
    }

    /**
     * Output the instance to the specified ContentHandler
     *
     * @param contentHandler    ContentHandler to write to
     */
    public void read(ContentHandler contentHandler) {
        try {
            final Transformer identity = TransformerUtils.getIdentityTransformer();
            identity.transform(documentInfo, new SAXResult(contentHandler));
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }

    /**
     * This prints the instance with extra annotation attributes to System.out. For debug only.
     */
    public void debugReadOut() {
        final TransformerHandler  th = TransformerUtils.getIdentityTransformerHandler();
        th.setResult(new StreamResult(System.out));
        read(th);
    }

    /**
     * This allows dumping all the current MIPs applying to this instance.
     */
    public void debugLogMIPs() {

        final Document result = Dom4jUtils.createDocument();

        getDocument().accept(new VisitorSupport() {

            private Element rootElement = result.addElement("mips");
            private Element currentElement;

            public final void visit(Element element) {
                currentElement = rootElement.addElement("element");
                currentElement.addAttribute("qname", element.getQualifiedName());
                currentElement.addAttribute("namespace-uri", element.getNamespaceURI());

                addMIPInfo(currentElement, element);
            }

            public final void visit(Attribute attribute) {
                final Element attributeElement = currentElement.addElement("attribute");
                attributeElement.addAttribute("qname", attribute.getQualifiedName());
                attributeElement.addAttribute("namespace-uri", attribute.getNamespaceURI());
                addMIPInfo(attributeElement, attribute);
            }

            private void addMIPInfo(Element parentInfoElement, Node node) {
                parentInfoElement.addAttribute("readonly", Boolean.toString(InstanceData.getInheritedReadonly(node)));
                parentInfoElement.addAttribute("relevant", Boolean.toString(InstanceData.getInheritedRelevant(node)));
                parentInfoElement.addAttribute("required", Boolean.toString(InstanceData.getRequired(node)));
                parentInfoElement.addAttribute("valid", Boolean.toString(InstanceData.getValid(node)));
                final String type = InstanceData.getType(node);
                parentInfoElement.addAttribute("type", (type == null) ? "" : type);
//                parentInfoElement.addAttribute("schema-error-messages", instanceData.getSchemaErrorsMsgs());
            }
        });

        XFormsUtils.logDebugDocument("MIPs: ", result);
    }

    public LocationData getLocationData() {
        if (documentInfo instanceof DocumentWrapper) {
            final Document document = getDocument();
            return XFormsUtils.getNodeLocationData(document.getRootElement());
        } else {
            return new LocationData(documentInfo.getSystemId(), documentInfo.getLineNumber(), -1);
        }
    }

    public XFormsEventObserver getParentEventObserver(XBLContainer container) {
        return getModel(container.getContainingDocument());
    }

    public void performDefaultAction(PropertyContext propertyContext, XFormsEvent event) {
        final String eventName = event.getName();
        if (XFormsEvents.XXFORMS_INSTANCE_INVALIDATE.equals(eventName)) {
            final IndentedLogger indentedLogger = event.getTargetXBLContainer().getContainingDocument().getIndentedLogger(XFormsModel.LOGGING_CATEGORY);
            // Invalidate instance if it is cached
            if (cache) {
                XFormsServerSharedInstancesCache.instance().remove(propertyContext, indentedLogger, sourceURI, null, handleXInclude);
            } else {
                indentedLogger.logWarning("", "XForms - xxforms-instance-invalidate event dispatched to non-cached instance", "instance id", getEffectiveId());
            }
        }
    }

    /**
     * Action run when the event reaches the target.
     *
     * @param propertyContext       current context
     * @param container             container
     * @param event                 event being dispatched
     */
    public void performTargetAction(final PropertyContext propertyContext, XBLContainer container, XFormsEvent event) {
        final String eventName = event.getName();
        if (XFormsEvents.XFORMS_INSERT.equals(eventName)) {
            // New nodes were just inserted
            final XFormsInsertEvent insertEvent = (XFormsInsertEvent) event;

            // As per XForms 1.1, this is where repeat indexes must be adjusted, and where new repeat items must be
            // inserted.

            // Find affected repeats
            final List<Item> insertedNodeInfos = insertEvent.getInsertedNodeInfos();

            final boolean didInsertNodes = insertedNodeInfos.size() != 0;
            final boolean mustAdjustIndexes = didInsertNodes && insertEvent.isAdjustIndexes();// isAdjustIndexes() used for offline mode optimizations

            if (mustAdjustIndexes) {
                // Perform the adjustments

                // Find affected repeats and update their node-sets and indexes
                final XFormsControls controls = container.getContainingDocument().getControls();
                updateRepeatNodesets(propertyContext, controls, insertedNodeInfos);
            }
        } else if (XFormsEvents.XFORMS_DELETE.equals(eventName)) {
            // New nodes were just deleted
            final XFormsDeleteEvent deleteEvent = (XFormsDeleteEvent) event;

            final List deletedNodeInfos = deleteEvent.getDeletedNodeInfos();
            final boolean didDeleteNodes = deletedNodeInfos.size() != 0;
            if (didDeleteNodes) {
                // Find affected repeats and update them
                final XFormsControls controls = container.getContainingDocument().getControls();
                updateRepeatNodesets(propertyContext, controls, null);
            }
        }
    }

    private void updateRepeatNodesets(PropertyContext propertyContext, XFormsControls controls, List<Item> insertedNodeInfos) {
        final Map<String, XFormsControl> repeatControlsMap = controls.getCurrentControlTree().getRepeatControls();
        if (repeatControlsMap != null) {

            final XBLBindings bindings = controls.getContainingDocument().getStaticState().getXBLBindings();
            final XBLBindings.Scope instanceScope = bindings.getResolutionScopeByPrefixedId(getPrefixedId());

            // NOTE: Read in a list as the list of repeat controls may change within updateNodeset()
            final List<XFormsControl> repeatControls = new ArrayList<XFormsControl>(repeatControlsMap.values());
            for (XFormsControl repeatControl: repeatControls) {
                // Get a new reference to the control, in case it is no longer present in the tree due to earlier updates
                // TODO: is this needed with new clone/update mechanism?
                final XFormsRepeatControl newRepeatControl = (XFormsRepeatControl) controls.getObjectByEffectiveId(repeatControl.getEffectiveId());
                // Update node-set
                if (newRepeatControl != null) {
                    // Only update controls within same scope as modified instance
                    // NOTE: This can clearly break with e.g. xxforms:instance()
                    if (newRepeatControl.getResolutionScope() == instanceScope) {
                        newRepeatControl.updateNodesetForInsertDelete(propertyContext, insertedNodeInfos);
                    }
                }
            }
        }
    }

    /**
     * Return the instance document as a dom4j Document.
     *
     * NOTE: Should use getInstanceDocumentInfo() whenever possible.
     *
     * @return  instance document
     */
    public Document getDocument() {
        if (documentInfo instanceof DocumentWrapper) {
            final DocumentWrapper documentWrapper = (DocumentWrapper) documentInfo;
            return (Document) documentWrapper.getUnderlyingNode();
        } else {
            return null;
        }
    }

    public static String getInstanceStaticId(Element xformsInstanceElement) {
        return xformsInstanceElement.attributeValue("id");
    }

    public static boolean isReadonlyHint(Element element) {
         return "true".equals(element.attributeValue(XFormsConstants.XXFORMS_READONLY_ATTRIBUTE_QNAME));
    }

    public static boolean isCacheHint(Element element) {
        return "true".equals(element.attributeValue(XFormsConstants.XXFORMS_CACHE_QNAME));
    }

    public static long getTimeToLive(Element element) {
        final String timeToLiveValue = element.attributeValue(XFormsConstants.XXFORMS_TIME_TO_LIVE_QNAME);
        return (timeToLiveValue != null) ? Long.parseLong(timeToLiveValue) : -1;
    }

    public List getEventHandlers(XBLContainer container) {
        return container.getContainingDocument().getStaticState().getEventHandlers(XFormsUtils.getPrefixedId(getEffectiveId()));
    }

    public void logInstance(IndentedLogger indentedLogger, String message) {
        if (indentedLogger.isDebugEnabled()) {
            indentedLogger.logDebug("", message,
                    "effective model id", getEffectiveModelId(),
                    "effective instance id", getEffectiveId(),
                    "instance", TransformerUtils.tinyTreeToString(getInstanceRootElementInfo()));
        }
    }

    public XFormsContextStack.BindingContext getBindingContext(PropertyContext propertyContext, XFormsContainingDocument containingDocument) {
        final XFormsModel model = getModel(containingDocument);
        final XFormsContextStack.BindingContext modelBindingContext = model.getBindingContext(propertyContext, containingDocument);
        // TODO: should push root element of this instance, right? But is this used anywhere?
        //final XFormsContextStack contextStack = model.getContextStack();
        return modelBindingContext;
    }

    // Don't allow any external events
    public boolean allowExternalEvent(IndentedLogger indentedLogger, String logType, String eventName) {
        return false;
    }
}
