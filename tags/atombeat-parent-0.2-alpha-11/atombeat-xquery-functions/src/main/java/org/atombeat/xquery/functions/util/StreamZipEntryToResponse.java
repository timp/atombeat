/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-09 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id: GetData.java 9749 2009-08-09 23:18:12Z ixitar $
 */
package org.atombeat.xquery.functions.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exist.dom.QName;
import org.exist.http.servlets.ResponseWrapper;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.response.ResponseModule;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.JavaObjectValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;


/**
 */
public class StreamZipEntryToResponse extends BasicFunction {

	protected static final Log logger = LogFactory.getLog(StreamZipEntryToResponse.class);

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName(
				"stream-zip-entry-to-response",
				AtomBeatUtilModule.NAMESPACE_URI,
				AtomBeatUtilModule.PREFIX),
			"Streams an entry from a zip file to the current servlet response output stream.",
			new SequenceType[] {
				new FunctionParameterSequenceType("path", Type.STRING, Cardinality.EXACTLY_ONE, "The path of the zip file."),
				new FunctionParameterSequenceType("entry", Type.STRING, Cardinality.EXACTLY_ONE, "The name of the zip entry."),
					new FunctionParameterSequenceType("content-type", Type.STRING, Cardinality.EXACTLY_ONE, "The Content-Type HTTP header value.")},
			new SequenceType(Type.ITEM, Cardinality.EMPTY));
		
	public StreamZipEntryToResponse(XQueryContext context) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
	{
		
        ResponseModule resModule = (ResponseModule) context.getModule(ResponseModule.NAMESPACE_URI);
        
        // request object is read from global variable $response
        Variable respVar = resModule.resolveVariable(ResponseModule.RESPONSE_VAR);
        
        if(respVar == null || respVar.getValue() == null)
            throw new XPathException(this, "No request object found in the current XQuery context.");
        
        if(respVar.getValue().getItemType() != Type.JAVA_OBJECT)
            throw new XPathException(this, "Variable $response is not bound to an Java object.");
        
        JavaObjectValue respValue = (JavaObjectValue) respVar.getValue().itemAt(0);
        
        if (!"org.exist.http.servlets.HttpResponseWrapper".equals(respValue.getObject().getClass().getName()))
            throw new XPathException(this, signature.toString() + " can only be used within the EXistServlet or XQueryServlet");
        
        ResponseWrapper response = (ResponseWrapper) respValue.getObject();
        
		String path = args[0].getStringValue();
		logger.debug(path);
		String entry = args[1].getStringValue();
		logger.debug(entry);
		String contentType = args[2].getStringValue();
		logger.debug(contentType);
		response.setContentType(contentType);
		
		try {
			
			ZipFile f = new ZipFile(path);
			ZipEntry e = f.getEntry(entry);
			InputStream in = f.getInputStream(e);
            OutputStream out = response.getOutputStream();
            Stream.copy(in, out);
		    response.flushBuffer(); // is this necessary?
			
		} catch (Exception e) {
            e.printStackTrace();
            throw new XPathException(this, "Exception while streaming data from zip file: " + e.getMessage(), e);
		}

        return Sequence.EMPTY_SEQUENCE;
	}
	
}