package org.atombeat;

import java.io.IOException;
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
	
	
	
	private static void executeMethod(HttpMethod method, int expectedStatus) {
		
		AtomTestUtils.executeMethod(method, USER, PASS, expectedStatus);

	}

	
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		String setupUrl = BASE_URI + "admin/setup-for-test.xql";
		
		executeMethod(new PostMethod(setupUrl), 200);
		
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	
	
	
	
	
	public void testPutAtomEntryToCollectionUriIsClientError() {
		
		PutMethod method = new PutMethod(TEST_COLLECTION_URI);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
    // expect the status code is 400 bad request
		executeMethod(method, 400);
		
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
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method, 200);

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
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method2, 200);


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
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method, 200);

		// now get
		GetMethod get = new GetMethod(location);
		executeMethod(get, 200);
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
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method2, 200);

		// now get again
		GetMethod get2 = new GetMethod(location);
		executeMethod(get2, 200);
		Document doc2 = AtomTestUtils.getResponseBodyAsDocument(get2);
		Element title2 = (Element) doc2.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated Again", title2.getTextContent());

	}
	
	
	

	public void testPostMediaWithSpaceInSlug() {
		
		// create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		method.setRequestHeader("Slug", "foo bar.txt");
		executeMethod(method, 201);
		
		Document mediaLinkDoc = getResponseBodyAsDocument(method);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		
		GetMethod get = new GetMethod(mediaLocation);
		executeMethod(get, 200);
		
		String contentDisposition = get.getResponseHeader("Content-Disposition").getValue();
		assertNotNull(contentDisposition);
		
		assertEquals("attachment; filename=\"foo bar.txt\"", contentDisposition);
		
	}




	
	public void testChangeMediaTypeOfMediaResource() {
		
		// create a new media resource by POSTing media to the collection URI
		PostMethod post = new PostMethod(TEST_COLLECTION_URI);
		String media = "This is a test.";
		setTextPlainRequestEntity(post, media);
		executeMethod(post, 201); 
		
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
		executeMethod(getMedia, 200);
		assertTrue(getMedia.getResponseHeader("Content-Type").getValue().startsWith("text/plain"));
		
		// update the media resource with a different media type
		PutMethod put = new PutMethod(mediaLocation);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(put, content, contentType);
		executeMethod(put, 200);
		
		// retrieve media link entry to check type is updated
		GetMethod get = new GetMethod(location);
		executeMethod(get, 200);
		
		mediaLinkDoc = getResponseBodyAsDocument(get);
		
		editMediaLink = getLinks(mediaLinkDoc, "edit-media").get(0);
		type = editMediaLink.getAttribute("type");
		assertEquals("application/vnd.ms-excel", type);

		contentElement = getContent(mediaLinkDoc);
		assertEquals(type, contentElement.getAttribute("type"));
		
		// check get on media resource has correct content type
		GetMethod getMedia2 = new GetMethod(mediaLocation);
		executeMethod(getMedia2, 200);
		assertTrue(getMedia2.getResponseHeader("Content-Type").getValue().startsWith("application/vnd.ms-excel"));
		
	}
	
	
	
	public void testPutMediaContentToMemberUriIsClientError() {

		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// put media
		PutMethod put = new PutMethod(location);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(put, content, contentType);
		executeMethod(put, 415);
		
	}

	
	
	public void testPutAtomFeedToMemberUriIsClientError() {
		
		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		PutMethod method = new PutMethod(location);
		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection - Updated</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		executeMethod(method, 400);

	}
	
	
	public void testPutMediaContentToCollectionUriIsClientError() {

		// put media
		PutMethod put = new PutMethod(TEST_COLLECTION_URI);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(put, content, contentType);
		executeMethod(put, 415);
		
	}
	
	
	
	public void testPutAtomEntryToMediaResourceUriIsClientError() {

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
		executeMethod(method, 415);

	}
	
	
	
	public void testPutAtomFeedToMediaResourceUriIsClientError() {

		// setup test
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(TEST_COLLECTION_URI, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		
		// now try PUT atom entry to media location
		PutMethod method = new PutMethod(mediaLocation);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
		executeMethod(method, 415);

	}
	
	
	
	public void testPostMediaResourceWithUnexpectedMediaType() {
		
		// now create a new media resource by POSTing media to the collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "foo/bar";
		setInputStreamRequestEntity(method, content, contentType);
    // expect the status code is 201 Created
		executeMethod(method, 201);
		
		verifyPostMediaResponse(method);

	}
	
	
	
	private static void verifyPostMediaResponse(PostMethod method) {
		

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
		executeMethod(get1, 200);

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
		executeMethod(post, 201);
		String location = post.getResponseHeader("Location").getValue();
		
		// now try GET to collection URI again
		GetMethod get2 = new GetMethod(TEST_COLLECTION_URI);
		executeMethod(get2, 200);

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
		executeMethod(put, 200);

		// now try GET to collection URI again
		GetMethod get3 = new GetMethod(TEST_COLLECTION_URI);
		executeMethod(get3, 200);

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
		executeMethod(put, 200);
		
		// now retrieve media link entry
		GetMethod get = new GetMethod(mediaLinkLocation);
		executeMethod(get, 200);
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
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(put1, 200);

		Document d1 = getResponseBodyAsDocument(put1);

		// now put an updated entry document using a PUT request
		PutMethod put2 = new PutMethod(location);
		InputStream content2 = this.getClass().getClassLoader().getResourceAsStream("entry1.xml");
		setInputStreamRequestEntity(put2, content2, contentType);
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(put2, 200);

		Document d2 = getResponseBodyAsDocument(put2);
		
		assertEquals( d1.getDocumentElement().getChildNodes().getLength(), d2.getDocumentElement().getChildNodes().getLength());
		
		// now put an updated entry document using a PUT request
		PutMethod put3 = new PutMethod(location);
		InputStream content3 = this.getClass().getClassLoader().getResourceAsStream("entry1.xml");
		setInputStreamRequestEntity(put3, content3, contentType);
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(put3, 200);

		Document d3 = getResponseBodyAsDocument(put3);
		
		assertEquals( d1.getDocumentElement().getChildNodes().getLength(), d3.getDocumentElement().getChildNodes().getLength());
		assertEquals( d2.getDocumentElement().getChildNodes().getLength(), d3.getDocumentElement().getChildNodes().getLength());
		
	}

	
	
	

	public void testPostNotWellFormedEntryIsClientError() {
		
		// create a new member by POSTing an atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(TEST_COLLECTION_URI);
		String content = 
			"<atom:entry>" + // not well-formed because forgot namespace declaration
				"<atom:title>Test Member - not well-formed because forgot namespace declaration</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		executeMethod(method, 400);
		
		// expect Content-Type header 
		String responseContentType = method.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/xml"));
		
	}

  private String getResponse(String content) throws IOException { 
      PostMethod postMethod = executePost(TEST_COLLECTION_URI, USER, PASS, content);
      Header locationHeader = postMethod.getResponseHeader("Location");
      GetMethod get = new GetMethod(locationHeader.getValue() );
      executeMethod(get, 200);
      
      return get.getResponseBodyAsString();
  }

  public void testRoundTrippingIsIdempotent() throws Exception { 
      // create test member
      String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" + 
                       "<atom:title>Test Entry</atom:title>" + 
                       "<atom:summary>this is a test</atom:summary>" + 
                       "</atom:entry>";

      String responseBody = getResponse(content);
      String responseBodyInvariant = makeInvariant(responseBody);
      
      String expectedResponse = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                                "    <atom:id></atom:id>\n" + 
                                "    <atom:published></atom:published>\n" + 
                                "    <atom:updated></atom:updated>\n" + 
                                "    <atom:author>\n" +
                                "        <atom:name>adam</atom:name>\n" + 
                                "    </atom:author>\n" + 
                                "    <atom:title>Test Entry</atom:title>\n" +
                                "    <atom:summary>this is a test</atom:summary>\n" + 
                                "    <atom:link rel=\"self\" type=\"application/atom+xml;type=entry\" href=\"\"/>\n" + 
                                "    <atom:link rel=\"edit\" type=\"application/atom+xml;type=entry\" href=\"\"/>\n" + 
                                "    <atom:link rel=\"http://purl.org/atombeat/rel/security-descriptor\" href=\"\" type=\"application/atom+xml;type=entry\"/>\n" + 
                                "</atom:entry>\n";
      assertEquals(expectedResponse, responseBodyInvariant);
      
      
      // save it back to data store
      
      String responseBody2 = getResponse(responseBody);
      String responseBodyInvariant2 = makeInvariant(responseBody2);
      
      // get it again, check that it is unchanged. 
      assertEquals(responseBodyInvariant, responseBodyInvariant2);
      
  }





/**
 * @param responseBody
 * @return
 */
private String makeInvariant(String responseBody) {
    String responseBodyInvariant = responseBody.replaceAll(":id>[^<]+</atom:id>", ":id></atom:id>");
      responseBodyInvariant = responseBodyInvariant.replaceAll(":updated>[^<]+</atom", ":updated></atom");
      responseBodyInvariant = responseBodyInvariant.replaceAll(":published>[^<]+</atom", ":published></atom");
      responseBodyInvariant = responseBodyInvariant.replaceAll("href=\"http[^\"]+\"", "href=\"\"");
    return responseBodyInvariant;
}
	
	
	
}



