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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exist.dom.QName;
import org.exist.external.org.apache.commons.io.output.ByteArrayOutputStream;
import org.exist.http.servlets.RequestWrapper;
import org.exist.memtree.DocumentBuilderReceiver;
import org.exist.memtree.MemTreeBuilder;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Variable;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.request.RequestModule;
import org.exist.xquery.value.Base64Binary;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.JavaObjectValue;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/**
 * The difference between this and the original are noted with <atombeat> tags. 
 *
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public class RequestGetData extends BasicFunction {

	protected static final Log logger = LogFactory.getLog(RequestGetData.class);

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName(
				"request-get-data",
				AtomBeatUtilModule.NAMESPACE_URI,
				AtomBeatUtilModule.PREFIX),
			"The same as the eXist request:get-data() function, except hard coded to treat POST content with a content type not in the eXist mime types table as binary rather than XML. (Should be unnecessary after eXist 1.4.1 as there is a switch in the mime-types file.)",
			null,
			new FunctionReturnSequenceType(Type.ITEM, Cardinality.ZERO_OR_ONE, "the content of a POST request"));
	
	public RequestGetData(XQueryContext context) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence)throws XPathException
	{
		
		RequestModule myModule = (RequestModule) context.getModule(RequestModule.NAMESPACE_URI);

		// request object is read from global variable $request
		Variable var = myModule.resolveVariable(RequestModule.REQUEST_VAR);
		
		if(var == null || var.getValue() == null)
			throw new XPathException(this, "No request object found in the current XQuery context.");
		
		if(var.getValue().getItemType() != Type.JAVA_OBJECT)
			throw new XPathException(this, "Variable $request is not bound to an Java object.");
		
		JavaObjectValue value = (JavaObjectValue) var.getValue().itemAt(0);
		
		
		if(value.getObject() instanceof RequestWrapper)
		{
			RequestWrapper request = (RequestWrapper)value.getObject();	
			
			//if the content length is unknown, return
			if(request.getContentLength() == -1)
			{
				return Sequence.EMPTY_SEQUENCE;
			}
			
			//first, get the content of the request
			byte[] bufRequestData = null;
			try
			{
				InputStream is = request.getInputStream();
				ByteArrayOutputStream bos = new ByteArrayOutputStream(request.getContentLength());
				byte[] buf = new byte[256];
				int l = 0;
				while ((l = is.read(buf)) > -1)
				{
					bos.write(buf, 0, l);
				}
				bufRequestData = bos.toByteArray();
			}
			catch(IOException ioe)
			{
				throw new XPathException(this, "An IO exception ocurred: " + ioe.getMessage(), ioe);
			}
			
			//was there any POST content
			if(bufRequestData != null)
			{
				//determine if exists mime database considers this binary data
				String contentType = request.getContentType();
				if(contentType != null)
				{
					//strip off any charset encoding info
					if(contentType.indexOf(";") > -1)
						contentType = contentType.substring(0, contentType.indexOf(";"));
					
					MimeType mimeType = MimeTable.getInstance().getContentType(contentType);
//<atombeat>
					// this code will only encode the request data if the mimeType
					// is present in the mime table, and the mimeType is stated
					// as binary...
					
//					if(mimeType != null)
//					{
//						if(!mimeType.isXMLType())
//						{
//							//binary data
//							return new Base64Binary(bufRequestData);
//						}
//					}
					
					// this code takes a more conservative position and assumes that
					// if the mime type is not present in the table, the request
					// data should be treated as binary, and should be encoded as 
					// base 64...
					
					if (mimeType == null || !mimeType.isXMLType()) {
						return new Base64Binary(bufRequestData);
					}
//</atombeat>					
				}
				
				//try and parse as an XML documemnt, otherwise fallback to returning the data as a string
				context.pushDocumentContext();
				try
				{ 
					//try and construct xml document from input stream, we use eXist's in-memory DOM implementation
					SAXParserFactory factory = SAXParserFactory.newInstance();
					factory.setNamespaceAware(true);
					//TODO : we should be able to cope with context.getBaseURI()				
					InputSource src = new InputSource(new ByteArrayInputStream(bufRequestData));
					SAXParser parser = factory.newSAXParser();
					XMLReader reader = parser.getXMLReader();
                    MemTreeBuilder builder = context.getDocumentBuilder();
                    DocumentBuilderReceiver receiver = new DocumentBuilderReceiver(builder, true);
					reader.setContentHandler(receiver);
					reader.parse(src);
					Document doc = receiver.getDocument();
					return (NodeValue)doc.getDocumentElement();
				}
				catch(ParserConfigurationException e)
				{				
					//do nothing, we will default to trying to return a string below
				}
				catch(SAXException e)
				{
					//do nothing, we will default to trying to return a string below
				}
				catch(IOException e)
				{
					//do nothing, we will default to trying to return a string below
				}
				finally
				{
                    context.popDocumentContext();
                }
				
				//not a valid XML document, return a string representation of the document
				String encoding = request.getCharacterEncoding();
				if(encoding == null)
				{
					encoding = "UTF-8";
				}
				try
				{
					String s = new String(bufRequestData, encoding);
					return new StringValue(s);
				}
				catch (IOException e)
				{
					throw new XPathException(this, "An IO exception ocurred: " + e.getMessage(), e);
				}
			}
			else
			{
				//no post data
				return Sequence.EMPTY_SEQUENCE;
			}
		}
		else
		{
			throw new XPathException(this, "Variable $request is not bound to a Request object.");
		}
	}
}