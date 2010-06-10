package org.atombeat;

import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestStandardAtomProtocol_Details extends TestCase {


	
	
	
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
	
	
	
	
	
	
	public void testPutAtomEntryToCollectionUriIsBadRequest() {
		
		PutMethod method = new PutMethod(TEST_COLLECTION_URI);
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
	
	
	
	
	public void testPutEntryTwice() {

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
	
	
	
	public void testPutAndGetEntry() {

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
	
	
	

	public void testPostMediaWithSpaceInSlug() {
		
		// create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
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




	
	public void testChangeMediaTypeOfMediaResource() {
		
		// create a new media resource by POSTing media to the collection URI
		PostMethod post = new PostMethod(TEST_COLLECTION_URI);
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
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
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
		
		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
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




	
	public void testFeedUpdatedDateIsModifiedAfterPostOrPutEntry() {

		// now try GET to collection URI
		GetMethod get1 = new GetMethod(TEST_COLLECTION_URI);
		int get1Result = executeMethod(get1);
		assertEquals(200, get1Result);

		// check content
		Document d = getResponseBodyAsDocument(get1);
		Element updatedElement = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		String updated1 = updatedElement.getTextContent();
		
		// now post an entry
		PostMethod post = new PostMethod(TEST_COLLECTION_URI);
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
		GetMethod get2 = new GetMethod(TEST_COLLECTION_URI);
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
		GetMethod get3 = new GetMethod(TEST_COLLECTION_URI);
		int get3Result = executeMethod(get3);
		assertEquals(200, get3Result);

		// check content
		d = getResponseBodyAsDocument(get3);
		updatedElement = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		String updated3 = updatedElement.getTextContent();
		
		assertFalse(updated1.equals(updated3));
		assertFalse(updated2.equals(updated3));

	}
	
	
	
	
	public void testMediaLinkEntryHasLengthAttributeOnEditMediaLink() {

		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		assertNotNull(mediaLinkDoc);

		Element editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		assertTrue(editMediaLink.hasAttribute("length"));
		
	}
	
	
	
	public void testPutMediaResourceCausesUpdatesToMediaLink() {

		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
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
	
	
	
	
	
	public void testPutEntryCausesNoMangle() {
		
		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// now put an updated entry document using a PUT request
		PutMethod put1 = new PutMethod(location);
		InputStream content1 = this.getClass().getClassLoader().getResourceAsStream("entry1.xml");
		String contentType = "application/atom+xml";
		setInputStreamRequestEntity(put1, content1, contentType);
		int result1 = executeMethod(put1);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result1);
		
		Document d1 = getResponseBodyAsDocument(put1);

		// now put an updated entry document using a PUT request
		PutMethod put2 = new PutMethod(location);
		InputStream content2 = this.getClass().getClassLoader().getResourceAsStream("entry1.xml");
		setInputStreamRequestEntity(put2, content2, contentType);
		int result2 = executeMethod(put2);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result2);

		Document d2 = getResponseBodyAsDocument(put2);
		
		assertEquals( d1.getDocumentElement().getChildNodes().getLength(), d2.getDocumentElement().getChildNodes().getLength());
		
		// now put an updated entry document using a PUT request
		PutMethod put3 = new PutMethod(location);
		InputStream content3 = this.getClass().getClassLoader().getResourceAsStream("entry1.xml");
		setInputStreamRequestEntity(put3, content3, contentType);
		int result3 = executeMethod(put3);

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result3);

		Document d3 = getResponseBodyAsDocument(put3);
		
		assertEquals( d1.getDocumentElement().getChildNodes().getLength(), d3.getDocumentElement().getChildNodes().getLength());
		assertEquals( d2.getDocumentElement().getChildNodes().getLength(), d3.getDocumentElement().getChildNodes().getLength());
		
		
		

	}

	
	
	
	
	
}



