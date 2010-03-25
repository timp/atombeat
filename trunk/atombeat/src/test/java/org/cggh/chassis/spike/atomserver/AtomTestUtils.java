package org.cggh.chassis.spike.atomserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static junit.framework.TestCase.fail;



public class AtomTestUtils {


	
	
	public static final HttpClient client = new HttpClient();
	public static final BasicScheme basic = new BasicScheme();
	private static final DocumentBuilderFactory factory;
	private static DocumentBuilder builder;
	
	static {

		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		
		try {
		
			builder = factory.newDocumentBuilder();
			
		} catch (ParserConfigurationException e) {

			e.printStackTrace();

		} 

	}

	

	
	
	
	

	public static void authenticate(HttpMethod method, String user, String pass) {
		try {
			
			String authorization = basic.authenticate(new UsernamePasswordCredentials(user, pass), method);
			method.setRequestHeader("Authorization", authorization);
			
		} 
		catch (Throwable t) {
			
			t.printStackTrace();
			fail(t.getLocalizedMessage());
			
		}
	}

	
	
	
	public static Integer executeMethod(HttpMethod method) {
		
		Integer result = null;
		
		try {

			result = client.executeMethod(method);

		} catch (HttpException e) {

			e.printStackTrace();
			fail(e.getLocalizedMessage());

		} catch (IOException e) {

			e.printStackTrace();
			fail(e.getLocalizedMessage());

		}
		
		return result;
		
	}
	
	
	

	
	public static Integer executeMethod(HttpMethod method, String user, String pass) {
		
		authenticate(method, user, pass);
 
		return executeMethod(method);

	}
	
	
	

	public static void setAtomRequestEntity(EntityEnclosingMethod method, String xml) {
		
		RequestEntity entity = null;
		String contentType = "application/atom+xml";
		String charSet = "utf-8";
		
		try {

			entity = new StringRequestEntity(xml, contentType, charSet);

		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
			fail(e.getLocalizedMessage());

		}
		
		method.setRequestEntity(entity);

	}
	
	
	
	public static void setTextPlainRequestEntity(EntityEnclosingMethod method, String text) {
		
		ByteArrayInputStream content = null;

		try {
			
			content = new ByteArrayInputStream(text.getBytes("utf-8"));

		} catch (UnsupportedEncodingException e1) {

			e1.printStackTrace();
			fail(e1.getLocalizedMessage());

		}
		
		String contentType = "text/plain;charset=utf-8";
		
		setInputStreamRequestEntity(method, content, contentType);
	
	}
	
	
	
	
	public static void setInputStreamRequestEntity(EntityEnclosingMethod method, InputStream content, String contentType) {
		
		RequestEntity entity = null;

		try {

			entity = new InputStreamRequestEntity(content, content.available(), contentType);

		} catch (IOException e) {

			e.printStackTrace();
			fail(e.getLocalizedMessage());

		}
		
		method.setRequestEntity(entity);
	
	}
	
	
	
	
	public static String createTestCollection(String serverUri, String user, String pass) {

		Header[] headers = {};
		return createTestCollection(serverUri, user, pass, headers);

	}
	
	
	
	
	public static String createTestCollection(String serverUri, String user, String pass, Header[] headers) {

		String collectionUri = serverUri + Double.toString(Math.random());
		PutMethod method = new PutMethod(collectionUri);
		for (Header h : headers) {
			method.setRequestHeader(h);
		}
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		int result = executeMethod(method, user, pass);
		
		if (result != 201)
			return null;
		
		return collectionUri;

	}
	
	
	
	
	public static String createTestEntryAndReturnLocation(String collectionUri, String user, String pass) {

		PostMethod method = new PostMethod(collectionUri);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry</atom:title><atom:summary>this is a test</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, user, pass);

		if (result != 201)
			return null;
		
		Header locationHeader = method.getResponseHeader("Location");
		
		return locationHeader.getValue();

	}
	
	
	
	public static Document createTestMediaResourceAndReturnMediaLinkEntry(String collectionUri, String user, String pass) {
		
		PostMethod method = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, user, pass);
		
		if (result != 201)
			return null;

		Document doc = getResponseBodyAsDocument(method);
		
		return doc;
		
	}
	
	
	
	public static String getEditMediaLocation(Document mediaLinkDoc) {
		return getLinkHref(mediaLinkDoc, "edit-media");
	}
	
	
	
	public static String getEditLocation(Document entryDoc) {
		return getLinkHref(entryDoc, "edit");
	}
	
	
	
	public static String getHistoryLocation(Document entryDoc) {
		return getLinkHref(entryDoc, "history");
	}
	
	
	
	public static String getLinkHref(Document doc, String rel) {
		
		String href = null;
		
		List<Element> links = getLinks(doc, rel);
		if (links.size() > 0) {
			href = links.get(0).getAttribute("href");
		}
		
		return href;
		
	}
	
	
	
	public static List<Element> getLinks(Document doc, String rel) {
		
		NodeList links = doc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "link");
		
		List<Element> els = new ArrayList<Element>();
		
		for (int i=0; i<links.getLength(); i++) {
			Element e = (Element) links.item(i);
			String relValue = e.getAttribute("rel");
			if (relValue.equals(rel)) {
				els.add(e);
			}
		}
		
		return els;
		
	}
	
	
	
	public static FilePart createFilePart(File file, String fileName, String contentType, String filePartName) {
		
		FilePart fp = null;

		try {
			
			fp = new FilePart(filePartName , fileName, file, contentType, null);

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			fail(e.getLocalizedMessage());

		}
		
		return fp;
		
	}
	
	
	
	public static void setMultipartRequestEntity(EntityEnclosingMethod method, Part[] parts) {
		
		MultipartRequestEntity entity = new MultipartRequestEntity(parts, method.getParams());
		method.setRequestEntity(entity);

	}
	
	
	
	public static Document postEntry(String collectionUri, String entryDoc, Header[] headers, String user, String pass) {
		
		// setup a new POST request
		PostMethod method = new PostMethod(collectionUri);
		
		// add custom headers
		for (Header h : headers) {
			method.addRequestHeader(h);
		}
		
		setAtomRequestEntity(method, entryDoc);
		
		int result = executeMethod(method, user, pass);

		if (result != 201) {
			return null;
		}

		Document doc = getResponseBodyAsDocument(method);
		
		return doc;
	}


	
	
	public static Document putEntry(String uri, String entryDoc, Header[] headers, String user, String pass) {
	
		// setup a new POST request
		PutMethod method = new PutMethod(uri);
		for (Header h : headers) {
			method.addRequestHeader(h);
		}
		setAtomRequestEntity(method, entryDoc);
		int result = executeMethod(method, user, pass);

		// expect the status code is 200 
		if (result != 200)
			return null;
	
		Document doc = getResponseBodyAsDocument(method);
		
		return doc;

	}

	
	
	public static Document getResponseBodyAsDocument(HttpMethod method) {
		
		Document doc = null;
		
		try {
		
			doc = builder.parse(method.getResponseBodyAsStream());
			
		} catch (SAXException e) {
	
			e.printStackTrace();
			fail(e.getLocalizedMessage());
	
		} catch (IOException e) {
	
			e.printStackTrace();
			fail(e.getLocalizedMessage());
	
		}
	
		return doc;

	}
	
	

}
