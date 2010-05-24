package org.atombeat;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestAtomProtocol extends TestCase {


	
	
	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	
	
	
	public static Integer executeMethod(HttpMethod method) {
		
		return AtomTestUtils.executeMethod(method, USER, PASS);

	}

	
	
	public TestAtomProtocol() {

		// need to run install once to ensure default global acl is stored
		
		String installUrl = BASE_URI + "admin/install-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method);
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}

	}
	

	
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
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());

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
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random()) + "/" + Double.toString(Math.random());

		doTestPutFeedToCreateCollection(collectionUri);

	}
	
	
	
	
	private static void doTestPutFeedToCreateCollection(String collectionUri) {
		
		PutMethod method = new PutMethod(collectionUri);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);

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
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		
		// now try to update feed metadata via a PUT request
		PutMethod method = new PutMethod(collectionUri);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection - Updated</atom:title></atom:feed>";
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
		String collectionUri = CONTENT_URI + Double.toString(Math.random());

		// setup a new POST request
		PostMethod method = new PostMethod(collectionUri);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);
		
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
	
	
	
	public void testPutAtomEntryToCollectionUriIsBadRequest() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);
		
		// expect the status code is 400 bad request
		assertEquals(400, result);
	}
	
	
	
	/**
	 * Test the standard atom protocol operation to create a new member of a
	 * collection via a POST request with an atom entry document as the request
	 * entity.
	 */
	public void testPostEntryToCreateMember() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
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



	
	/**
	 * Test the standard atom operation to update a member of a collection by
	 * a PUT request with an atom entry document as the request entity.
	 */
	public void testPutEntryToUpdateMember() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

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
	
	
	
	/**
	 * Test the standard atom operation to update a member of a collection by
	 * a PUT request with an atom entry document as the request entity.
	 */
	public void testPutEntryToUpdateMemberTwice() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

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
		int result2 = executeMethod(method2);

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
	
	
	
	/**
	 * Test the standard atom operation to update a member of a collection by
	 * a PUT request with an atom entry document as the request entity.
	 */
	public void testPutAndGetEntry() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

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

		// now get
		GetMethod get = new GetMethod(location);
		int getResult = executeMethod(get);
		assertEquals(200, getResult);
		Document doc = AtomTestUtils.getResponseBodyAsDocument(get);
		Element title = (Element) doc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated", title.getTextContent());
		
		// now put an updated entry document using a PUT request
		PutMethod method2 = new PutMethod(location);
		String content2 = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated Again</atom:title>" +
				"<atom:summary>This is a summary, updated again.</atom:summary>" +
			"</atom:entry>";
		
		setAtomRequestEntity(method2, content2);
		int result2 = executeMethod(method2);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result2);

		// now get again
		GetMethod get2 = new GetMethod(location);
		int getResult2 = executeMethod(get2);
		assertEquals(200, getResult2);
		Document doc2 = AtomTestUtils.getResponseBodyAsDocument(get2);
		Element title2 = (Element) doc2.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated Again", title2.getTextContent());

	}
	
	
	
	public void testPostTextDocumentToCreateMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method);
		
		verifyPostMediaResponse(result, method);
		
	}




	public void testPostMediaWithSpaceInSlug() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		method.setRequestHeader("Slug", "foo bar");
		executeMethod(method);
		
		Document mediaLinkDoc = getResponseBodyAsDocument(method);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		
		GetMethod get = new GetMethod(mediaLocation);
		executeMethod(get);
		
		String contentDisposition = get.getResponseHeader("Content-Disposition").getValue();
		assertNotNull(contentDisposition);
		
		assertEquals("attachment; filename=\"foo bar\"", contentDisposition);
		
	}




	public void testPostSpreadsheetToCreateMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(collectionUri);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(method, content, contentType);
		int result = executeMethod(method);
		
		verifyPostMediaResponse(result, method);

	}
	
	
	
	public void testChangeMediaTypeOfMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// create a new media resource by POSTing media to the collection URI
		PostMethod post = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(post, media);
		executeMethod(post); 
		
		Document mediaLinkDoc = getResponseBodyAsDocument(post);
		String location = getEditLocation(mediaLinkDoc);
		
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		
		Element editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		String type = editMediaLink.getAttribute("type");
		assertEquals("text/plain", type);
		
		Element contentElement = getContent(mediaLinkDoc);
		assertEquals(type, contentElement.getAttribute("type"));
		
		// check get on media resource has correct content type
		GetMethod getMedia = new GetMethod(mediaLocation);
		executeMethod(getMedia);
		assertTrue(getMedia.getResponseHeader("Content-Type").getValue().startsWith("text/plain"));
		
		// update the media resource with a different media type
		PutMethod put = new PutMethod(mediaLocation);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(put, content, contentType);
		int putResult = executeMethod(put);
		
		// check ok
		assertEquals(200, putResult);
		
		// retrieve media link entry to check type is updated
		GetMethod get = new GetMethod(location);
		executeMethod(get);
		
		mediaLinkDoc = getResponseBodyAsDocument(get);
		
		editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		type = editMediaLink.getAttribute("type");
		assertEquals("application/vnd.ms-excel", type);

		contentElement = getContent(mediaLinkDoc);
		assertEquals(type, contentElement.getAttribute("type"));
		
		// check get on media resource has correct content type
		GetMethod getMedia2 = new GetMethod(mediaLocation);
		executeMethod(getMedia2);
		assertTrue(getMedia2.getResponseHeader("Content-Type").getValue().startsWith("application/vnd.ms-excel"));
		
	}
	
	
	
	public void testPutMediaContentToAtomEntry() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// put media
		PutMethod put = new PutMethod(location);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(put, content, contentType);
		int putResult = executeMethod(put);
		
		// check result
		assertEquals(415, putResult);

	}
	
	
	
	public void testPutAtomEntryToMediaResource() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		
		// now try PUT atom entry to media location
		PutMethod method = new PutMethod(mediaLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);

		assertEquals(415, result);

	}
	
	
	
	public void testPostMediaResourceWithUnexpectedMediaType() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(collectionUri);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "foo/bar";
		setInputStreamRequestEntity(method, content, contentType);
		int result = executeMethod(method);
		
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

		// expect Content-Location header 
		String responseContentLocation = method.getResponseHeader("Content-Location").getValue();
		assertNotNull(responseContentLocation);

	}




	public void testPutTextDocumentToUpdateMediaResource() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
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
	
	
	
	public void testGetFeed() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now try GET to collection URI
		GetMethod method = new GetMethod(collectionUri);
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
	
	
	
	
	public void testGetFeedWithEntries() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod post = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(post, content);
		int postResult = executeMethod(post);
		
		// expect the status code is 201 Created
		assertEquals(201, postResult);
		
		// now try GET to collection URI
		GetMethod get = new GetMethod(collectionUri);
		int result = executeMethod(get);
		
		// expect the status code is 200 OK
		assertEquals(200, result);


		// check content
		Document d = getResponseBodyAsDocument(get);
		NodeList entries = d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries.getLength());

	}
	
	
	
	
	public void testFeedUpdatedDateIsModifiedAfterPostOrPutEntry() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now try GET to collection URI
		GetMethod get1 = new GetMethod(collectionUri);
		int get1Result = executeMethod(get1);
		assertEquals(200, get1Result);

		// check content
		Document d = getResponseBodyAsDocument(get1);
		Element updatedElement = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		String updated1 = updatedElement.getTextContent();
		
		// now post an entry
		PostMethod post = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(post, content);
		int postResult = executeMethod(post);
		String location = post.getResponseHeader("Location").getValue();
		assertEquals(201, postResult);
		
		// now try GET to collection URI again
		GetMethod get2 = new GetMethod(collectionUri);
		int get2Result = executeMethod(get2);
		assertEquals(200, get2Result);

		// check content
		d = getResponseBodyAsDocument(get2);
		updatedElement = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		String updated2 = updatedElement.getTextContent();
		
		assertFalse(updated1.equals(updated2));
		
		// now put an updated entry document using a PUT request
		PutMethod put = new PutMethod(location);
		content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(put, content);
		int putResult = executeMethod(put);
		assertEquals(200, putResult);

		// now try GET to collection URI again
		GetMethod get3 = new GetMethod(collectionUri);
		int get3Result = executeMethod(get3);
		assertEquals(200, get3Result);

		// check content
		d = getResponseBodyAsDocument(get3);
		updatedElement = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		String updated3 = updatedElement.getTextContent();
		
		assertFalse(updated1.equals(updated3));
		assertFalse(updated2.equals(updated3));

	}
	
	
	
	
	public void testGetEntry() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

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
	
	
	
	public void testGetMedia() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
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
	
	
	
	public void testMediaLinkEntryHasLengthAttributeOnEditMediaLink() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		assertNotNull(mediaLinkDoc);

		Element editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		assertTrue(editMediaLink.hasAttribute("length"));
		
	}
	
	
	
	public void testPutMediaResourceCausesUpdatesToMediaLink() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		assertNotNull(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		
		// store updated date for comparison
		String updatedBefore = getUpdated(mediaLinkDoc);

		// store length before for comparison later
		Element editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		assertTrue(editMediaLink.hasAttribute("length"));
		String lengthBefore = editMediaLink.getAttribute("length");

		// now make PUT request to update media resource
		PutMethod put = new PutMethod(mediaLocation);
		String media = "This is a test - updated.";
		setTextPlainRequestEntity(put, media);
		int putResult = executeMethod(put);
		assertEquals(200, putResult);
		
		// now retrieve media link entry
		GetMethod get = new GetMethod(mediaLinkLocation);
		int getResult = executeMethod(get);
		assertEquals(200, getResult);
		mediaLinkDoc = getResponseBodyAsDocument(get);

		// compared updated
		String updatedAfter = getUpdated(mediaLinkDoc);
		assertFalse(updatedBefore.equals(updatedAfter));
		
		// compare length after with length before
		editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		assertTrue(editMediaLink.hasAttribute("length"));
		String lengthAfter = editMediaLink.getAttribute("length");
		assertFalse(lengthBefore.equals(lengthAfter));
		
	}
	
	
	
	
	public void testMultipartRequestWithFileAcceptAtom() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = createMultipartRequest(collectionUri);
		post.setRequestHeader("Accept", "application/atom+xml");
		int result = executeMethod(post);
		
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
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = createMultipartRequest(collectionUri);
		int result = executeMethod(post);
		
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
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);
		assertEquals(201, result);

		String location = method.getResponseHeader("Location").getValue();
		
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
	
	
	
	public void testDeleteMediaResource() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
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
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
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

	
	
	public void testGetEntryResponseHasEtagHeader() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now try GET to member URI
		GetMethod method = new GetMethod(location);
		int result = executeMethod(method);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect etag header
		assertNotNull(method.getResponseHeader("ETag"));
		
	}
	
	
	
	public void testPostEntryResponseHasEtagHeader() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method);
		
		// expect the status code is 201 Created
		assertEquals(201, result);
		assertNotNull(method.getResponseHeader("ETag"));
		
	}
	
	
	
	public void testPutEntryResponseHasEtagHeader() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

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
		assertNotNull(method.getResponseHeader("ETag"));
		
	}
	
	
	
	public void testEtagsChangeAfterUpdate() {
		 
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now try GET to member URI
		GetMethod get1 = new GetMethod(location);
		int get1Result = executeMethod(get1);
		
		// expect the status code is 200 OK
		assertEquals(200, get1Result);

		// expect etag header
		assertNotNull(get1.getResponseHeader("ETag"));
		
		// store etag header for later comparison
		String etag1 = get1.getResponseHeader("ETag").getValue();
		
		// now put an updated entry document using a PUT request
		PutMethod put = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(put, content);
		int putResult = executeMethod(put);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, putResult);

		// now try GET to member URI again
		GetMethod get2 = new GetMethod(location);
		int get2Result = executeMethod(get2);
		
		// expect the status code is 200 OK
		assertEquals(200, get2Result);

		// expect etag header
		assertNotNull(get2.getResponseHeader("ETag"));
		
		// store etag header for later comparison
		String etag2 = get2.getResponseHeader("ETag").getValue();
		
		assertFalse(etag1.equals(etag2));

	}
	
	
	
	public void testConditionalGet() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now try GET to member URI
		GetMethod get1 = new GetMethod(location);
		int get1Result = executeMethod(get1);
		
		// expect the status code is 200 OK
		assertEquals(200, get1Result);

		// expect etag header
		assertNotNull(get1.getResponseHeader("ETag"));
		
		// store etag header for use in conditional get
		String etag = get1.getResponseHeader("ETag").getValue();
		
		// now try conditional GET to member URI 
		GetMethod get2 = new GetMethod(location);
		get2.setRequestHeader("If-None-Match" , etag);
		int get2Result = executeMethod(get2);
		
		// expect the status code is 304 Not Modified
		assertEquals(304, get2Result);

		// now try conditional GET to member URI with different etag
		GetMethod get3 = new GetMethod(location);
		get3.setRequestHeader("If-None-Match" , "\"foo\"");
		int get3Result = executeMethod(get3);
		
		// expect the status code is 200
		assertEquals(200, get3Result);

	}
	
	
	
	public void testConditionalPut() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		String location = createTestEntryAndReturnLocation(collectionUri, USER, PASS);

		// now try GET to member URI
		GetMethod get1 = new GetMethod(location);
		int get1Result = executeMethod(get1);
		
		// expect the status code is 200 OK
		assertEquals(200, get1Result);

		// expect etag header
		assertNotNull(get1.getResponseHeader("ETag"));
		
		// store etag header for later comparison
		String etag = get1.getResponseHeader("ETag").getValue();
		
		// now put an updated entry document using a PUT request
		PutMethod put1 = new PutMethod(location);
		put1.setRequestHeader("If-Match", "foo");
		
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(put1, content);
		int put1Result = executeMethod(put1);

		// expect the status code is 412 Precondition Failed 
		assertEquals(412, put1Result);
		
		// now put an updated entry document using a PUT request
		PutMethod put2 = new PutMethod(location);
		put2.setRequestHeader("If-Match", etag);
		setAtomRequestEntity(put2, content);
		int put2Result = executeMethod(put2);

		// expect the status code is 200 OK 
		assertEquals(200, put2Result);
		
	}
	
	
	
	public void testRecursiveCollection() {

		String col1 = CONTENT_URI + Double.toString(Math.random());
		PutMethod put1 = new PutMethod(col1);
		String content1 = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:recursive=\"true\">" +
				"<atom:title>Test Collection (Recursive)</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put1, content1);
		int result1 = executeMethod(put1);
		assertEquals(201, result1);

		createTestEntryAndReturnLocation(col1, USER, PASS);

		String col2 = col1 + "/" + Double.toString(Math.random());
		PutMethod put2 = new PutMethod(col2);
		String content2 = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Sub-Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put2, content2);
		int result2 = executeMethod(put2);
		assertEquals(201, result2);

		createTestEntryAndReturnLocation(col2, USER, PASS);

		GetMethod get3 = new GetMethod(col1);
		int result3 = executeMethod(get3);
		assertEquals(200, result3);
		Document d3 = getResponseBodyAsDocument(get3);
		NodeList entries3 = d3.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, entries3.getLength()); // should be 2 because 1 is included via recursion into sub-collection
		
		GetMethod get4 = new GetMethod(col2);
		int result4 = executeMethod(get4);
		assertEquals(200, result4);
		Document d4 = getResponseBodyAsDocument(get4);
		NodeList entries4 = d4.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries4.getLength());
		
	}
	



	public void testExplicitlyNotRecursiveCollection() {

		String col1 = CONTENT_URI + Double.toString(Math.random());
		PutMethod put1 = new PutMethod(col1);
		String content1 = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:recursive=\"false\">" +
				"<atom:title>Test Collection (Recursive)</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put1, content1);
		int result1 = executeMethod(put1);
		assertEquals(201, result1);

		createTestEntryAndReturnLocation(col1, USER, PASS);

		String col2 = col1 + "/" + Double.toString(Math.random());
		PutMethod put2 = new PutMethod(col2);
		String content2 = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Sub-Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put2, content2);
		int result2 = executeMethod(put2);
		assertEquals(201, result2);

		createTestEntryAndReturnLocation(col2, USER, PASS);

		GetMethod get3 = new GetMethod(col1);
		int result3 = executeMethod(get3);
		assertEquals(200, result3);
		Document d3 = getResponseBodyAsDocument(get3);
		NodeList entries3 = d3.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries3.getLength()); // should be 1 because not recursive
		
		GetMethod get4 = new GetMethod(col2);
		int result4 = executeMethod(get4);
		assertEquals(200, result4);
		Document d4 = getResponseBodyAsDocument(get4);
		NodeList entries4 = d4.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries4.getLength());
		
	}
	



	public void testImplicitlyNotRecursiveCollection() {

		String col1 = CONTENT_URI + Double.toString(Math.random());
		PutMethod put1 = new PutMethod(col1);
		String content1 = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\">" +
				"<atom:title>Test Collection (Recursive)</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put1, content1);
		int result1 = executeMethod(put1);
		assertEquals(201, result1);

		createTestEntryAndReturnLocation(col1, USER, PASS);

		String col2 = col1 + "/" + Double.toString(Math.random());
		PutMethod put2 = new PutMethod(col2);
		String content2 = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Sub-Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put2, content2);
		int result2 = executeMethod(put2);
		assertEquals(201, result2);

		createTestEntryAndReturnLocation(col2, USER, PASS);

		GetMethod get3 = new GetMethod(col1);
		int result3 = executeMethod(get3);
		assertEquals(200, result3);
		Document d3 = getResponseBodyAsDocument(get3);
		NodeList entries3 = d3.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries3.getLength()); // should be 1 because not recursive by default
		
		GetMethod get4 = new GetMethod(col2);
		int result4 = executeMethod(get4);
		assertEquals(200, result4);
		Document d4 = getResponseBodyAsDocument(get4);
		NodeList entries4 = d4.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries4.getLength());
		
	}
	
}



