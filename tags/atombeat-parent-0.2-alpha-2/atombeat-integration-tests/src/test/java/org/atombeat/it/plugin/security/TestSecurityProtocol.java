package org.atombeat.it.plugin.security;



import org.atombeat.it.AtomTestUtils;
import static org.atombeat.it.AtomTestUtils.*;

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
		
		String installUrl = SERVICE_URL + "admin/setup-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method, "adam", "test");
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}
		
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	

	public void testGetWorkspaceDescriptor() throws Exception {
		
		GetMethod g = new GetMethod(SECURITY_URI);
		int r = executeMethod(g, "adam", "test");

		assertEquals(200, r);
		
		verifyAtomResponse(g);
		
		Document d = getResponseBodyAsDocument(g);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(d);
		
	}
	
	
	
	public void testGetWorkspaceDescriptorDenied() {

		GetMethod g = new GetMethod(SECURITY_URI);
		int r = executeMethod(g, "rebecca", "test");

		assertEquals(403, r);

	}
	

	
	public void testGetCollectionDescriptor() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a second get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

	}
	
	
	
//	public void testGetCollectionNoDescriptorLink() {
//
//		// set up test by creating a collection
//		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
//
//		// retrieve collection feed
//		GetMethod g = new GetMethod(collectionUri);
//		int r = executeMethod(g, "rebecca", "test");
//		assertEquals(200, r);
//		
//		// look for ACL link
//		Document d = getResponseBodyAsDocument(g);
//		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
//		assertNull(descriptorLocation);
//		
//	}
	
	
	
	public void testGetCollectionDescriptorDenied() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");

		// retrieve collection feed
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a second get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "rebecca", "test");
		assertEquals(403, s);

	}
	
	
	
	public void testGetMemberDescriptor() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestMemberAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// check atombeat:allow extension attribute
		Element link = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR).get(0);
		assertNotNull(link); 
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

	}
	
	
	

	public void testGetMemberDescriptor2() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		createTestMemberAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve collection
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
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
		int s = executeMethod(h, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document f = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(f);

	}
	
	
	

	public void testGetMemberDescriptor3() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestMemberAndReturnLocation(collectionUri, "audrey", "test");

		// edwina is an editor, and should be able to retrieve acl but not update
		
		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "edwina", "test");
		assertEquals(200, r);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// check atombeat:allow extension attribute
		Element link = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR).get(0);
		assertNotNull(link); 
		
		// make a get request for the descriptor as editor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "edwina", "test");
		assertEquals(200, s);
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
		int t = executeMethod(p, "edwina", "test");
		assertEquals(403, t);

	}
	
	
	

//	public void testGetMemberNoDescriptorLink() {
//
//		// set up test by creating a collection
//		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
//		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");
//
//		// retrieve member entry
//		GetMethod g = new GetMethod(memberUri);
//		int r = executeMethod(g, "rebecca", "test");
//		assertEquals(200, r);
//		
//		// look for ACL link
//		Document d = getResponseBodyAsDocument(g);
//		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
//		assertNull(descriptorLocation);
//		
//	}
	
	
	
	public void testGetMemberNoMediaDescriptorLink() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestMemberAndReturnLocation(collectionUri, "adam", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String mediaDescriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNull(mediaDescriptorLocation);
		
	}
	
	
	
	public void testDescriptorLinkPresentInResponseToCreateEntry() throws Exception {
		
		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMemberAndReturnDocument(collectionUri, "audrey", "test");

		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
	}
	
	
	
	public void testGetMemberDescriptorDenied() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestMemberAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
		// look for ACL link
		Document d = getResponseBodyAsDocument(g);
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "rebecca", "test");
		assertEquals(403, s);

	}

	
	
//	public void testGetMemberDescriptorLinkExcluded() {
//
//		// set up test by creating a collection
//		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
//		String memberUri = createTestEntryAndReturnLocation(collectionUri, "audrey", "test");
//
//		// retrieve member entry
//		GetMethod g = new GetMethod(memberUri);
//		int r = executeMethod(g, "rebecca", "test");
//		assertEquals(200, r);
//		
//		// look for ACL link
//		Document d = getResponseBodyAsDocument(g);
//		String descriptorLocation = getLinkHref(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
//		assertNull(descriptorLocation);
//		
//	}

	
	
	public void testGetMediaDescriptor() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		// look for ACL link
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "audrey", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document e = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

	}
	
	
	

	public void testGetMediaDescriptor2() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		GetMethod get = new GetMethod(collectionUri);
		executeMethod(get, "audrey", "test");
		Document d = getResponseBodyAsDocument(get);
		Element e = (Element) d.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry").item(0);
		assertNotNull(e);
		
		// look for ACL link
		String descriptorLocation = getLinkHref(e, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "audrey", "test");
		assertEquals(200, s);
		verifyAtomResponse(h);
		Document f = getResponseBodyAsDocument(h);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(f);

	}
	
	
	

	public void testGetMediaDescriptorDenied() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");

		// look for ACL link
		String descriptorLocation = getLinkHref(d, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
		assertNotNull(descriptorLocation);
		
		// make a get request for the descriptor
		GetMethod h = new GetMethod(descriptorLocation);
		int s = executeMethod(h, "rebecca", "test");
		assertEquals(403, s);

	}
	
	
	

