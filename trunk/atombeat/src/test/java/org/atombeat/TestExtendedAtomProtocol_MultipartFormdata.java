package org.atombeat;

import java.io.File;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestExtendedAtomProtocol_MultipartFormdata extends TestCase {


	
	
	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	private static final String TEST_COLLECTION_URI = CONTENT_URI + "test";
	
	
	private static void executeMethod(HttpMethod method, int expectedStatus) {
		
		AtomTestUtils.executeMethod(method, USER, PASS, expectedStatus);

	}

	
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		String setupUrl = BASE_URI + "admin/setup-for-test.xql";
		
		PostMethod method = new PostMethod(setupUrl);
		
		executeMethod(method, 200);
		
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	
	
	public void testMultipartRequestWithFile() {
		
		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = new PostMethod(TEST_COLLECTION_URI);
		File file = new File(this.getClass().getClassLoader().getResource("spreadsheet1.xls").getFile());
		FilePart fp = createFilePart(file, "spreadsheet1.xls", "application/vnd.ms-excel", "media");
		StringPart sp1 = new StringPart("summary", "this is a great spreadsheet");
		StringPart sp2 = new StringPart("category", "scheme=\"foo\"; term=\"bar\"; label=\"baz\"");
		Part[] parts = { fp , sp1 , sp2 };
		setMultipartRequestEntity(post, parts);
		executeMethod(post, 201);
		
		// expect the Location header is set with an absolute URI
		String responseLocation = post.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		assertTrue(responseLocation.startsWith("http://")); 
		// N.B. we shouldn't assume any more than this, because entry could have
		// a location anywhere
		
		// expect Content-Type header 
		String responseContentType = post.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));

		// check additional parts are used
		Document d = getResponseBodyAsDocument(post);
		
		NodeList sl = d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "summary");
		assertEquals(1, sl.getLength());
		Element se = (Element) sl.item(0);
		assertEquals("this is a great spreadsheet", se.getTextContent());

		NodeList cl = d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "category");
		assertEquals(1, cl.getLength());
		Element ce = (Element) cl.item(0);
		assertEquals("foo", ce.getAttribute("scheme"));
		assertEquals("bar", ce.getAttribute("term"));
		assertEquals("baz", ce.getAttribute("label"));

	}



	
	
	public void testBadMultipartRequest() {
		
		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = new PostMethod(TEST_COLLECTION_URI);
		File file = new File(this.getClass().getClassLoader().getResource("spreadsheet1.xls").getFile());
		FilePart fp = createFilePart(file, "spreadsheet1.xls", "application/vnd.ms-excel", "media-oops"); // should be "media"
		StringPart sp1 = new StringPart("summary", "this is a great spreadsheet");
		StringPart sp2 = new StringPart("category", "scheme=\"foo\"; term=\"bar\"; label=\"baz\"");
		Part[] parts = { fp , sp1 , sp2 };
		setMultipartRequestEntity(post, parts);
		executeMethod(post, 400);
		
		// expect Content-Type header 
		String responseContentType = post.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/xml"));

	}

	
	
	
}



