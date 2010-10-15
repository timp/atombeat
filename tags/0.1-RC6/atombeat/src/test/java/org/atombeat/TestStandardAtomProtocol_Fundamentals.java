package org.atombeat;

import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestStandardAtomProtocol_Fundamentals extends TestCase {


	
	
	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	private static final String TEST_COLLECTION_URI = CONTENT_URI + "test";
	
	
	
	private static Integer executeMethod(HttpMethod method) {
		
		return AtomTestUtils.executeMethod(method, USER, PASS);

	}

	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		String setupUrl = BASE_URI + "admin/setup-for-test.xql";
		
		PostMethod method = new PostMethod(setupUrl);
		
		int result = executeMethod(method);
		
		if (result != 200) {
			throw new RuntimeException("setup failed: "+result);
		}

	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	
	
	
	public void testPostEntry() {
		
		// create a new member by POSTing and atom entry document to the
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

	}



	
	public void testGetEntry() {

		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));

	}
	
	
	

	public void testPutEntry() {

		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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

	}
	
	
	

	public void testDeleteEntry() {
		
		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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

	}

	
	
	
	public void testGetFeed() {

		// try GET to collection URI
		GetMethod get1 = new GetMethod(TEST_COLLECTION_URI);
		int result1 = executeMethod(get1);
		
		// expect the status code is 200 OK
		assertEquals(200, result1);

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
		NodeList entries1 = d1.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(0, entries1.getLength());

		// add a member
		createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// try GET to collection URI
		GetMethod get2 = new GetMethod(TEST_COLLECTION_URI);
		int result2 = executeMethod(get2);
		
		// expect the status code is 200 OK
		assertEquals(200, result2);

		// check content
		Document d2 = getResponseBodyAsDocument(get2);
		NodeList entries2 = d2.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries2.getLength());

		// add a member
		createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// try GET to collection URI
		GetMethod get3 = new GetMethod(TEST_COLLECTION_URI);
		int result3 = executeMethod(get3);
		
		// expect the status code is 200 OK
		assertEquals(200, result3);

		// check content
		Document d3 = getResponseBodyAsDocument(get3);
		NodeList entries3 = d3.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, entries3.getLength());

	}
	
	
	
	
	public void testPostMedia_Text() {
		
		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method);
		
		verifyPostMediaResponse(result, method);
		
	}





	public void testPostMedia_Binary() {
		
		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(method, content, contentType);
		int result = executeMethod(method);
		
		verifyPostMediaResponse(result, method);

	}
	
	
	
	
	public void testGetMedia() {

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

	}
	
	
	

	public void testPutMedia_Text() {

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

	}

	
	
	
	public void testDeleteMedia() {
		
		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = AtomTestUtils.getEditLocation(mediaLinkDoc);
		
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

	}

	
	
	
	public void testDeleteMediaLinkEntry() {
		
		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = AtomTestUtils.getEditLocation(mediaLinkDoc);
		
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


