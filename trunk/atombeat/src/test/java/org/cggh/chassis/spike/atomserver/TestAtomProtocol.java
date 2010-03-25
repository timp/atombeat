package org.cggh.chassis.spike.atomserver;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.cggh.chassis.spike.atomserver.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestAtomProtocol extends TestCase {


	
	
	
	private static final String SERVER_URI = "http://localhost:8081/atomserver/atomserver/content/";
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";

	
	

	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	
	
	
	/**
	 * Test creation of a new atom collection by a PUT request with an atom feed
	 * document as the request body. N.B. this operation is not part of the
	 * standard atom protocol, but is an extension to support bootstrapping
	 * of an atom workspace, e.g., by an administrator.
	 */
	public void testPutFeedToCreateCollection() {
		
		// we want to test that we can create a new collection by PUT of atom 
		// feed doc however, we may run this test several times without cleaning
		// the database so we have to generate a random collection URI
		
		String collectionUri = SERVER_URI + Double.toString(Math.random());

		doTestPutFeedToCreateCollection(collectionUri);
		
	}
	
	
	
	/**
	 * Test creation of a new atom collection by a PUT request with an atom feed
	 * document as the request body, where the atom collection is nested within
	 * another collection, e.g. "/foo/bar". 
	 */
	public void testPutFeedToCreateNestedCollection() {
		
		// we want to test that we can create a new collection by PUT of atom 
		// feed doc however, we may run this test several times without cleaning
		// the database so we have to generate a random collection URI
		
		String collectionUri = SERVER_URI + Double.toString(Math.random()) + "/" + Double.toString(Math.random());

		doTestPutFeedToCreateCollection(collectionUri);

	}
	
	
	
	
	private static void doTestPutFeedToCreateCollection(String collectionUri) {
		
		PutMethod method = new PutMethod(collectionUri);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);

		// expect the status code is 201 CREATED
		assertEquals(201, result);

		// expect the Location header is set with an absolute URI
		String responseLocation = method.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		assertEquals(collectionUri, responseLocation);
		
		// expect the Content-Type header starts with the Atom media type
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));

	}
	
	
	

	/**
	 * Test the use of a PUT request to update the feed metadata for an already
	 * existing atom collection.
	 */
	public void testPutFeedToCreateAndUpdateCollection() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		
		// now try to update feed metadata via a PUT request
		PutMethod method = new PutMethod(collectionUri);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection - Updated</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);
		
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
	
	
	
	
	/**
	 * The eXist atom servlet allows the use of a POST request with a feed 
	 * document as the request entity, and responds with a 204 No Content if
	 * the request was successful. We think it is more appropriate to use a PUT
	 * request with a feed document to create a new atom collection, with a 201
	 * Created response indicating the result. However we will implement this 
	 * operation as eXist does, except for using a response code of 201, for 
	 * compatibility with clients using the eXist atom servlet. Supporting this 
	 * operation at all, and if so, the appropriate response code, may be 
	 * reviewed in the near future.
	 */
	public void testPostFeedToCreateCollection() {
		
		// setup test
		String collectionUri = SERVER_URI + Double.toString(Math.random());

		// setup a new POST request
		PostMethod method = new PostMethod(collectionUri);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);
		
		// expect the status code is 201 No Content
		assertEquals(201, result);

		// expect the Location header is set with an absolute URI
		String responseLocation = method.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		assertEquals(collectionUri, responseLocation);
		
		// expect no Content-Type header 
		Header contentTypeHeader = method.getResponseHeader("Content-Type");
		assertNotNull(contentTypeHeader);
		assertTrue(contentTypeHeader.getValue().startsWith("application/atom+xml"));

	}
	
	
	
	
	/**
	 * Test the standard atom protocol operation to create a new member of a
	 * collection via a POST request with an atom entry document as the request
	 * entity.
	 */
	public void testPostEntryToCreateMember() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);
		
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
		
	}



	
	/**
	 * Test the standard atom operation to update a member of a collection by
	 * a PUT request with an atom entry document as the request entity.
	 */
	public void testPutEntryToUpdateMember() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now put an updated entry document using a PUT request
		PutMethod method = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);

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
	
	
	
	/**
	 * Test the standard atom operation to update a member of a collection by
	 * a PUT request with an atom entry document as the request entity.
	 */
	public void testPutEntryToUpdateMemberTwice() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now put an updated entry document using a PUT request
		PutMethod method = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result);

		// expect no Location header 
		Header responseLocationHeader = method.getResponseHeader("Location");
		assertNull(responseLocationHeader);
		
		// expect the Content-Type header starts with the Atom media type
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));

		Document d1 = AtomTestUtils.getResponseBodyAsDocument(method);
		Element title = (Element) d1.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated", title.getTextContent());
		
		// now put an updated entry document using a PUT request
		PutMethod method2 = new PutMethod(location);
		String content2 = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated Again</atom:title>" +
				"<atom:summary>This is a summary, updated again.</atom:summary>" +
			"</atom:entry>";
		
		setAtomRequestEntity(method2, content2);
		int result2 = executeMethod(method2, USER, PASS);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result2);

		// expect no Location header 
		Header responseLocationHeader2 = method2.getResponseHeader("Location");
		assertNull(responseLocationHeader2);
		
		// expect the Content-Type header starts with the Atom media type
		String responseContentType2 = method2.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType2);
		assertTrue(responseContentType2.trim().startsWith("application/atom+xml"));

		Document d2 = AtomTestUtils.getResponseBodyAsDocument(method2);
		Element title2 = (Element) d2.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated Again", title2.getTextContent());
		
	}
	
	
	
	public void testPostTextDocumentToCreateMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, USER, PASS);
		
		verifyPostMediaResponse(result, method);
		
	}




	public void testPostSpreadsheetToCreateMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(collectionUri);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(method, content, contentType);
		int result = executeMethod(method, USER, PASS);
		
		verifyPostMediaResponse(result, method);

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

	}




	public void testPutTextDocumentToUpdateMediaResource() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		assertNotNull(mediaLinkDoc);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		assertNotNull(mediaLocation);

		// now make PUT request to update media resource
		PutMethod method = new PutMethod(mediaLocation);
		String media = "This is a test - updated.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, USER, PASS);
		
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
	
	
	
	public void testGetFeed() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now try GET to collection URI
		GetMethod method = new GetMethod(collectionUri);
		int result = executeMethod(method, USER, PASS);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect no Location header 
		Header locationHeader = method.getResponseHeader("Location");
		assertNull(locationHeader);
		
		// expect Content-Type header 
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));
		
		// check content
		Document d = getResponseBodyAsDocument(method);
		Element id = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "id").item(0);
		assertNotNull(id);
		Element title = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertNotNull(title);
		Element updated = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		assertNotNull(updated);
		String editLocation = AtomTestUtils.getLinkHref(d, "edit");
		assertNotNull(editLocation);
		String selfLocation = AtomTestUtils.getLinkHref(d, "self");
		assertNotNull(selfLocation);

	}
	
	
	
	
	public void testGetEntry() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now try GET to member URI
		GetMethod method = new GetMethod(location);
		int result = executeMethod(method, USER, PASS);
		
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
	
	
	
	public void testGetMedia() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		assertNotNull(mediaLinkDoc);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		assertNotNull(mediaLocation);
		
		// now try get on media location
		GetMethod method = new GetMethod(mediaLocation);
		int result = executeMethod(method, USER, PASS);
		
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
	
	
	
	
	public void testMultipartRequestWithFileAcceptAtom() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = createMultipartRequest(collectionUri);
		post.setRequestHeader("Accept", "application/atom+xml");
		int result = executeMethod(post, USER, PASS);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

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
		
	}



	public void testMultipartRequestWithFileDefault() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = createMultipartRequest(collectionUri);
		int result = executeMethod(post, USER, PASS);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect the Location header is set with an absolute URI
		String responseLocation = post.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		
		assertTrue(responseLocation.startsWith("http://")); 
		// N.B. we shouldn't assume any more than this, because entry could have
		// a location anywhere
		
		// expect Content-Type header 
		
		// without an Accept request header, we expect the response to default
		// to text/html content, to be compatible with use from GWT clients
		// and other browser environments where other content types cause
		// inconsistent behaviour across browsers
		String responseContentType = post.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("text/html"));
		
	}
	
	
	
	private PostMethod createMultipartRequest(String collectionUri) {
		
		PostMethod post = new PostMethod(collectionUri);
		File file = new File(this.getClass().getClassLoader().getResource("spreadsheet1.xls").getFile());
		FilePart fp = createFilePart(file, "spreadsheet1.xls", "application/vnd.ms-excel", "media");
		StringPart sp = new StringPart("summary", "this is a great spreadsheet");
		Part[] parts = { fp , sp };
		setMultipartRequestEntity(post, parts);
		return post;
		
	}

	
	
	public void testDeleteEntry() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, USER, PASS);
		assertEquals(201, result);

		String location = method.getResponseHeader("Location").getValue();
		
		// check we can GET the entry
		GetMethod get1 = new GetMethod(location);
		int get1Result = executeMethod(get1, USER, PASS);
		assertEquals(get1Result, 200);
		
		// now try DELETE the entry
		DeleteMethod delete = new DeleteMethod(location);
		int deleteResult = executeMethod(delete, USER, PASS);
		assertEquals(204, deleteResult);
		
		// now try to GET the entry
		GetMethod get2 = new GetMethod(location);
		int get2Result = executeMethod(get2, USER, PASS);
		assertEquals(404, get2Result);

	}
	
	
	
	public void testDeleteMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = AtomTestUtils.getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1, USER, PASS);
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2, USER, PASS);
		assertEquals(200, resultGet2);
		
		// now try delete on media location
		DeleteMethod delete = new DeleteMethod(mediaLocation);
		int result = executeMethod(delete, USER, PASS);
		assertEquals(204, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3, USER, PASS);
		assertEquals(404, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4, USER, PASS);
		assertEquals(404, resultGet4);

	}


	
	public void testDeleteMediaLinkEntry() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = AtomTestUtils.getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1, USER, PASS);
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2, USER, PASS);
		assertEquals(200, resultGet2);
		
		// now try delete on media link location
		DeleteMethod delete = new DeleteMethod(mediaLinkLocation);
		int result = executeMethod(delete, USER, PASS);
		assertEquals(204, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3, USER, PASS);
		assertEquals(404, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4, USER, PASS);
		assertEquals(404, resultGet4);

	}


	
}



