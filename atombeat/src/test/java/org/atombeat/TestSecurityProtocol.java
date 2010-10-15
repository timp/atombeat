package org.atombeat;



import static org.atombeat.AtomTestUtils.*;

import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.atombeat.AtomBeat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import junit.framework.TestCase;



public class TestSecurityProtocol extends TestCase {


	
	
	
	protected void setUp() throws Exception {
		super.setUp();
		
		// need to run install before each test to ensure default workspace descriptor is restored
		
		String installUrl = BASE_URI + "admin/setup-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		executeMethod(method, "adam", "test", 200);
		
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	

	public void testGetWorkspaceDescriptor() {
		
		GetMethod g = new GetMethod(SECURITY_URI);
		executeMethod(g, "adam", "test", 200);

		verifyAtomResponse(g);
		
		Document d = getResponseBodyAsDocument(g);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(d);
		
	}
	
	
	
	public void testGetWorkspaceDescriptorDenied() {

		executeMethod(new GetMethod(SECURITY_URI), "rebecca", "test", 403);

	}
	

	
	public void testGetCollectionDescriptor() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "adam", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a second get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "adam", "test", 200);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

	}
	
	
	/** FIXME Failing test */
	public void FAILtestGetCollectionNoDescriptorLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "rebecca", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNull(descriptorLocation);
		
	}
	
	
	
	public void testGetCollectionDescriptorDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "adam", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a second get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "rebecca", "test", 403);

	}
	
	
	
	public void testGetMemberDescriptor() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "audrey", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// check atombeat:allow extension attribute
		Element link = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR).get(0);
		assertNotNull(link); 
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "adam", "test", 200);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

	}
	
	
	

	public void testGetMemberDescriptor2() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve collection
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "audrey", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		Element e = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry").item(0);
		assertNotNull(e);
		
		String descriptorLocation = getLinkHref(e, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// check atombeat:allow extension attribute
		Element link = getLinks(e, AtomBeat.REL_SECURITY_DESCRIPTOR).get(0);
		assertNotNull(link); 
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "adam", "test", 200);
		verifyAtomResponse(h);
		Document f = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(f);

	}
	
	
	

	public void testGetMemberDescriptor3() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// edwina is an editor, and should be able to retrieve acl but not update
		
		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "edwina", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// check atombeat:allow extension attribute
		Element link = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR).get(0);
		assertNotNull(link); 
		
		// make a get request for the descriptor as editor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "edwina", "test", 200);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

		// try to update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "edwina", "test", 403);

	}
	
	
	
/** FIXME FAILing test */
	public void FAILINGtestGetMemberNoDescriptorLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "rebecca", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNull(descriptorLocation);
		
	}
	
	
	
	public void testGetMemberNoMediaDescriptorLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "adam", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "adam", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String mediaDescriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNull(mediaDescriptorLocation);
		
	}
	
	
	
	public void testDescriptorLinkPresentInResponseToCreateEntry() {
		
		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestEntryAndReturnDocument(collectionUri, "audrey", "test");

		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
	}
	
	
	
	public void testGetMemberDescriptorDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "audrey", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "rebecca", "test", 403);

	}

	
/** FIXME Failing test */	
	public void FAILINGtestGetMemberDescriptorLinkExcluded() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "rebecca", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNull(descriptorLocation);
		
	}

	
	
	public void testGetMediaDescriptor() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		// look for ACL link
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "audrey", "test", 200);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

	}
	
	
	

	public void testGetMediaDescriptor2() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		GetMethod get = new GetMethod(collectionUri);
		executeMethod(get, "audrey", "test", 200);
		Document d = getResponseBodyAsDocument(get);
		Element e = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry").item(0);
		assertNotNull(e);
		
		// look for ACL link
		String descriptorLocation = getLinkHref(e, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "audrey", "test", 200);
		verifyAtomResponse(h);
		Document f = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(f);

	}
	
	
	

	public void testGetMediaDescriptorDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		// look for ACL link
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		executeMethod(h, "rebecca", "test", 403);

	}
	
	
	
