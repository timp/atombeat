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
package org.orbeon.oxf.transformer.xupdate.statement;

import org.dom4j.Element;
import org.dom4j.QName;
import org.orbeon.oxf.transformer.xupdate.Statement;
import org.orbeon.oxf.transformer.xupdate.VariableContextImpl;
import org.orbeon.oxf.transformer.xupdate.DocumentContext;
import org.orbeon.oxf.xml.dom4j.LocationData;
import org.orbeon.oxf.xml.dom4j.NonLazyUserDataElement;

import javax.xml.transform.URIResolver;
import java.util.Arrays;

public class DynamicElement extends Statement {
    private QName qname;
    private Statement[] statements;


    public DynamicElement(LocationData locationData, QName qname, Statement[] statements) {
        super(locationData);
        this.qname = qname;
        this.statements = statements;
    }

    public Object execute(URIResolver uriResolver, Object context, VariableContextImpl variableContext, DocumentContext documentContext) {
        Element element = new NonLazyUserDataElement(qname);
        Utils.insert(getLocationData(), element, 0, Utils.execute(uriResolver, context, variableContext, documentContext, statements));
        return Arrays.asList(new Object[] {element});
    }
}
