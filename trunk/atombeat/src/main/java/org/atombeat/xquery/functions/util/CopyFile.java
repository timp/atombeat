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
import java.io.FileOutputStream;
import java.io.IOException;


import org.apache.log4j.Logger;
import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;


/**
 */
public class CopyFile extends BasicFunction {

	protected static final Logger logger = Logger.getLogger(CopyFile.class);

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName(
				"copy-file",
				AtombeatUtilModule.NAMESPACE_URI,
				AtombeatUtilModule.PREFIX),
			"Copy a file to another location.",
			new SequenceType[] {
					new FunctionParameterSequenceType("from", Type.STRING, Cardinality.EXACTLY_ONE, "The path to the file to copy."),
					new FunctionParameterSequenceType("to", Type.STRING, Cardinality.EXACTLY_ONE, "The path where the new file will be stored.")},
			new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true if the operation succeeded, false otherwise"));
		
	public CopyFile(XQueryContext context) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
	{
		
		// try to copy 
		try
		{
			String from = args[0].getStringValue();
			String to = args[1].getStringValue();
			File fromFile = new File(from);
			File toFile = new File(to);
			FileInputStream in = new FileInputStream(fromFile);
			FileOutputStream out = new FileOutputStream(toFile);
			
			Stream.copy(in, out);
			
//		    byte buf[]=new byte[8*1024];
//		    int len;
//		    while((len=in.read(buf))>0)
//		    	out.write(buf,0,len);
//		    out.flush();
//		    out.close();
//		    in.close();		
		    
		    return BooleanValue.TRUE;
		}
		catch(IOException ioe)
		{
			throw new XPathException(this, "An IO exception ocurred: " + ioe.getMessage(), ioe);
		}
		
	}

}