/** FIXME Failing test */
	public void FAILINGtestGetMediaDescriptorNoDescriptorLink() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String location = getEditLocation(d);
		
		// retrieve media link as rebecca
		GetMethod g = new GetMethod(location);
		executeMethod(g, "rebecca", "test", 200);
		Document e = getResponseBodyAsDocument(g);
		String mediaDescriptorLocation = getLinkHref(e, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNull(mediaDescriptorLocation);
		
	}
	
	
	


	
	
	public void testUpdateWorkspaceDescriptor() {
		
		// make sure adam can create collections
		String u = createTestCollection(CONTENT_URI, "adam", "test");
		assertNotNull(u);

		// strip workspace descriptor
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		
		PutMethod p = new PutMethod(SECURITY_URI);
		setAtomRequestEntity(p, content);
		executeMethod(p, "adam", "test", 200);
		verifyAtomResponse(p);
		Document e = getResponseBodyAsDocument(p);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);
		
		// now try to create a collection
		try { 
		  createTestCollection(CONTENT_URI, "adam", "test");
		} catch (RuntimeException e1) { 
		    assertEquals("Expected status 201 but got 403", e1.getMessage());
		}
		 
	}
	
	
	
	public void testUpdateWorkspaceDescriptorDenied() {
		
		// try to update workspace descriptor
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		
		PutMethod p = new PutMethod(SECURITY_URI);
		setAtomRequestEntity(p, content);
		executeMethod(p, "rebecca", "test", 403);
		
	}
	
	
	
	public void testUpdateCollectionDescriptor() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
 
		// try to retrieve collection feed as rebecca (reader) to check allowed
		GetMethod f = new GetMethod(collectionUri);
		executeMethod(f, "rebecca", "test", 200);
		

		// retrieve collection feed as adam (administrator) to get security descriptor link
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "adam", "test", 200);
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "adam", "test", 200);
		verifyAtomResponse(p);
		Document e = getResponseBodyAsDocument(p);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

		// try to retrieve collection feed again as rebecca to check forbidden
		GetMethod h = new GetMethod(collectionUri);
		executeMethod(h, "rebecca", "test", 403);
		
	}
	
	
	
	public void testUpdateCollectionDescriptorDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
 
		// retrieve collection feed as adam (administrator) to get security descriptor link
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "adam", "test", 200);
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// try to update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "rebecca", "test", 403);
		
	}
	
	
	
	public void testBadRequest() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
 
		// retrieve collection feed as adam (administrator) to get security descriptor link
		GetMethod g = new GetMethod(collectionUri);
		executeMethod(g, "adam", "test", 200);
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// try to update the descriptor with bad content - missing acl
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "adam", "test", 400);

	}
	
	
	
	
	public void testUpdateMemberDescriptor() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "audrey", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// try to update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "audrey", "test", 200);

		// try retrieve member entry again
		GetMethod h = new GetMethod(memberUri);
		executeMethod(h, "audrey", "test", 403);

	}
	
	
	

	public void testUpdateMemberDescriptorDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		executeMethod(g, "audrey", "test", 200);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// try to update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "rebecca", "test", 403);

		// try retrieve member entry again
		GetMethod h = new GetMethod(memberUri);
		executeMethod(h, "audrey", "test", 200);

	}
	
	
	
	public void testUpdateMediaDescriptor() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(d);
		
		// retrieve media resource
		GetMethod g = new GetMethod(mediaLocation);
		executeMethod(g, "audrey", "test", 200);

		// look for ACL link
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// try to update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "audrey", "test", 200);

		// try retrieve media resource again
		GetMethod i = new GetMethod(mediaLocation);
		executeMethod(i, "audrey", "test", 403);

	}
	
	
	

	public void testUpdateMediaDescriptorDenied() {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(d);
		
		// retrieve media resource
		GetMethod g = new GetMethod(mediaLocation);
		executeMethod(g, "audrey", "test", 200);

		// look for ACL link
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// try to update the descriptor
		PutMethod p = new PutMethod(descriptorLocation);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
			"<atom:content type=\"application/vnd.atombeat+xml\">" +
			"<atombeat:security-descriptor xmlns:atombeat=\"http://purl.org/atombeat/xmlns\"><atombeat:acl/></atombeat:security-descriptor>" +
			"</atom:content>" +
			"</atom:entry>";
		setAtomRequestEntity(p, content);
		executeMethod(p, "rebecca", "test", 403);

		// try retrieve media resource again
		GetMethod i = new GetMethod(mediaLocation);
		executeMethod(i, "audrey", "test", 200);

	}
	
	
	
	
	public void testCannotOverrideDescriptorLinksOnCreateCollection() {
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
				"<atom:link rel=\""+AtomBeat.REL_SECURITY_DESCRIPTOR+"\" href=\"http://foo.bar/spong\"/>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
    // expect the status code is 201 CREATED
		executeMethod(method, "adam", "test", 201);

		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));

	}
	
	
	public void testCannotOverrideDescriptorLinksOnUpdateCollection() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		
		// now try to update feed metadata via a PUT request
		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
				"<atom:link rel=\""+AtomBeat.REL_SECURITY_DESCRIPTOR+"\" href=\"http://foo.bar/spong\"/>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method, "adam", "test", 200);
		
		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));

	}
	
	
	public void testCannotOverrideDescriptorLinksOnCreateMember() {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");

		// now create a new member by POSTing and atom entry document to the
		// collection URI
		PostMethod method = new PostMethod(collectionUri);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary.</atom:summary>" +
				"<atom:link rel=\""+AtomBeat.REL_SECURITY_DESCRIPTOR+"\" href=\"http://foo.bar/spong\"/>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
    // expect the status code is 201 Created
		executeMethod(method, "adam", "test", 201);
		
		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));
	}
	
	
	public void testCannotOverrideDescriptorLinksOnUpdateMember() {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String location = createTestEntryAndReturnLocation(collectionUri, "adam", "test");

		// now put an updated entry document using a PUT request
		PutMethod method = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
				"<atom:link rel=\""+AtomBeat.REL_SECURITY_DESCRIPTOR+"\" href=\"http://foo.bar/spong\"/>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
    // expect the status code is 200 OK - we just did an update, no creation
		executeMethod(method, "adam", "test", 200);

		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));
	}
	
	private static String verifyAtomResponse(HttpMethod m) {

		String h = m.getResponseHeader("Content-Type").getValue();
		assertNotNull(h);
		assertTrue(h.startsWith("application/atom+xml"));
		return h;
		
	}
	
	
	
	
	private static Element verifyDocIsAtomEntryWithSecurityDescriptorContent(Document d) {

		// verify root element is atom entry
		Element e = d.getDocumentElement();
		assertEquals("entry", e.getLocalName());
		assertEquals("http://www.w3.org/2005/Atom", e.getNamespaceURI());
		
		// verify atom content element is present
		Element c = (Element) e.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "content").item(0);
		assertNotNull(c);
		
		// verify content
		NodeList n = c.getElementsByTagNameNS("http://purl.org/atombeat/xmlns", "security-descriptor");
		Element a = (Element) n.item(0);
		assertNotNull(a);
		
		Element id = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "id").item(0);
		assertNotNull(id);
		Element title = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertNotNull(title);
		Element updated = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "updated").item(0);
		assertNotNull(updated);
		String selfLocation = AtomTestUtils.getLinkHref(d, "self");
		assertNotNull(selfLocation);
		String editLocation = AtomTestUtils.getLinkHref(d, "edit");
		assertNotNull(editLocation);
		
		return a;
		
	}
	
	
	
	
	// TODO test that security links are provided after
	// retrieve, create and update operations, as appropriate
	
}



