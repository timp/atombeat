package org.atombeat.it;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static junit.framework.TestCase.fail;



public class AtomTestUtils {

	public static final String PORT = 
		(System.getProperty("org.atombeat.it.port") != null) ? 
				System.getProperty("org.atombeat.it.port") : "8081";
		
	public static final String HOST = 
		(System.getProperty("org.atombeat.it.host") != null) ? 
				System.getProperty("org.atombeat.it.host") : "localhost";
		
	public static final String CONTEXTPATH = 
		(System.getProperty("org.atombeat.it.contextPath") != null) ? 
				System.getProperty("org.atombeat.it.contextPath") : "/atombeat-exist-minimal-secure";

	public static final String SERVICEPATH = 
		(System.getProperty("org.atombeat.it.servicePath") != null) ? 
				System.getProperty("org.atombeat.it.servicePath") : "/service/";
				
	public static final Boolean SECURE = 
		(System.getProperty("org.atombeat.it.secure") != null) ? 
				Boolean.parseBoolean(System.getProperty("org.atombeat.it.secure")) : true;
							
	public static final String SERVICE_URL = "http://" + HOST + ":" + PORT + CONTEXTPATH + SERVICEPATH;
	public static final String CONTENT_URL = SERVICE_URL + "content/";
	public static final String SECURITY_URL = SERVICE_URL + "security/";
	public static final String LIB_URL = SERVICE_URL + "lib/";
	public static final String TEST_COLLECTION_URL = CONTENT_URL + "test";

	public static final String REALM = "AtomBeat";

	public static final String ADAM = "adam";
	public static final String AUDREY = "audrey";
	public static final String AUSTIN = "austin";
	public static final String URSULA = "ursula";
	public static final String LAURA = "laura";

	public static final String SCHEME_BASIC = "BASIC";

	public static final String PASSWORD = "test";


	
	
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
	
	
	
	
	public static String createTestVersionedCollection(String serverUri, String user, String pass) {

		Header[] headers = {};
		return createTestVersionedCollection(serverUri, user, pass, headers);

	}
	
	
	
	
	public static String createTestCollection(String serverUri, String user, String pass, Header[] headers) {

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		return createTestCollection(serverUri, user, pass, headers, content);

	}
	
	
	
	public static String createTestVersionedCollection(String serverUri, String user, String pass, Header[] headers) {

		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-versioning=\"true\">\n" +
			"	<atom:title>Test Collection</atom:title>\n" +
			"</atom:feed>";
		return createTestCollection(serverUri, user, pass, headers, content);

	}
	
	
	
	
	public static String createTestCollection(String serverUri, String user, String pass, Header[] headers, String content) {

		String collectionUri = serverUri + Double.toString(Math.random());
		PutMethod method = new PutMethod(collectionUri);
		for (Header h : headers) {
			method.setRequestHeader(h);
		}
		setAtomRequestEntity(method, content);
		
		int result = executeMethod(method, user, pass);
		
		method.releaseConnection();
		
		if (result != 201)
			return null;
		
		return collectionUri;

	}
	
	
	
	
	public static String createTestMemberAndReturnLocation(String collectionUri, String user, String pass) throws Exception {

		PostMethod method = new PostMethod(collectionUri);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry</atom:title><atom:summary>this is a test</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, user, pass);
		String location = null;
		
		if (result == 201) {
			
			Header locationHeader = method.getResponseHeader("Location");
			location = locationHeader.getValue();
			
		}
		
		method.releaseConnection();
		
		if (location == null) {
			throw new Exception("Could not create member.");
		}
		
