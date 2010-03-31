/**
 *  Copyright (C) 2009 Orbeon, Inc.
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
package org.orbeon.oxf.xml.dom4j;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Element;

public class Dom4jUtilsTest extends TestCase {
    public void testDomToString() {

        final Document document = Dom4jUtils.createDocument();
        final Element rootElement = Dom4jUtils.createElement("div");
        document.setRootElement(rootElement);

        rootElement.addText("    ");
        final Element bElement = Dom4jUtils.createElement("b");
        bElement.addText("bold");
        rootElement.add(bElement);
        rootElement.addText("    ");

        // Normal output
        assertEquals("<div>    <b>bold</b>    </div>", Dom4jUtils.domToString(document));
        assertEquals("<div>    <b>bold</b>    </div>", Dom4jUtils.domToString(document.getRootElement()));
        assertEquals("<div>    <b>bold</b>    </div>", Dom4jUtils.nodeToString(document));
        assertEquals("<div>    <b>bold</b>    </div>", Dom4jUtils.nodeToString(document.getRootElement()));

        // Formatted output
        assertEquals("\n<div>\n    <b>bold</b>\n</div>", Dom4jUtils.domToPrettyString(document));

        // Compact output
        assertEquals("<div><b>bold</b></div>", Dom4jUtils.domToCompactString(document));
    }
}