//	public void testGetMediaDescriptorNoDescriptorLink() {
//
//		// set up test by creating a collection
//		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
//		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
//		String location = getEditLocation(d);
//		
//		// retrieve media link as rebecca
//		GetMethod g = new GetMethod(location);
//		int s = executeMethod(g, "rebecca", "test");
//		assertEquals(200, s);
//		Document e = getResponseBodyAsDocument(g);
//		String mediaDescriptorLocation = getLinkHref(e, AtomBeat.REL_MEDIA_SECURITY_DESCRIPTOR);
//		assertNull(mediaDescriptorLocation);
//		
//	}
	
	
	


	
	
	public void testUpdateWorkspaceDescriptor() throws Exception {
		
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
		int r = executeMethod(p, "adam", "test");
		assertEquals(200, r);
		verifyAtomResponse(p);
		Document e = getResponseBodyAsDocument(p);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);
		
		// now try to create a collection
		String v = createTestCollection(CONTENT_URI, "adam", "test");
		assertNull(v);
		 
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
		int r = executeMethod(p, "rebecca", "test");
		assertEquals(403, r);
		
	}
	
	
	
	public void testUpdateCollectionDescriptor() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
 
		// try to retrieve collection feed as rebecca (reader) to check allowed
		GetMethod f = new GetMethod(collectionUri);
		int q = executeMethod(f, "rebecca", "test");
		assertEquals(200, q);
		

		// retrieve collection feed as adam (administrator) to get security descriptor link
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
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
		int s = executeMethod(p, "adam", "test");
		assertEquals(200, s);
		verifyAtomResponse(p);
		Document e = getResponseBodyAsDocument(p);
		verifyDocIsAtomEntryWithSecurityDescriptorContent(e);

		// try to retrieve collection feed again as rebecca to check forbidden
		GetMethod h = new GetMethod(collectionUri);
		int t = executeMethod(h, "rebecca", "test");
		assertEquals(403, t);
		
	}
	
	
	
	public void testUpdateCollectionDescriptorDenied() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
 
		// retrieve collection feed as adam (administrator) to get security descriptor link
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
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
		int s = executeMethod(p, "rebecca", "test");
		assertEquals(403, s);
		
	}
	
	
	
	public void testBadRequest() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
 
		// retrieve collection feed as adam (administrator) to get security descriptor link
		GetMethod g = new GetMethod(collectionUri);
		int r = executeMethod(g, "adam", "test");
		assertEquals(200, r);
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
		int s = executeMethod(p, "adam", "test");
		assertEquals(400, s);

	}
	
	
	
	
	public void testUpdateMemberDescriptor() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestMemberAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
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
		int s = executeMethod(p, "audrey", "test");
		assertEquals(200, s);

		// try retrieve member entry again
		GetMethod h = new GetMethod(memberUri);
		int t = executeMethod(h, "audrey", "test");
		assertEquals(403, t);

	}
	
	
	

	public void testUpdateMemberDescriptorDenied() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String memberUri = createTestMemberAndReturnLocation(collectionUri, "audrey", "test");

		// retrieve member entry
		GetMethod g = new GetMethod(memberUri);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);
		
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
		int s = executeMethod(p, "rebecca", "test");
		assertEquals(403, s);

		// try retrieve member entry again
		GetMethod h = new GetMethod(memberUri);
		int t = executeMethod(h, "audrey", "test");
		assertEquals(200, t);

	}
	
	
	
	public void testUpdateMediaDescriptor() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(d);
		
		// retrieve media resource
		GetMethod g = new GetMethod(mediaLocation);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);

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
		int t = executeMethod(p, "audrey", "test");
		assertEquals(200, t);

		// try retrieve media resource again
		GetMethod i = new GetMethod(mediaLocation);
		int u = executeMethod(i, "audrey", "test");
		assertEquals(403, u);

	}
	
	
	

	public void testUpdateMediaDescriptorDenied() throws Exception {

		// set up test by creating a collection
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		Document d = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(d);
		
		// retrieve media resource
		GetMethod g = new GetMethod(mediaLocation);
		int r = executeMethod(g, "audrey", "test");
		assertEquals(200, r);

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
		int t = executeMethod(p, "rebecca", "test");
		assertEquals(403, t);

		// try retrieve media resource again
		GetMethod i = new GetMethod(mediaLocation);
		int u = executeMethod(i, "audrey", "test");
		assertEquals(200, u);

	}
	
	
	
	
	public void testCannotOverrideDescriptorLinksOnCreateCollection() throws Exception {
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
				"<atom:link rel=\""+AtomBeat.REL_SECURITY_DESCRIPTOR+"\" href=\"http://foo.bar/spong\"/>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, "adam", "test");

		// expect the status code is 201 CREATED
		assertEquals(201, result);
		
		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));

	}
	
	
	public void testCannotOverrideDescriptorLinksOnUpdateCollection() throws Exception {
		
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
		int result = executeMethod(method, "adam", "test");
		
		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result);
		
		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));

	}
	
	
	public void testCannotOverrideDescriptorLinksOnCreateMember() throws Exception {

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
		int result = executeMethod(method, "adam", "test");
		
		// expect the status code is 201 Created
		assertEquals(201, result);

		Document d = getResponseBodyAsDocument(method);
		List<Element> links = getLinks(d, AtomBeat.REL_SECURITY_DESCRIPTOR);
		assertEquals(1, links.size());
		assertFalse(links.get(0).getAttribute("href").equals("http://foo.bar/spong"));
	}
	
	
	public void testCannotOverrideDescriptorLinksOnUpdateMember() throws Exception {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URI, "adam", "test");
		String location = createTestMemberAndReturnLocation(collectionUri, "adam", "test");

		// now put an updated entry document using a PUT request
		PutMethod method = new PutMethod(location);
		String content = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
				"<atom:link rel=\""+AtomBeat.REL_SECURITY_DESCRIPTOR+"\" href=\"http://foo.bar/spong\"/>" +
			"</atom:entry>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, "adam", "test");

		// expect the status code is 200 OK - we just did an update, no creation
		assertEquals(200, result);

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



