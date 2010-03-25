package org.cggh.chassis.spike.atomserver;



import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.cggh.chassis.spike.atomserver.AtomTestUtils.*;

import junit.framework.TestCase;

public class TestHistoryProtocol extends TestCase {

	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	private static final String SERVER_URI = "http://localhost:8081/atomserver/atomserver/content/";

	

	private String collectionUri = null;
	
	
	
	public void setUp() {
		
		Header[] headers = {
				new Header("X-Atom-Enable-History", "true")
		};
		
		collectionUri = createTestCollection(SERVER_URI, USER, PASS, headers);
	
	}
	
	
	

	public void testEntryWithNoRevisions() {
		
		// the request body - an atom entry document
		String entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Will Not Be Revised</atom:title>" +
				"<atom:summary>This is a summary, will not be revised.</atom:summary>" +
			"</atom:entry>";

		Header[] headers = {
				new Header("X-Atom-Revision-Comment", "first draft")
		};
		
		Document doc = postEntry(collectionUri, entryDoc, headers, USER, PASS);
		verifyRevisionComments(doc, 1);
		
		String historyLocation = getHistoryLocation(doc);
		assertNotNull(historyLocation);
		
		// now try to retrieve history
		verifyHistory(historyLocation, 1);

	}

	
	
	public void testEntryWithOneRevision() {
		
		// create an atom entry
		
		String entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary, first daft.</atom:summary>" +
			"</atom:entry>";

		Header[] headers = {
				new Header("X-Atom-Revision-Comment", "first draft")
		};
		
		Document initialDoc = postEntry(collectionUri, entryDoc, headers, USER, PASS);
		String location = getEditLocation(initialDoc);
		assertNotNull(location);
		verifyRevisionComments(initialDoc, 1);
		
		// now make a revision
		
		entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";

		Header[] headers2 = {
				new Header("X-Atom-Revision-Comment", "second draft")
		};
		
		Document updatedDoc = putEntry(location, entryDoc, headers2, USER, PASS);
		String historyLocation = getHistoryLocation(updatedDoc);
		assertNotNull(historyLocation);
		verifyRevisionComments(updatedDoc, 1); // only keep latest comment

		// now try to retrieve history
		verifyHistory(historyLocation, 2);

	}



	public void testEntryWithTwoRevisions() {
		
		// create an atom entry
		
		String entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary, first daft.</atom:summary>" +
			"</atom:entry>";

		Header[] headers = {
				new Header("X-Atom-Revision-Comment", "first draft")
		};
		
		Document initialDoc = postEntry(collectionUri, entryDoc, headers, USER, PASS);
		String location = getEditLocation(initialDoc);
		assertNotNull(location);

		// now make a revision
		
		entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";

		Header[] headers2 = {
				new Header("X-Atom-Revision-Comment", "second draft")
		};
		
		putEntry(location, entryDoc, headers2, USER, PASS);

		// now make another revision
		
		entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated Again</atom:title>" +
				"<atom:summary>This is a summary, updated (third draft).</atom:summary>" +
			"</atom:entry>";

		Header[] headers3 = {
				new Header("X-Atom-Revision-Comment", "third draft")
		};
		
		Document updatedAgainDoc = putEntry(location, entryDoc, headers3, USER, PASS);
		String historyLocation = getHistoryLocation(updatedAgainDoc);
		assertNotNull(historyLocation);
		verifyRevisionComments(updatedAgainDoc, 1); // only keep latest comment
		
		Element title = (Element) updatedAgainDoc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated Again", title.getTextContent());
		
		// now try to retrieve history
		verifyHistory(historyLocation, 3);

	}



