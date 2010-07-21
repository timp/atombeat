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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;


import org.apache.log4j.Logger;
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
public class StreamFileToResponse extends BasicFunction {

	protected static final Logger logger = Logger.getLogger(StreamFileToResponse.class);

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName(
				"stream-file-to-response",
				AtombeatUtilModule.NAMESPACE_URI,
				AtombeatUtilModule.PREFIX),
			"Streams a file to the current servlet response output stream.",
			new SequenceType[] {
					new FunctionParameterSequenceType("path", Type.STRING, Cardinality.EXACTLY_ONE, "The path of the file to stream."),
					new FunctionParameterSequenceType("content-type", Type.STRING, Cardinality.EXACTLY_ONE, "The Content-Type HTTP header value.")},
			new SequenceType(Type.ITEM, Cardinality.EMPTY));
		
	public StreamFileToResponse(XQueryContext context) {
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
		String contentType = args[1].getStringValue();
		response.setHeader("Content-Type", contentType);
		
		try {
			
			File file = new File(path);
			FileInputStream in = new FileInputStream(file);
            OutputStream out = response.getOutputStream();

            Stream.copy(in, out);
            
//            byte buf[]=new byte[8*1024];
//		    int len;
//		    while((len=in.read(buf))>0)
//		    	out.write(buf,0,len);
//		    out.flush();
//		    out.close();
//		    in.close();	

		    response.flushBuffer(); // is this necessary?
			
		} catch (IOException e) {
            throw new XPathException(this, "IO exception while streaming data: " + e.getMessage(), e);
		}

        return Sequence.EMPTY_SEQUENCE;
	}
	
}