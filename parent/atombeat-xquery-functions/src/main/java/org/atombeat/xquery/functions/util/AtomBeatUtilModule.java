package org.atombeat.xquery.functions.util;

import java.util.Arrays;

import org.atombeat.xquery.functions.fn.FunDeepEqual;
import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.XPathException;

@SuppressWarnings("unchecked")
public class AtomBeatUtilModule extends AbstractInternalModule {

	public static final String NAMESPACE_URI = "http://purl.org/atombeat/xquery/atombeat-util";
	public static final String PREFIX = "atombeat-util";

	public static final FunctionDef[] functions = {
		new FunctionDef(StreamRequestDataToFile.signature, StreamRequestDataToFile.class),
		new FunctionDef(Mkdirs.signature, Mkdirs.class),
		new FunctionDef(GetZipEntries.signature, GetZipEntries.class),
		new FunctionDef(GetZipEntrySize.signature, GetZipEntrySize.class),
		new FunctionDef(GetZipEntryCrc.signature, GetZipEntryCrc.class),
		new FunctionDef(CopyFile.signature, CopyFile.class),
		new FunctionDef(FileLength.signature, FileLength.class),
		new FunctionDef(FileExists.signature, FileExists.class),
		new FunctionDef(DeleteFile.signature, DeleteFile.class),
		new FunctionDef(StreamFileToResponse.signature, StreamFileToResponse.class),
		new FunctionDef(StreamZipEntryToResponse.signature, StreamZipEntryToResponse.class),
		new FunctionDef(RequestGetData.signature, RequestGetData.class),
		new FunctionDef(XMLDBStore.signatures[0], XMLDBStore.class),
		new FunctionDef(XMLDBStore.signatures[1], XMLDBStore.class),
		new FunctionDef(SaveUploadAs.signature, SaveUploadAs.class),
		new FunctionDef(
				FunDeepEqual.signatures[0], FunDeepEqual.class),
		new FunctionDef(
				FunDeepEqual.signatures[1], FunDeepEqual.class),
		new FunctionDef(
				FunDeepEqual.signatures[2], FunDeepEqual.class)
	};

    static {
        Arrays.sort(functions, new FunctionComparator());
    }

	public AtomBeatUtilModule() throws XPathException {
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
