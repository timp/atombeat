package org.atombeat;

import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestExtendedAtomProtocol_Collections extends TestCase {


	
	
	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	
	
	
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
    // expect the status code is 201 CREATED
		executeMethod(method, 201);

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
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method, 200);
		
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
    // expect the status code is 201 No Content
		executeMethod(method, 201);
		
		// expect the Location header is set with an absolute URI
		String responseLocation = method.getResponseHeader("Location").getValue();
		assertNotNull(responseLocation);
		assertEquals(collectionUri, responseLocation);
		
		// expect no Content-Type header 
		Header contentTypeHeader = method.getResponseHeader("Content-Type");
		assertNotNull(contentTypeHeader);
		assertTrue(contentTypeHeader.getValue().startsWith("application/atom+xml"));

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
		executeMethod(put1, 201);

		createTestEntryAndReturnLocation(col1, USER, PASS);

		String col2 = col1 + "/" + Double.toString(Math.random());
		PutMethod put2 = new PutMethod(col2);
		String content2 = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Sub-Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put2, content2);
		executeMethod(put2, 201);

		createTestEntryAndReturnLocation(col2, USER, PASS);

		GetMethod get3 = new GetMethod(col1);
		executeMethod(get3, 200);
		Document d3 = getResponseBodyAsDocument(get3);
		List<Element> entries3 = getChildrenByTagNameNS(d3, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, entries3.size()); // should be 2 because 1 is included via recursion into sub-collection
		
		GetMethod get4 = new GetMethod(col2);
		executeMethod(get4, 200);
		Document d4 = getResponseBodyAsDocument(get4);
		List<Element> entries4 = getChildrenByTagNameNS(d4, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries4.size());
		
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
		executeMethod(put1, 201);

		createTestEntryAndReturnLocation(col1, USER, PASS);

		String col2 = col1 + "/" + Double.toString(Math.random());
		PutMethod put2 = new PutMethod(col2);
		String content2 = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Sub-Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put2, content2);
		executeMethod(put2, 201);

		createTestEntryAndReturnLocation(col2, USER, PASS);

		GetMethod get3 = new GetMethod(col1);
		executeMethod(get3, 200);
		Document d3 = getResponseBodyAsDocument(get3);
		List<Element> entries3 = getChildrenByTagNameNS(d3, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries3.size()); // should be 1 because not recursive
		
		GetMethod get4 = new GetMethod(col2);
		executeMethod(get4, 200);
		Document d4 = getResponseBodyAsDocument(get4);
		List<Element> entries4 = getChildrenByTagNameNS(d4, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries4.size());
		
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
		executeMethod(put1, 201);

		createTestEntryAndReturnLocation(col1, USER, PASS);

		String col2 = col1 + "/" + Double.toString(Math.random());
		PutMethod put2 = new PutMethod(col2);
		String content2 = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Sub-Collection</atom:title>" +
			"</atom:feed>";
		setAtomRequestEntity(put2, content2);
		executeMethod(put2, 201);

		createTestEntryAndReturnLocation(col2, USER, PASS);

		GetMethod get3 = new GetMethod(col1);
		executeMethod(get3, 200);
		Document d3 = getResponseBodyAsDocument(get3);
		List<Element> entries3 = getChildrenByTagNameNS(d3, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries3.size()); // should be 1 because not recursive by default
		
		GetMethod get4 = new GetMethod(col2);
		executeMethod(get4, 200);
		Document d4 = getResponseBodyAsDocument(get4);
		List<Element> entries4 = getChildrenByTagNameNS(d4, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries4.size());
		
	}
	
	
	

	
	
}



