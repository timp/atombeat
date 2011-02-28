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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exist.dom.QName;
import org.exist.http.servlets.RequestWrapper;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.request.RequestModule;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.JavaObjectValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.StringValue;


/**
 */
public class StreamRequestDataToFile extends BasicFunction {

	protected static final Log logger = LogFactory.getLog(StreamRequestDataToFile.class);

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName(
				"stream-request-data-to-file",
				AtomBeatUtilModule.NAMESPACE_URI,
				AtomBeatUtilModule.PREFIX),
			"Streams the content of a POST request to a file. Returns true if the operation succeeded, otherwise false.",
			new SequenceType[] {
					new FunctionParameterSequenceType("path", Type.STRING, Cardinality.EXACTLY_ONE, "The file system path where the data is to be stored.")},
			new FunctionReturnSequenceType(Type.STRING, Cardinality.ZERO_OR_ONE, "an MD5 digest of the content, or empty if there was a problem"));
		
	public StreamRequestDataToFile(XQueryContext context) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
	{
		
		RequestModule reqModule = (RequestModule) context.getModule(RequestModule.NAMESPACE_URI);

		// request object is read from global variable $request
		Variable var = reqModule.resolveVariable(RequestModule.REQUEST_VAR);
		
		if (var == null || var.getValue() == null)
			throw new XPathException(this, "No request object found in the current XQuery context.");
		
		if (var.getValue().getItemType() != Type.JAVA_OBJECT)
			throw new XPathException(this, "Variable $request is not bound to an Java object.");
		
		JavaObjectValue value = (JavaObjectValue) var.getValue().itemAt(0);

		if ( !( value.getObject() instanceof RequestWrapper) ) {
			throw new XPathException(this, "Variable $request is not bound to a Request object.");
		}

		RequestWrapper request = (RequestWrapper) value.getObject();	
			
		// if the content length is unknown, return
		if (request.getContentLength() == -1)
		{
			return Sequence.EMPTY_SEQUENCE;
		}
			
		// try to stream request content to file
		try
		{
			InputStream in = request.getInputStream();
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			in = new DigestInputStream(in, md5);
			String path = args[0].getStringValue();
			File file = new File(path);
			FileOutputStream out = new FileOutputStream(file);
			
			Stream.copy(in, out);
			
			String signature = new BigInteger(1, md5.digest()).toString(16);		    
		    return new StringValue(signature);

		}
		catch(IOException ioe)
		{
			throw new XPathException(this, "An IO exception ocurred: " + ioe.getMessage(), ioe);
		} catch (NoSuchAlgorithmException e) {
			throw new XPathException(this, "A message digest exception ocurred: " + e.getMessage(), e);
		}

	}
}