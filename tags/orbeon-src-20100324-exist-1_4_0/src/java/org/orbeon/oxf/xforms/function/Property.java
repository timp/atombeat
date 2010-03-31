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
package org.orbeon.oxf.xforms.function;

import org.dom4j.QName;
import org.orbeon.oxf.xforms.XFormsConstants;
import org.orbeon.oxf.xforms.XFormsProperties;
import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xml.dom4j.Dom4jUtils;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * XForms property() function.
 *
 * As an extension, supports XForms properties in the xxforms namespace to access XForms engine properties. E.g.:
 *
 *   property('xxforms:noscript')
 */
public class Property extends XFormsFunction {

    private static final StringValue VERSION = new StringValue("1.1");
    private static final StringValue CONFORMANCE_LEVEL = new StringValue("full");

    private static final String VERSION_PROPERTY = "version";
    private static final String CONFORMANCE_LEVEL_PROPERTY = "conformance-level";

    private Map<String, String> namespaceMappings;

    public Item evaluateItem(XPathContext xpathContext) throws XPathException {

        final String propertyNameString = argument[0].evaluateAsString(xpathContext);
        final QName propertyNameQName = Dom4jUtils.extractTextValueQName(namespaceMappings, propertyNameString, false);

        // Never return any property containing the string "password" as a first line of defense
        if (propertyNameString.toLowerCase().indexOf("password") != -1) {
            return null;
        }

        if (VERSION_PROPERTY.equals(propertyNameString)) {
            // Standard "version" property
            return VERSION;
        } else if (CONFORMANCE_LEVEL_PROPERTY.equals(propertyNameString)) {
            // Standard "conformance-level" property
            return CONFORMANCE_LEVEL;
        } else if (XFormsConstants.XXFORMS_NAMESPACE_URI.equals(propertyNameQName.getNamespaceURI())) {
            // Property in the xxforms namespace: return our properties

            // Retrieve property
            final Object value = XFormsProperties.getProperty(getContainingDocument(xpathContext), propertyNameQName.getName());
            if (value == null)
                return null;

            // Convert Java object to Saxon object before returning it
            return (Item) XFormsUtils.convertJavaObjectToSaxonObject(value);

        } else {
            throw new StaticError("Invalid property() function parameter: " + propertyNameString);
        }
    }

    // The following copies StaticContext namespace information
    public void checkArguments(StaticContext env) throws XPathException {
        // See also Saxon Evaluate.java
        if (namespaceMappings == null) { // only do this once
            super.checkArguments(env);

            namespaceMappings = new HashMap<String, String>();

            final NamespaceResolver namespaceResolver = env.getNamespaceResolver();
            for (Iterator iterator = namespaceResolver.iteratePrefixes(); iterator.hasNext();) {
                final String prefix = (String) iterator.next();
                if (!"".equals(prefix)) {
                    final String uri = namespaceResolver.getURIForPrefix(prefix, true);
                    namespaceMappings.put(prefix, uri);
                }
            }
        }
    }
}
