package org.atombeat.xquery.functions.request;

import java.util.Arrays;

import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;
import org.exist.xquery.XPathException;

public class XRequestModule extends AbstractInternalModule {

	public static final String NAMESPACE_URI = "http://purl.org/atombeat/xquery/request";
	public static final String PREFIX = "xrequest";

	public static final FunctionDef[] functions = {
		new FunctionDef(StreamDataToFile.signature, StreamDataToFile.class),
	};

    static {
        Arrays.sort(functions, new FunctionComparator());
    }

	public XRequestModule() throws XPathException {
		super(functions, true);
	}

	@Override
	public String getDefaultPrefix() {
		return PREFIX;
	}

	@Override
	public String getDescription() {
		return "A module extending the default eXist request module.";
	}

	@Override
	public String getNamespaceURI() {
		return NAMESPACE_URI;
	}

	@Override
	public String getReleaseVersion() {
		return "foo";
	}

}
