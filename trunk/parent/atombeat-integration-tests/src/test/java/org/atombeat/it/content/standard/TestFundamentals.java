package org.atombeat.it.content.standard;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.atombeat.it.AtomTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.atombeat.it.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestFundamentals extends TestCase {


	
	
	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	private static final String TEST_COLLECTION_URI = CONTENT_URI + "test";
	
	
	
	private static Integer executeMethod(HttpMethod method) {
		
		return AtomTestUtils.executeMethod(method, USER, PASS);

	}

	
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		String installUrl = SERVICE_URL + "admin/setup-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method);
		
		method.releaseConnection();
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}
		
	}
	
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	
	public void testPostEntry() {
		
		// create a new member by POSTing an atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);
		
		// expect the status code is 201 Created
		assertEquals(201, result);

		// expect the Location header is set with an absolute URI
		String responseLocation = method.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		assertTrue(responseLocation.startsWith("http://")); 
		// N.B. we shouldn't assume any more than this, because entry could have
		// a location anywhere
		
		// expect Content-Type header 
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));
		
		// expect Content-Location header 
		String responseContentLocation = method.getResponseHeader("Content-Location").getValue();
		assertNotNull(responseContentLocation);
		
		method.releaseConnection();

	}



	
	public void testGetEntry() throws Exception {

		// setup test
		String location = createTestMemberAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// now try GET to member URI
		GetMethod method = new GetMethod(location);
		int result = executeMethod(method);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect no Location header 
		Header locationHeader = method.getResponseHeader("Location");
		assertNull(locationHeader);
		
		// expect Content-Type header 
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml;type=entry"));
		
		method.releaseConnection();

	}
	
	
	

	public void testPutEntry() throws Exception {

		// setup test
		String location = createTestMemberAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// now put an updated entry document using a PUT request
		PutMethod method = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result);

		// expect no Location header 
		Header responseLocationHeader = method.getResponseHeader("Location");
		assertNull(responseLocationHeader);
		
		// expect the Content-Type header starts with the Atom media type
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));
		
		method.releaseConnection();

	}
	
	
	

	public void testDeleteEntry() throws Exception {
		
		// setup test
		String location = createTestMemberAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// check we can GET the entry
		GetMethod get1 = new GetMethod(location);
		int get1Result = executeMethod(get1);
		assertEquals(get1Result, 200);
		
		// now try DELETE the entry
		DeleteMethod delete = new DeleteMethod(location);
		int deleteResult = executeMethod(delete);
		assertEquals(204, deleteResult);
		
		// now try to GET the entry
		GetMethod get2 = new GetMethod(location);
		int get2Result = executeMethod(get2);
		assertEquals(404, get2Result);
		
		get2.releaseConnection();

	}

	
	
	
	public void testGetFeed() throws Exception {

		// try GET to collection URI
		GetMethod get1 = new GetMethod(TEST_COLLECTION_URI);
		int result1 = executeMethod(get1);
		
		// expect the status code is 200 OK
		assertEquals(200, result1);
		
		String contentType = get1.getResponseHeader("Content-Type").getValue();
		assertTrue(contentType.startsWith("application/atom+xml;type=feed"));

		// check content
		Document d1 = getResponseBodyAsDocument(get1);
		Element id = (Element) d1.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "id").item(0);
		assertNotNull(id);
		Element title = (Element) d1.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertNotNull(title);
		Element updated = (Element) d1.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		assertNotNull(updated);
		String editLocation = AtomTestUtils.getLinkHref(d1, "edit");
		assertNotNull(editLocation);
		String selfLocation = AtomTestUtils.getLinkHref(d1, "self");
		assertNotNull(selfLocation);
		List<Element> entries1 = getChildrenByTagNameNS(d1, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(0, entries1.size());

		// add a member
		createTestMemberAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// try GET to collection URI
		GetMethod get2 = new GetMethod(TEST_COLLECTION_URI);
		int result2 = executeMethod(get2);
		
		// expect the status code is 200 OK
		assertEquals(200, result2);

		// check content
		Document d2 = getResponseBodyAsDocument(get2);
		List<Element> entries2 = getChildrenByTagNameNS(d2, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries2.size());

		// add a member
		createTestMemberAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// try GET to collection URI
		GetMethod get3 = new GetMethod(TEST_COLLECTION_URI);
		int result3 = executeMethod(get3);
		
		// expect the status code is 200 OK
		assertEquals(200, result3);

		// check content
		Document d3 = getResponseBodyAsDocument(get3);
		List<Element> entries3 = getChildrenByTagNameNS(d3, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, entries3.size());
		
		get3.releaseConnection();

	}
	
	
	
	
	public void testPostMedia_Text() {
		
		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method);
		
		verifyPostMediaResponse(result, method);
		
		method.releaseConnection();
		
	}





	public void testPostMedia_Binary() {
		
		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(method, content, contentType);
		int result = executeMethod(method);
		
		verifyPostMediaResponse(result, method);
		
		method.releaseConnection();

	}
	
	
	
	
	public void testGetMedia_Text() throws Exception {

		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		assertNotNull(mediaLinkDoc);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		assertNotNull(mediaLocation);
		
		// now try get on media location
		GetMethod method = new GetMethod(mediaLocation);
		int result = executeMethod(method);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect no Location header 
		Header locationHeader = method.getResponseHeader("Location");
		assertNull(locationHeader);
		
		// expect Content-Type header 
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("text/plain"));
		
		// test response body
		String responseContent = method.getResponseBodyAsString();
		assertEquals("This is a test.", responseContent);
		
		method.releaseConnection();

	}
	
	
	

	public void testGetMedia_Binary() throws Exception {

		// create a new media resource by POSTing media to the collection URI
		PostMethod post = new PostMethod(TEST_COLLECTION_URI);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(post, content, contentType);
		int result = executeMethod(post);
		assertEquals(201, result);
		Document mediaLinkDoc = getResponseBodyAsDocument(post);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);

		// now try get on media location
		GetMethod get = new GetMethod(mediaLocation);
		result = executeMethod(get);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect no Location header 
		Header locationHeader = get.getResponseHeader("Location");
		assertNull(locationHeader);
		
		// expect Content-Type header 
		String responseContentType = get.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/vnd.ms-excel"));
		
		// test response body
		String responseContent = get.getResponseBodyAsString();
		assertNotNull(responseContent);
		assertFalse(responseContent.isEmpty());
		
		get.releaseConnection();

	}
	
	
	

	public void testPutMedia_Text() throws Exception {

		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		assertNotNull(mediaLinkDoc);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		assertNotNull(mediaLocation);

		// now make PUT request to update media resource
		PutMethod method = new PutMethod(mediaLocation);
		String media = "This is a test - updated.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect no Location header 
		Header locationHeader = method.getResponseHeader("Location");
		assertNull(locationHeader);
		
		// expect Content-Type header for media-link
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));
		
		// expect Content-Location header
		Header contentLocationHeader = method.getResponseHeader("Content-Location");
		assertNotNull(contentLocationHeader);
		
		method.releaseConnection();

	}

	
	
	
	public void testDeleteMedia() throws Exception {
		
		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1);
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2);
		assertEquals(200, resultGet2);
		
		// now try delete on media location
		DeleteMethod delete = new DeleteMethod(mediaLocation);
		int result = executeMethod(delete);
		assertEquals(204, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3);
		assertEquals(404, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4);
		assertEquals(404, resultGet4);

		get4.releaseConnection();
		
	}

	
	
	
	public void testDeleteMediaLinkEntry() throws Exception {
		
		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1);
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2);
		assertEquals(200, resultGet2);
		
		// now try delete on media link location
		DeleteMethod delete = new DeleteMethod(mediaLinkLocation);
		int result = executeMethod(delete);
		assertEquals(204, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3);
		assertEquals(404, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4);
		assertEquals(404, resultGet4);
		
		get4.releaseConnection();

	}

	
	
	
	private static void verifyPostMediaResponse(int result, PostMethod method) {
		
		// expect the status code is 201 Created
		assertEquals(201, result);

		// expect the Location header is set with an absolute URI
		String responseLocation = method.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		assertTrue(responseLocation.startsWith("http://")); 
		// N.B. we shouldn't assume any more than this, because entry could have
		// a location anywhere
		
		// expect Content-Type header 
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));

		// expect Content-Location header 
		String responseContentLocation = method.getResponseHeader("Content-Location").getValue();
		assertNotNull(responseContentLocation);

	}




}



