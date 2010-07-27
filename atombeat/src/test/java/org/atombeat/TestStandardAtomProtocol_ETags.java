package org.atombeat;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestStandardAtomProtocol_ETags extends TestCase {


	
	
	
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
	
	
	
	

	public void testGetEntryResponseHasEtagHeader() {
		
		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

		// now try GET to member URI
		GetMethod method = new GetMethod(location);
		int result = executeMethod(method);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect etag header
		assertNotNull(method.getResponseHeader("ETag"));
		
	}
	
	
	
	public void testPostEntryResponseHasEtagHeader() {
		
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
		assertNotNull(method.getResponseHeader("ETag"));
		
	}
	
	
	
	public void testPutEntryResponseHasEtagHeader() {
		
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
		assertNotNull(method.getResponseHeader("ETag"));
		
	}
	
	
	
	public void testEtagsChangeAfterUpdate() {
		 
		// setup test
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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
		String location = createTestEntryAndReturnLocation(TEST_COLLECTION_URI, USER, PASS);

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
	
	
	
}