	public void testEntryWithThreeRevisions() {
		
		// create an atom entry
		
		String entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member</atom:title>" +
				"<atom:summary>This is a summary, first daft.</atom:summary>" +
			"</atom:entry>";

		Header[] headers = {
				new Header("X-Atom-Revision-Comment", "first draft")
		};
		
		Document initialDoc = postEntry(collectionUri, entryDoc, headers, USER, PASS);
		String location = getEditLocation(initialDoc);
		assertNotNull(location);

		// now make a revision
		
		entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated</atom:title>" +
				"<atom:summary>This is a summary, updated.</atom:summary>" +
			"</atom:entry>";

		Header[] headers2 = {
				new Header("X-Atom-Revision-Comment", "second draft")
		};
		
		putEntry(location, entryDoc, headers2, USER, PASS);

		// now make another revision
		
		entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated Again</atom:title>" +
				"<atom:summary>This is a summary, updated (third draft).</atom:summary>" +
			"</atom:entry>";

		Header[] headers3 = {
				new Header("X-Atom-Revision-Comment", "third draft")
		};
		
		Document updatedAgainDoc = putEntry(location, entryDoc, headers3, USER, PASS);
		verifyRevisionComments(updatedAgainDoc, 1); // only keep latest comment
		Element title = (Element) updatedAgainDoc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated Again", title.getTextContent());
		
		// now make another revision
		
		entryDoc = 
			"<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Member - Updated Yet Again</atom:title>" +
				"<atom:summary>This is a summary, updated yet again (fourth draft).</atom:summary>" +
			"</atom:entry>";

		Header[] headers4 = {
				new Header("X-Atom-Revision-Comment", "fourth draft")
		};
		
		Document updatedYetAgainDoc = putEntry(location, entryDoc, headers4, USER, PASS);
		String historyLocation = getHistoryLocation(updatedYetAgainDoc);
		assertNotNull(historyLocation);
		verifyRevisionComments(updatedYetAgainDoc, 1); // only keep latest comment
		title = (Element) updatedYetAgainDoc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "title").item(0);
		assertEquals("Test Member - Updated Yet Again", title.getTextContent());
		
		// now try to retrieve history
		verifyHistory(historyLocation, 4);

	}



	private static void verifyHistory(String historyLocation, int expectedRevisions) {
		
		GetMethod get = new GetMethod(historyLocation);
		int result = executeMethod(get, USER, PASS);
		
		// expect the status code is 200 OK
		assertEquals(200, result);

		// expect Content-Type header 
		String responseContentType = get.getResponseHeader("Content-Type").getValue();
		assertNotNull(responseContentType);
		assertTrue(responseContentType.trim().startsWith("application/atom+xml"));

		// now check content is feed
		Document historyFeedDoc = getResponseBodyAsDocument(get);
		assertNotNull(historyFeedDoc);

		// now check content is feed
		NodeList feedNodes = historyFeedDoc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "feed");
		assertEquals(1, feedNodes.getLength());
		
		// check number of entries
		NodeList entryNodes = historyFeedDoc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
		assertEquals(expectedRevisions, entryNodes.getLength());
		
		for (int i=0; i<entryNodes.getLength(); i++) {
			Element entry = (Element) entryNodes.item(i);
			verifyRevisionMetadata(entry, i+1);
		}

	}
	
	
	
	public static void verifyRevisionComments(Document doc, int n) {
		
		// check number of comments
		NodeList entryNodes = doc.getElementsByTagNameNS("http://purl.org/atompub/revision/1.0", "comment");
		assertEquals(n, entryNodes.getLength());

	}


	
	public static void verifyRevisionMetadata(Element entry, int n) {
		
		// get revision element
		NodeList nodes = entry.getElementsByTagNameNS("http://purl.org/atompub/revision/1.0", "revision");
		assertEquals(1, nodes.getLength());

		Element revision = (Element) nodes.item(0);
		assertEquals(n, Integer.parseInt(revision.getAttribute("number")));
		
		if (n==1) {
			assertEquals("yes", revision.getAttribute("initial"));
		}
		else {
			assertEquals("no", revision.getAttribute("initial"));
		}
	}


	
}
