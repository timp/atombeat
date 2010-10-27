package org.atombeat.it.tombstones;

import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.atombeat.Atom;
import org.atombeat.AtomBeat;
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
		
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-tombstones=\"true\">" +
				"<atom:title>Test Collection with Tombstones</atom:title>" +
			"</atom:feed>";
		
		return createTombstoneEnabledCollection(content);
		
	}
	
	
	private static String createTombstoneEnabledCollection(String content) throws Exception {
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
		
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

	
	
	public void testDeleteMediaResourceViaMediaLinkUri() throws Exception {
		
		// create a tombstone-enabled collection
		String collectionUri = createTombstoneEnabledCollection();
		
		// create a media resource
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		String memberId = getAtomId(mediaLinkDoc);
		
		// retrieve the media-link member, verify response prior to deletion
		GetMethod getMediaLink = new GetMethod(mediaLinkLocation);
		int getMediaLinkResult = executeMethod(getMediaLink);
		assertEquals(200, getMediaLinkResult);
		
		// retrieve the media resource, verify the response prior to deletion
		GetMethod getMedia = new GetMethod(mediaLocation);
		int getMediaResult = executeMethod(getMedia);
		assertEquals(200, getMediaResult);
		
		// list the collection, verify the response prior to deletion
		GetMethod getFeed = new GetMethod(collectionUri);
		int getFeedResult = executeMethod(getFeed);
		assertEquals(200, getFeedResult);
		Document feed = getResponseBodyAsDocument(getFeed);
		assertEquals(1, getEntries(feed).size());
		List<Element> deletedEntries = getChildrenByTagNameNS(feed, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(0, deletedEntries.size());
		
		// delete the media resource via the member URI
		DeleteMethod delete = new DeleteMethod(mediaLinkLocation);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "delete media resource via media link URI");
		int deleteResult = executeMethod(delete);
		
		// verify delete response
		assertEquals(200, deleteResult);
		verifyResponseBodyIsDeletedEntry(delete, memberId, USER, "delete media resource via media link URI");
		
		// retrieve the media-link member, verify response after deletion
		GetMethod getMediaLink2 = new GetMethod(mediaLinkLocation);
		int getMediaLinkResult2 = executeMethod(getMediaLink2);
		assertEquals(410, getMediaLinkResult2);
		verifyResponseBodyIsDeletedEntry(getMediaLink2, memberId, USER, "delete media resource via media link URI");
		
		// retrieve the media resource, verify the response after deletion
		GetMethod getMedia2 = new GetMethod(mediaLocation);
		int getMediaResult2 = executeMethod(getMedia2);
		assertEquals(404, getMediaResult2);
		
		// list the collection, verify the response after deletion
		GetMethod getFeed2 = new GetMethod(collectionUri);
		int getFeedResult2 = executeMethod(getFeed2);
		assertEquals(200, getFeedResult2);
		Document feed2 = getResponseBodyAsDocument(getFeed2);
		assertEquals(0, getEntries(feed2).size());
		List<Element> deletedEntries2 = getChildrenByTagNameNS(feed2, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(1, deletedEntries2.size());
		
	}




	public void testDeleteMediaResourceViaMediaResourceUri() throws Exception {
		
		// create a tombstone-enabled collection
		String collectionUri = createTombstoneEnabledCollection();
		
		// create a media resource
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, USER, PASS);
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		String memberId = getAtomId(mediaLinkDoc);
		
		// retrieve the media-link member, verify response prior to deletion
		GetMethod getMediaLink = new GetMethod(mediaLinkLocation);
		int getMediaLinkResult = executeMethod(getMediaLink);
		assertEquals(200, getMediaLinkResult);
		
		// retrieve the media resource, verify the response prior to deletion
		GetMethod getMedia = new GetMethod(mediaLocation);
		int getMediaResult = executeMethod(getMedia);
		assertEquals(200, getMediaResult);
		
		// list the collection, verify the response prior to deletion
		GetMethod getFeed = new GetMethod(collectionUri);
		int getFeedResult = executeMethod(getFeed);
		assertEquals(200, getFeedResult);
		Document feed = getResponseBodyAsDocument(getFeed);
		assertEquals(1, getEntries(feed).size());
		List<Element> deletedEntries = getChildrenByTagNameNS(feed, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(0, deletedEntries.size());
		
		// delete the media resource via the media resource URI
		DeleteMethod delete = new DeleteMethod(mediaLocation);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "delete media resource via media URI");
		int deleteResult = executeMethod(delete);
		
		// verify delete response
		assertEquals(200, deleteResult);
		verifyResponseBodyIsDeletedEntry(delete, memberId, USER, "delete media resource via media URI");
		
		// retrieve the media-link member, verify response after deletion
		GetMethod getMediaLink2 = new GetMethod(mediaLinkLocation);
		int getMediaLinkResult2 = executeMethod(getMediaLink2);
		assertEquals(410, getMediaLinkResult2);
		verifyResponseBodyIsDeletedEntry(getMediaLink2, memberId, USER, "delete media resource via media URI");
		
		// retrieve the media resource, verify the response after deletion
		GetMethod getMedia2 = new GetMethod(mediaLocation);
		int getMediaResult2 = executeMethod(getMedia2);
		assertEquals(404, getMediaResult2);
		
		// list the collection, verify the response after deletion
		GetMethod getFeed2 = new GetMethod(collectionUri);
		int getFeedResult2 = executeMethod(getFeed2);
		assertEquals(200, getFeedResult2);
		Document feed2 = getResponseBodyAsDocument(getFeed2);
		assertEquals(0, getEntries(feed2).size());
		List<Element> deletedEntries2 = getChildrenByTagNameNS(feed2, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(1, deletedEntries2.size());
		
	}
	
	
	
	public void testDefaultGhostConfiguration() throws Exception {
		
		// create a collection with default ghost configuration
		String collectionUri = createTombstoneEnabledCollection();
		
		// create member 
		Document entryDoc = createTestMemberAndReturnDocument(collectionUri, USER, PASS);
		String memberUri = getEditLocation(entryDoc);
		
		// delete the member 
		DeleteMethod delete = new DeleteMethod(memberUri);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove spam");
		executeMethod(delete);

		// verify response (elements in ghost)
		Document d = getResponseBodyAsDocument(delete);
		List<Element> ghosts = getChildrenByTagNameNS(d, AtomBeat.XMLNS, AtomBeat.GHOST);
		assertEquals(0, ghosts.size());
		
	}


	
	public void testSpecificGhostConfiguration() throws Exception {
		
		// create a collection with default ghost configuration
		
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-tombstones=\"true\">" +
				"<atom:title>Test Collection with Tombstones and Ghosts</atom:title>" +
				"<atombeat:config-tombstones>" +
					"<atombeat:config>" +
						"<atombeat:param name=\"ghost-atom-elements\" value=\"title author published\"/>" +
					"</atombeat:config>" +
				"</atombeat:config-tombstones>" +
			"</atom:feed>";
		
		String collectionUri = createTombstoneEnabledCollection(content);
		
		// create member 
		Document entryDoc = createTestMemberAndReturnDocument(collectionUri, USER, PASS);
		String memberUri = getEditLocation(entryDoc);
		String title = getAtomTitle(entryDoc);
		String published = getAtomPublished(entryDoc);
		String authorName = getAtomAuthorName(entryDoc);
		
		// delete the member 
		DeleteMethod delete = new DeleteMethod(memberUri);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove spam");
		executeMethod(delete);

		// verify response (elements in ghost)
		Document d = getResponseBodyAsDocument(delete);
		List<Element> ghosts = getChildrenByTagNameNS(d, AtomBeat.XMLNS, AtomBeat.GHOST);
		assertEquals(1, ghosts.size());
		Element ghost = ghosts.get(0);
		String ghostTitle = getAtomTitle(ghost); assertEquals(title, ghostTitle);
		String ghostPublished = getAtomPublished(ghost); assertEquals(published, ghostPublished);
		String ghostAuthorName = getAtomAuthorName(ghost); assertEquals(authorName, ghostAuthorName);

	}

	
	
	public void testInteractionWithHistoryPluginNoTombstones() throws Exception {
		
		// create versioned collection, tombstones disabled
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-tombstones=\"false\" " +
				"atombeat:enable-versioning=\"true\">" +
				"<atom:title>Test Collection with Versioning but no Tombstones</atom:title>" +
			"</atom:feed>";
		String collectionUri = createTombstoneEnabledCollection(content);
		
		// create and update a member
		String location = createTestMemberAndReturnLocation(collectionUri, USER, PASS);
		String entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		Header[] headers = { new Header("X-Atom-Revision-Comment", "second draft") };
		Document updatedEntryDoc = putEntry(location, entryDoc, headers, USER, PASS);
		String historyLocation = getHistoryLocation(updatedEntryDoc);
		assertNotNull(historyLocation);
		
		// retrieve history feed, verify response prior to deletion
		GetMethod get = new GetMethod(historyLocation);
		int result = executeMethod(get);
		assertEquals(200, result);
		Document historyFeedDoc = getResponseBodyAsDocument(get);
		List<Element> entries = getEntries(historyFeedDoc);
		assertEquals(2, entries.size());
		List<Element> deletedEntries = getChildrenByTagNameNS(historyFeedDoc, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(0, deletedEntries.size());
		
		// retrieve revisions, verify response prior to deletion
		for (Element e : entries) {
			String revloc = getLinkHref(e, "this-revision");
			GetMethod getrev = new GetMethod(revloc);
			int getrevres = executeMethod(getrev);
			assertEquals(200, getrevres);
		}
		
		// delete member
		DeleteMethod delete = new DeleteMethod(location);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove versioned resource");
		int delresult = executeMethod(delete);
		assertEquals(204, delresult);
		
		// retrieve history feed, verify response after deletion
		GetMethod gethist2 = new GetMethod(historyLocation);
		int gethist2result = executeMethod(gethist2);
		assertEquals(404, gethist2result);
		
		// retrieve revisions, verify response after deletion
		for (Element e : entries) {
			String revloc = getLinkHref(e, "this-revision");
			GetMethod getrev = new GetMethod(revloc);
			int getrevres = executeMethod(getrev);
			assertEquals(404, getrevres);
		}
		
	}




	public void testInteractionWithHistoryPlugin() throws Exception {
		
		// create versioned collection, tombstones disabled
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-tombstones=\"true\" " +
				"atombeat:enable-versioning=\"true\">" +
				"<atom:title>Test Collection with Versioning but no Tombstones</atom:title>" +
			"</atom:feed>";
		String collectionUri = createTombstoneEnabledCollection(content);
		
		// create and update a member
		String location = createTestMemberAndReturnLocation(collectionUri, USER, PASS);
		String entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";
		Header[] headers = { new Header("X-Atom-Revision-Comment", "second draft") };
		Document updatedEntryDoc = putEntry(location, entryDoc, headers, USER, PASS);
		String historyLocation = getHistoryLocation(updatedEntryDoc);
		assertNotNull(historyLocation);
		
		// retrieve history feed, verify response prior to deletion
		GetMethod get = new GetMethod(historyLocation);
		int result = executeMethod(get);
		assertEquals(200, result);
		Document historyFeedDoc = getResponseBodyAsDocument(get);
		List<Element> entries = getEntries(historyFeedDoc);
		assertEquals(2, entries.size());
		List<Element> deletedEntries = getChildrenByTagNameNS(historyFeedDoc, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(0, deletedEntries.size());
		
		// retrieve revisions, verify response prior to deletion
		for (Element e : entries) {
			String revloc = getLinkHref(e, "this-revision");
			GetMethod getrev = new GetMethod(revloc);
			int getrevres = executeMethod(getrev);
			assertEquals(200, getrevres);
		}
		
		// delete member
		DeleteMethod delete = new DeleteMethod(location);
		delete.setRequestHeader("X-Atom-Tombstone-Comment", "remove versioned resource");
		int delresult = executeMethod(delete);
		assertEquals(200, delresult);
		
		// retrieve history feed, verify response after deletion
		GetMethod gethist2 = new GetMethod(historyLocation);
		int gethist2result = executeMethod(gethist2);
		assertEquals(200, gethist2result);
		Document historyFeedDoc2 = getResponseBodyAsDocument(gethist2);
		List<Element> entries2 = getEntries(historyFeedDoc2);
		assertEquals(2, entries2.size());
		List<Element> deletedEntries2 = getChildrenByTagNameNS(historyFeedDoc2, Tombstones.NSURI, Tombstones.DELETED_ENTRY);
		assertEquals(1, deletedEntries2.size());
		Element delent = deletedEntries2.get(0);
		Element revmeta = getChildrenByTagNameNS(delent, "http://purl.org/atompub/revision/1.0", "revision").get(0);
		assertEquals("3", revmeta.getAttribute("number"));
		assertEquals("no", revmeta.getAttribute("initial"));
		assertEquals("yes", revmeta.getAttribute("final"));
		assertEquals("yes", revmeta.getAttribute("significant"));
		
		// retrieve revisions, verify response after deletion
		for (Element e : entries2) {

			String revloc = getLinkHref(e, "this-revision");
			GetMethod getrev = new GetMethod(revloc);
			int getrevres = executeMethod(getrev);
			assertEquals(200, getrevres);

			String nextrevloc = getLinkHref(e, "next-revision");
			assertNotNull(nextrevloc);
			if (nextrevloc != null) {
				// check we can always retrieve the next revision (which might be a deleted-entry)
				GetMethod getnextrev = new GetMethod(nextrevloc);
				int getnextrevres = executeMethod(getnextrev);
				assertEquals(200, getnextrevres);
			}

		}
		
	}

}
