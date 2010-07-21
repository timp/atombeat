package org.atombeat.xquery.functions.util;

import java.util.Arrays;

import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.XPathException;

@SuppressWarnings("unchecked")
public class AtombeatUtilModule extends AbstractInternalModule {

	public static final String NAMESPACE_URI = "http://purl.org/atombeat/xquery/atombeat-util";
	public static final String PREFIX = "atombeat-util";

	public static final FunctionDef[] functions = {
		new FunctionDef(StreamRequestDataToFile.signature, StreamRequestDataToFile.class),
		new FunctionDef(Mkdirs.signature, Mkdirs.class),
		new FunctionDef(CopyFile.signature, CopyFile.class),
		new FunctionDef(FileLength.signature, FileLength.class),
		new FunctionDef(FileExists.signature, FileExists.class),
		new FunctionDef(DeleteFile.signature, DeleteFile.class),
		new FunctionDef(StreamFileToResponse.signature, StreamFileToResponse.class),
		new FunctionDef(SaveUploadAs.signature, SaveUploadAs.class)
	};

    static {
        Arrays.sort(functions, new FunctionComparator());
    }

	public AtombeatUtilModule() throws XPathException {
		super(functions, true);
	}

	@Override
	public String getDefaultPrefix() {
		return PREFIX;
	}

	@Override
	public String getDescription() {
		return "A module providing Java utilities for AtomBeat.";
	}

	@Override
	public String getNamespaceURI() {
		return NAMESPACE_URI;
	}

	@Override
	public String getReleaseVersion() {
		return "TODO";
	}

}
