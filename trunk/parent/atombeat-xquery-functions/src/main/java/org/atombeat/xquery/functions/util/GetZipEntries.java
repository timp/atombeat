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

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;



/**
 */
public class GetZipEntries extends BasicFunction {

	protected static final Log log = LogFactory.getLog(GetZipEntries.class);

	public final static FunctionSignature signature =
		new FunctionSignature(
			new QName(
				"get-zip-entries",
				AtomBeatUtilModule.NAMESPACE_URI,
				AtomBeatUtilModule.PREFIX),
			"Return zip entries for a given file.",
			new SequenceType[] {
					new FunctionParameterSequenceType("path", Type.STRING, Cardinality.EXACTLY_ONE, "The file system path to unzip.")},
			new FunctionReturnSequenceType(Type.STRING, Cardinality.ZERO_OR_MORE, "any zip entries present"));
		
	public GetZipEntries(XQueryContext context) {
		super(context, signature);
	}
	
	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
	{
		
		try {
			String path = args[0].getStringValue();
			ZipFile zf = new ZipFile(path);
			log.debug(zf.getName());
			log.debug(zf.size());
			log.debug(zf.hashCode());
			ZipEntry e;
			Enumeration<? extends ZipEntry> entries = zf.entries();
			ValueSequence s = new ValueSequence();
			for (int i=0; entries.hasMoreElements(); i++) {
				log.debug(i);
				e = entries.nextElement();
				log.debug(e.getName());
				log.debug(e.getComment());
				log.debug(e.isDirectory());
				log.debug(e.getCompressedSize());
				log.debug(e.getCrc());
				log.debug(e.getMethod());
				log.debug(e.getSize());
				log.debug(e.getTime());
				if (!e.isDirectory()) s.add(new StringValue(e.getName()));
			}
			return s;
		}
		catch (Exception e) {
			throw new XPathException("error processing zip file: "+e.getLocalizedMessage(), e);
		}

	}
	
}