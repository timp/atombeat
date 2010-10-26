package org.atombeat.it.tombstones;

import static org.atombeat.it.AtomTestUtils.BASE_URI;
import static org.atombeat.it.AtomTestUtils.CONTENT_URI;

import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.atombeat.Atom;
import org.atombeat.Tombstones;
import org.atombeat.it.AtomTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.atombeat.it.AtomTestUtils.*;

import junit.framework.TestCase;



/**
 * Tests the expectations defined in http://code.google.com/p/atombeat/wiki/TombstonesDesign
 * @author aliman
 *
 */
public class TestTombstones extends TestCase {

	
	
	protected void setUp() throws Exception {
		super.setUp();

		String installUrl = BASE_URI + "admin/setup-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method);
		
		method.releaseConnection();
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}
		
	}
	
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	

	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	
	
	
	private static Integer executeMethod(HttpMethod method) {
		
		return AtomTestUtils.executeMethod(method, USER, PASS);

	}
	
	
	
	private static String createTombstoneEnabledCollection() throws Exception {
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
		
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-tombstones=\"true\">" +
				"<atom:title>Test Collection with Tombstones</atom:title>" +
			"</atom:feed>";
		
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, content);
		int result = executeMethod(put);
		
		if (result == 201) {
			collectionUri = put.getResponseHeader("Location").getValue();
		}
		else {
			collectionUri = null;
		}
		
		// make sure we release the connection
		put.releaseConnection();
		
		if (collectionUri == null) {
			throw new Exception("Error creating collection.");
		}
		
		return collectionUri;

	}
	
	
	public void testDeleteMemberResponse() throws Exception {
		
		// create a tombstone-enabled collection
		String collectionUri = createTombstoneEnabledCollection();
		
		// create a member to be deleted
		Document entryDoc = createTestMemberAndReturnDocument(collectionUri, USER, PASS);
		String memberUri = getEditLocation(entryDoc);
		String memberId = getAtomId(entryDoc);
		
		// delete the member 
		DeleteMethod delete = new DeleteMethod(memberUri);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove spam");
		int result = executeMethod(delete);
		
		// verify the response to the delete member request
		///////////////////////////////////////////////////
		
		// verify response code and content type
		assertEquals(200, result);
		Header contentTypeHeader = delete.getResponseHeader("Content-Type");
		assertNotNull(contentTypeHeader);
		assertTrue(contentTypeHeader.getValue().startsWith(Tombstones.MEDIATYPE));
		
		// verify response body
		verifyResponseBodyIsDeletedEntry(delete, memberId, USER, "remove spam");
		
		// clean up
		delete.releaseConnection();
		
	}
	
	
	
	public void testRetrieveMemberAfterDeleteMember() throws Exception {
		
		// create a tombstone-enabled collection
		String collectionUri = createTombstoneEnabledCollection();
		
		// create a member to be deleted
		String memberUri = createTestMemberAndReturnLocation(collectionUri, USER, PASS);
		
		// retrieve the member, verify the response prior to deletion
		GetMethod get = new GetMethod(memberUri);
		int getResult = executeMethod(get);
		assertEquals(200, getResult);
		Document entryDoc = getResponseBodyAsDocument(get);
		String memberId = getAtomId(entryDoc);
		assertNotNull(memberId);
		
		// delete the member 
		DeleteMethod delete = new DeleteMethod(memberUri);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove spam");
		executeMethod(delete);
		
		// retrieve the member now deleted
		GetMethod get2 = new GetMethod(memberUri);
		int get2Result = executeMethod(get2);
		
		// verify the response to the retrieve request after deletion
		/////////////////////////////////////////////////////////////

		// verify response code and content type
		assertEquals(410, get2Result); // Gone
		Header contentTypeHeader = delete.getResponseHeader("Content-Type");
		assertNotNull(contentTypeHeader);
		assertTrue(contentTypeHeader.getValue().startsWith(Tombstones.MEDIATYPE));

		// verify response body
		verifyResponseBodyIsDeletedEntry(get2, memberId, USER, "remove spam");
		
		// clean up
		get2.releaseConnection();

	}
	
	
	
	private void verifyResponseBodyIsDeletedEntry(HttpMethod method, String expectedRef, String expectedByName, String expectedComment) throws Exception {

		Document d = getResponseBodyAsDocument(method);
		verifyDocumentIsDeletedEntry(d, expectedRef, expectedByName, expectedComment);
		
	}
	
	
	
	private void verifyDocumentIsDeletedEntry(Document d, String expectedRef, String expectedByName, String expectedComment) throws Exception {

		Element root = d.getDocumentElement();
		verifyElementIsDeletedEntry(root, expectedRef, expectedByName, expectedComment);
		
	}
	
	
	
	private void verifyElementIsDeletedEntry(Element e, String expectedRef, String expectedByName, String expectedComment) throws Exception {

		// verify element
		assertEquals(Tombstones.NSURI, e.getNamespaceURI());
		assertEquals(Tombstones.DELETED_ENTRY, e.getLocalName());
		
		// verify ref attribute is atom id
		String ref = e.getAttribute(Tombstones.REF);
		assertEquals(expectedRef, ref);
		
		// verify when attribute exists
		String when = e.getAttribute(Tombstones.WHEN);
		assertNotNull(when);
		assertFalse("".equals(when));
		
		// verify at:by element
		List<Element> bys = getChildrenByTagNameNS(e, Tombstones.NSURI, Tombstones.BY);
		assertEquals(1, bys.size());
		Element by = bys.get(0);
		List<Element> names = getChildrenByTagNameNS(by, Atom.NSURI, Atom.NAME);
		assertEquals(1, names.size());
		assertEquals(expectedByName, names.get(0).getTextContent());
		
		// verify at:comment element
		List<Element> comments = getChildrenByTagNameNS(e, Tombstones.NSURI, Tombstones.COMMENT);
		assertEquals(1, comments.size());
		assertEquals(expectedComment, comments.get(0).getTextContent());

	}
	
	
	
	public void testListCollectionAfterDeleteMember() throws Exception {
		
		// create a tombstone-enabled collection
		String collectionUri = createTombstoneEnabledCollection();
		
		// create a member to be deleted
		Document entryDoc = createTestMemberAndReturnDocument(collectionUri, USER, PASS);
		String memberUri = getEditLocation(entryDoc);
		String memberId = getAtomId(entryDoc);
		
		// list the collection, verify the response prior to deletion
		GetMethod get = new GetMethod(collectionUri);
		int getResult = executeMethod(get);
		assertEquals(200, getResult);
		Document feedDoc = getResponseBodyAsDocument(get);
		List<Element> entries = getEntries(feedDoc);
		assertEquals(1, entries.size());
		List<Element> deletedEntries = getChildrenByTagNameNS(feedDoc, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(0, deletedEntries.size());
		
		// delete the member 
		DeleteMethod delete = new DeleteMethod(memberUri);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove spam");
		executeMethod(delete);
		
		// list the collection again
		GetMethod get2 = new GetMethod(collectionUri);
		int get2Result = executeMethod(get2);
		
		// verify the list collection response after deletion of the member
		assertEquals(200, get2Result);
		feedDoc = getResponseBodyAsDocument(get2);
		entries = getEntries(feedDoc);
		assertEquals(0, entries.size());
		deletedEntries = getChildrenByTagNameNS(feedDoc, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(1, deletedEntries.size());
		verifyElementIsDeletedEntry(deletedEntries.get(0), memberId, USER, "remove spam");

		// clean up
		get2.releaseConnection();
		
	}

	
	
	public void testDeleteMediaResourceViaMediaLinkUri() {
		
		// create a tombstone-enabled collection
		// TODO
		
		// create a media resource
		// TODO
		
		// retrieve the media-link member, verify response prior to deletion
		// TODO
		
		// retrieve the media resource, verify the response prior to deletion
		// TODO
		
		// list the collection, verify the response prior to deletion
		// TODO
		
		// delete the media resource via the member URI
		// TODO
		
		// verify delete response
		// TODO
		
		// retrieve the media-link member, verify response after deletion
		// TODO
		
		// retrieve the media resource, verify the response after deletion
		// TODO
		
		// list the collection, verify the response after deletion
		// TODO
		
		fail("TODO");
		
	}




	public void testDeleteMediaResourceViaMediaResourceUri() {
		
		// create a tombstone-enabled collection
		// TODO
		
		// create a media resource
		// TODO
		
		// retrieve the media-link member, verify response prior to deletion
		// TODO
		
		// retrieve the media resource, verify the response prior to deletion
		// TODO
		
		// list the collection, verify the response prior to deletion
		// TODO
		
		// delete the media resource via the media resource URI
		// TODO
		
		// verify delete response
		// TODO
		
		// retrieve the media-link member, verify response after deletion
		// TODO
		
		// retrieve the media resource, verify the response after deletion
		// TODO
		
		// list the collection, verify the response after deletion
		// TODO
		
		fail("TODO");
		
	}
	
	
	
	public void testDefaultGhostConfiguration() {
		
		// TODO create a collection with default ghost configuration
		
		// TODO create member, delete member, verify response (elements in ghost)
		
		fail("TODO");
		
	}


	
	public void testSpecificGhostConfiguration() {
		
		// TODO create a collection with specific ghost configuration
		
		// TODO create member, delete member, verify response (elements in ghost)
		
		fail("TODO");
		
	}

	
	
	public void testInteractionWithHistoryPluginNoTombstones() {
		
		// TODO create versioned collection, tombstones disabled
		
		// TODO create and update a member
		
		// TODO retrieve history feed, verify response prior to deletion
		
		// TODO retrieve revisions, verify response prior to deletion
		
		// TODO delete member
		
		// TODO retrieve history feed, verify response after deletion
		
		// TODO retrieve revisions, verify response after deletion
		
		fail("TODO");
		
	}




	public void testInteractionWithHistoryPlugin() {
		
		// TODO create versioned collection, tombstones enabled
		
		// TODO create and update a member
		
		// TODO retrieve history feed, verify response prior to deletion
		
		// TODO retrieve revisions, verify response prior to deletion
		
		// TODO delete member
		
		// TODO retrieve history feed, verify response after deletion
		
		// TODO retrieve revisions, verify response after deletion
		
		fail("TODO");
		
	}

}
