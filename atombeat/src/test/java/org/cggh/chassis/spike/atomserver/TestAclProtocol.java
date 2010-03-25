package org.cggh.chassis.spike.atomserver;



import static org.cggh.chassis.spike.atomserver.AtomTestUtils.*;

import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import junit.framework.TestCase;




public class TestAclProtocol extends TestCase {


	
	
	
	private static final String SERVER_URI = "http://localhost:8081/atomserver/atomserver/content/";
	private static final String ACL_URI = "http://localhost:8081/atomserver/atomserver/acl/";

	
	

	public TestAclProtocol() {
	
	
	}
	
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		// need to run install before each test to ensure default global acl is restored
		
		String installUrl = "http://localhost:8081/atomserver/atomserver/admin/install-example.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method, "adam", "test");
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}
		
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	

	public void testGetGlobalAcl() {
		
		GetMethod g = new GetMethod(ACL_URI);
		int r = executeMethod(g, "adam", "test");

		assertEquals(200, r);
		
		verifyAtomResponse(g);
		
		Document d = getResponseBodyAsDocument(g);
		verifyDocIsAtomEntryWithAclContent(d);
		
	}
	
	
	
	public void testGetGlobalAclDenied() {

		GetMethod g = new GetMethod(ACL_URI);
		int r = executeMethod(g, "rebecca", "test");

		assertEquals(403, r);

	}
	

	
	public void testGetCollectionAcl() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// make a second get request for the acl
		GetMethod h = new GetMethod(aclLocation);
		int s = executeMethod(h, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithAclContent(e);

	}
	
	
	
	public void testGetCollectionNoEditAclLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "rebecca", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNull(aclLocation);
		
	}
	
	
	
	public void testGetCollectionAclDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// make a second get request for the acl
		GetMethod h = new GetMethod(aclLocation);
		int s = executeMethod(h, "rebecca", "test");
		assertEquals(403, s);

	}
	
	
	
	public void testGetMemberAcl() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// make a get request for the acl
		GetMethod h = new GetMethod(aclLocation);
		int s = executeMethod(h, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithAclContent(e);

	}
	
	
	

	public void testGetMemberNoEditAclLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "rebecca", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNull(aclLocation);
		
	}
	
	
	
	public void testGetMemberAclDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// make a get request for the acl
		GetMethod h = new GetMethod(aclLocation);
		int s = executeMethod(h, "rebecca", "test");
		assertEquals(403, s);

	}

	
	
	public void testGetMediaAcl() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		// look for "edit-acl" link
		String aclLocation = getLinkHref(d, "edit-media-acl");
		assertNotNull(aclLocation);
		
		// make a get request for the acl
		GetMethod h = new GetMethod(aclLocation);
		int s = executeMethod(h, "audrey", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithAclContent(e);

	}
	
	
	

	public void testGetMediaAclDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		// look for "edit-acl" link
		String aclLocation = getLinkHref(d, "edit-media-acl");
		assertNotNull(aclLocation);
		
		// make a get request for the acl
		GetMethod h = new GetMethod(aclLocation);
		int s = executeMethod(h, "rebecca", "test");
		assertEquals(403, s);

	}
	
	
	

	public void testGetMediaAclNoEditMediaAclLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String location = getEditLocation(d);
		
		// retrieve media link as rebecca
		GetMethod g = new GetMethod(location);
		int s = executeMethod(g, "rebecca", "test");
		assertEquals(200, s);
		Document e = getResponseBodyAsDocument(g);
		String mediaAclLocation = getLinkHref(e, "edit-media-acl");
		assertNull(mediaAclLocation);
		
	}
	
	
	


	
	
	public void testUpdateGlobalAcl() {
		
		// make sure adam can create collections
		String u = createTestCollection(SERVER_URI, "adam", "test");
		assertNotNull(u);

		// strip global acls
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		
		PutMethod p = new PutMethod(ACL_URI);
		setAtomRequestEntity(p, content);
		int r = executeMethod(p, "adam", "test");
		assertEquals(200, r);
		verifyAtomResponse(p);
		Document e = getResponseBodyAsDocument(p);
		verifyDocIsAtomEntryWithAclContent(e);
		
		// now try to create a collection
		String v = createTestCollection(SERVER_URI, "adam", "test");
		assertNull(v);
		 
	}
	
	
	
	public void testUpdateGlobalAclDenied() {
		
		// try to update global acls
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		
		PutMethod p = new PutMethod(ACL_URI);
		setAtomRequestEntity(p, content);
		int r = executeMethod(p, "rebecca", "test");
		assertEquals(403, r);
		
	}
	
	
	
	public void testUpdateCollectionAcl() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
 
		// try to retrieve collection feed as rebecca (reader) to check allowed
		GetMethod f = new GetMethod(collectionUri);
		int q = executeMethod(f, "rebecca", "test");
		assertEquals(200, q);
		

		// retrieve collection feed as adam (administrator) to get edit-acl link
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// update the acl
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int s = executeMethod(p, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(p);
		Document e = getResponseBodyAsDocument(p);
		verifyDocIsAtomEntryWithAclContent(e);

		// try to retrieve collection feed again as rebecca to check forbidden
		GetMethod h = new GetMethod(collectionUri);
		int t = executeMethod(h, "rebecca", "test");
		assertEquals(403, t);
		
	}
	
	
	
	public void testUpdateCollectionAclDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
 
		// retrieve collection feed as adam (administrator) to get edit-acl link
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// try to update the acl
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int s = executeMethod(p, "rebecca", "test");
		assertEquals(403, s);
		
	}
	
	
	
	public void testBadAclRequest() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
 
		// retrieve collection feed as adam (administrator) to get edit-acl link
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// try to update the acl with bad content - missing rules
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int s = executeMethod(p, "adam", "test");
		assertEquals(400, s);

	}
	
	
	
	
	public void testUpdateMemberAcl() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// try to update the acl
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int s = executeMethod(p, "audrey", "test");
		assertEquals(200, s);

		// try retrieve member entry again
		GetMethod h = new GetMethod(memberUri);
		int t = executeMethod(h, "audrey", "test");
		assertEquals(403, t);

	}
	
	
	

	public void testUpdateMemberAclDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
		// look for "edit-acl" link
		Document d = getResponseBodyAsDocument(g);
		String aclLocation = getLinkHref(d, "edit-acl");
		assertNotNull(aclLocation);
		
		// try to update the acl
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int s = executeMethod(p, "rebecca", "test");
		assertEquals(403, s);

		// try retrieve member entry again
		GetMethod h = new GetMethod(memberUri);
		int t = executeMethod(h, "audrey", "test");
		assertEquals(200, t);

	}
	
	
	
	public void testUpdateMediaAcl() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(d);
		
		// retrieve media resource
		GetMethod g = new GetMethod(mediaLocation);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);

		// look for "edit-acl" link
		String aclLocation = getLinkHref(d, "edit-media-acl");
		assertNotNull(aclLocation);
		
		// try to update the acl
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int t = executeMethod(p, "audrey", "test");
		assertEquals(200, t);

		// try retrieve media resource again
		GetMethod i = new GetMethod(mediaLocation);
		int u = executeMethod(i, "audrey", "test");
		assertEquals(403, u);

	}
	
	
	

	public void testUpdateMediaAclDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(d);
		
		// retrieve media resource
		GetMethod g = new GetMethod(mediaLocation);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);

		// look for "edit-acl" link
		String aclLocation = getLinkHref(d, "edit-media-acl");
		assertNotNull(aclLocation);
		
		// try to update the acl
		PutMethod p = new PutMethod(aclLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<acl xmlns=\"\"><rules/></acl>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		int t = executeMethod(p, "rebecca", "test");
		assertEquals(403, t);

		// try retrieve media resource again
		GetMethod i = new GetMethod(mediaLocation);
		int u = executeMethod(i, "audrey", "test");
		assertEquals(200, u);

	}
	
	
	
	
	public void testCannotOverrideAclLinksOnCreateCollection() {
		
		String collectionUri = SERVER_URI + Double.toString(Math.random());
		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
				"<atom:link rel=\"edit-acl\" href=\"http://foo.bar/spong\"/>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, "adam", "test");

		// expect the status code is 201 CREATED
		assertEquals(201, result);
		
		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, "edit-acl");
		assertEquals(1, links.size());

	}
	
	
	public void testCannotOverrideAclLinksOnUpdateCollection() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		
		// now try to update feed metadata via a PUT request
		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
				"<atom:link rel=\"edit-acl\" href=\"http://foo.bar/spong\"/>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, "adam", "test");
		
		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result);
		
		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, "edit-acl");
		assertEquals(1, links.size());

	}
	
	
	public void testCannotOverrideAclLinksOnCreateMember() {

		// setup test
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
				"<atom:link rel=\"edit-acl\" href=\"http://foo.bar/spong\"/>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, "adam", "test");
		
		// expect the status code is 201 Created
		assertEquals(201, result);

		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, "edit-acl");
		assertEquals(1, links.size());
	}
	
	
	public void testCannotOverrideAclLinksOnUpdateMember() {
		
		// setup test
		String collectionUri = createTestCollection(SERVER_URI, "adam", "test");
		String location = createTestEntryAndReturnLocation(collectionUri, "adam", "test");

		// now put an updated entry document using a PUT request
		PutMethod method = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
				"<atom:link rel=\"edit-acl\" href=\"http://foo.bar/spong\"/>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, "adam", "test");

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result);

		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, "edit-acl");
		assertEquals(1, links.size());
	}
	
	private static String verifyAtomResponse(HttpMethod m) {

		String h = m.getResponseHeader("Content-Type").getValue();
		assertNotNull(h);
		assertTrue(h.startsWith("application/atom+xml"));
		return h;
		
	}
	
	
	
	
	private static Element verifyDocIsAtomEntryWithAclContent(Document d) {

		// verify root element is atom entry
		Element e = d.getDocumentElement();
		assertEquals("entry", e.getLocalName());
		assertEquals("http://www.w3.org/2005/Atom", e.getNamespaceURI());
		
		// verify atom content element is present
		Element c = (Element) e.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "content").item(0);
		assertNotNull(c);
		
		// verify acl content
		NodeList n = c.getElementsByTagNameNS("", "acl");
		Element a = (Element) n.item(0);
		assertNotNull(a);
		
		return a;
		
	}
	
	
	
	
	// TODO test that edit-acl and edit-media acl links are provided after
	// retrieve, create and update operations, as appropriate
	
}