		return location;

	}
	
	
	
	public static Document createTestMemberAndReturnDocument(String collectionUri, String user, String pass) throws Exception {

		PostMethod method = new PostMethod(collectionUri);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry</atom:title><atom:summary>this is a test</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, user, pass);
		
		Document doc = null;

		if (result == 201) 
			doc = getResponseBodyAsDocument(method);
		
		method.releaseConnection();
		
		if (doc == null) 
			throw new Exception("Could not create member.");
		
		return doc;

	}
	
	
	
	public static Document createTestMediaResourceAndReturnMediaLinkEntry(String collectionUri, String user, String pass) throws Exception {
		
		PostMethod method = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, user, pass);
		
		Document doc = null;
		
		if (result == 201)
			doc = getResponseBodyAsDocument(method);

		method.releaseConnection();
		
		if (doc == null)
			throw new Exception("Could not create media resource.");
		
		return doc;
		
	}
	
	
	
	public static List<Element> getChildrenByTagNameNS(Document d, String nsuri, String tagName) {
		return getChildrenByTagNameNS(d.getDocumentElement(), nsuri, tagName);
	}
	
	public static List<Element> getChildrenByTagNameNS( Element parent, String nsuri, String tagName ) {
		NodeList l = parent.getChildNodes();
		List<Element> o = new ArrayList<Element>();
		for (int i=0;i<l.getLength();i++) {
			Node n = l.item(i);
			if (n instanceof Element) {
				Element e = (Element) n;
				if (e.getNamespaceURI() != null && e.getNamespaceURI().equals(nsuri) && e.getLocalName() != null && e.getLocalName().equals(tagName)) {
					o.add(e);
				}
			}
		}
		return o;
	}
	
	
	public static Element getFirstChildByTagNameNS(Document d, String nsuri, String tagName) {
		return getFirstChildByTagNameNS(d.getDocumentElement(), nsuri, tagName);
	}

	
	
	public static Element getFirstChildByTagNameNS(Element e, String nsuri, String tagName) {
		List<Element> l = getChildrenByTagNameNS(e, nsuri, tagName);
		if (l.size()>0) return l.get(0);
		else return null;
	}
	
	
	public static List<Element> getEntries(Document d) {
		return getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
	}
	
	

	public static List<Element> getEntries(Element e) {
		return getChildrenByTagNameNS(e, "http://www.w3.org/2005/Atom", "entry");
	}
	
	
	

	public static String getEditMediaLocation(Document mediaLinkDoc) {
		return getLinkHref(mediaLinkDoc, "edit-media");
	}
	
	
	
	public static String getEditMediaLocation(Element mediaLinkEntry) {
		return getLinkHref(mediaLinkEntry, "edit-media");
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
	
	
	
	public static String getLinkHref(Element e, String rel) {
		
		String href = null;
		
		List<Element> links = getLinks(e, rel);
		if (links.size() > 0) {
			href = links.get(0).getAttribute("href");
		}
		
		return href;
		
	}
	
	
	
	public static Element getLink(Element e, String rel) {

		List<Element> links = getLinks(e, rel);
		if (links.size() > 0) {
			return links.get(0);
		}
		
		return null;

	}
	
	
	public static Element getLink(Document d, String rel) {

		return getLink(d.getDocumentElement(), rel);

	}
	
	
	public static List<Element> getLinks(Document doc, String rel) {
		
		return getLinks(doc.getDocumentElement(), rel);
		
	}
	
	
	public static List<Element> getLinks(Element elm, String rel) {
		
		List<Element> links = getChildrenByTagNameNS(elm, "http://www.w3.org/2005/Atom", "link");
		
		List<Element> els = new ArrayList<Element>();
		
		for (Element e : links) {
			String relValue = e.getAttribute("rel");
			if (relValue.equals(rel)) {
				els.add(e);
			}
		}
		
		return els;
		
	}
	
	
	
	public static String getAtomId(Document doc) {
		
		return getAtomId(doc.getDocumentElement());

	}
	
	

	public static String getAtomId(Element parent) {
		
		NodeList nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "id");
		Element e = (Element) nodes.item(0);
		return e.getTextContent();

	}
	
	
	public static String getAtomTitle(Document doc) {

		return getAtomTitle(doc.getDocumentElement());

	}

	public static String getAtomTitle(Element parent) {

		NodeList nodes = parent.getElementsByTagNameNS(
				"http://www.w3.org/2005/Atom", "title");
		Element e = (Element) nodes.item(0);
		return e.getTextContent();

	}


	public static String getAtomUpdated(Document doc) {
		
		return getAtomUpdated(doc.getDocumentElement());
		
	}

	
	
	
	public static String getAtomUpdated(Element parent) {

		NodeList nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated");
		Element e = (Element) nodes.item(0);
		return e.getTextContent();
		
	}



	public static String getAtomPublished(Document doc) {
		
		return getAtomPublished(doc.getDocumentElement());
		
	}

	
	
	
	public static String getAtomName(Element parent) {

		NodeList nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "name");
		Element e = (Element) nodes.item(0);
		return e.getTextContent();
		
	}



	public static String getAtomName(Document doc) {
		
		return getAtomName(doc.getDocumentElement());
		
	}

	
	
	
	public static String getAtomPublished(Element parent) {

		NodeList nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "published");
		Element e = (Element) nodes.item(0);
		return e.getTextContent();
		
	}




	public static Element getAtomContent(Document doc) {
		
		return getAtomContent(doc.getDocumentElement());
		
	}
	
	
	
	
	public static Element getAtomContent(Element parent) {
		
		NodeList nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "content");
		Element e = (Element) nodes.item(0);
		return e;
		
	}
	
	
	
	
	public static Element getAtomAuthor(Document doc) {
		
		return getAtomAuthor(doc.getDocumentElement());
		
	}
	
	
	
	
	public static Element getAtomAuthor(Element parent) {
		
		NodeList nodes = parent.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "author");
		Element e = (Element) nodes.item(0);
		return e;
		
	}
	
	
	
	public static String getAtomAuthorName(Element parent) {
		 
		return getAtomName(getAtomAuthor(parent));
		
	}
	
	
	
	public static String getAtomAuthorName(Document d) {
		 
		return getAtomAuthorName(d.getDocumentElement());
		
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
	
	
	
	public static Document postEntry(String collectionUri, String entryDoc, Header[] headers, String user, String pass) throws Exception {
		
		// setup a new POST request
		PostMethod method = new PostMethod(collectionUri);
		
		// add custom headers
		for (Header h : headers) {
			method.addRequestHeader(h);
		}
		
		setAtomRequestEntity(method, entryDoc);
		
		int result = executeMethod(method, user, pass);
		
		Document doc = null;

		if (result == 201) {
			doc =  getResponseBodyAsDocument(method);
		}

		method.releaseConnection();
		
		return doc;
		
	}


	
	
	public static Document putEntry(String uri, String entryDoc, Header[] headers, String user, String pass) throws Exception {
	
		// setup a new POST request
		PutMethod method = new PutMethod(uri);
		for (Header h : headers) {
			method.addRequestHeader(h);
		}
		setAtomRequestEntity(method, entryDoc);
		int result = executeMethod(method, user, pass);

		Document doc = null;
		
		// expect the status code is 200 
		if (result == 200)
			doc = getResponseBodyAsDocument(method);
	
		method.releaseConnection();
		
		return doc;

	}

	
	
	public static Document getResponseBodyAsDocument(HttpMethod method) throws Exception {
		
		Document doc = builder.parse(method.getResponseBodyAsStream());
		
		return doc;

	}
	
	

}
