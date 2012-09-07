package org.atombeat.it.plugin.linkexpansion;

import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.*;




public class TestLinkExpansionPlugin extends TestCase {


	
	
	
	protected void setUp() throws Exception {
		super.setUp();

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

	
	

	public void testExpansionInFeed() throws Exception {
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='feed'>\n" +
			"			<atombeat:param name='match-rels' value='related http://purl.org/atombeat/rel/security-descriptor'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>\n" +
			"	<atom:link rel='related' href='"+TEST_COLLECTION_URL+"'/>" +
			"</atom:feed>";
		
		// create the collection
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, feed);
		int putResult = executeMethod(put, "adam", "test");
		assertEquals(201, putResult);
		
		// list the collection
		
		GetMethod get1 = new GetMethod(collectionUri);
		int get1Result = executeMethod(get1, "adam", "test");
		assertEquals(200, get1Result);
		Document feedDoc = getResponseBodyAsDocument(get1);
		
		// test feed context
		
		Element l = getLink(feedDoc, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		// look for inline content
		List<Element> c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());
		
		Element l2 = getLink(feedDoc, "related"); assertNotNull(l);
		// look for inline content
		List<Element> c2 = getChildrenByTagNameNS(l2, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c2.size());
		
	}
	
	
	
	

	public void testExpansionInEntry() throws Exception {
		
		// create a test member to link to
		
		String entry = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member To Link To</atom:title>\n" +
			"</atom:entry>";
		PostMethod post = new PostMethod(TEST_COLLECTION_URL);
		setAtomRequestEntity(post, entry);
		int postResult = executeMethod(post, "adam", "test");
		assertEquals(201, postResult);
		String memberUri = post.getResponseHeader("Location").getValue();

		// create a collection with expansion configured
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='entry'>\n" +
			"			<atombeat:param name='match-rels' value='related http://purl.org/atombeat/rel/security-descriptor'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>" +
			"</atom:feed>";
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, feed);
		int putResult = executeMethod(put, "adam", "test");
		assertEquals(201, putResult);
		
		// create a member in the new collection
		
		String entry2 = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member Testing Link Expansion</atom:title>\n" +
			"	<atom:link rel='related' href='"+memberUri+"'/>\n" +
			"</atom:entry>";
		PostMethod post2 = new PostMethod(collectionUri);
		setAtomRequestEntity(post2, entry2);
		int postResult2 = executeMethod(post2, "adam", "test");
		assertEquals(201, postResult2);
		String memberUri2 = post2.getResponseHeader("Location").getValue();
		
		// retrieve new member, look for expansions
		
		GetMethod get2 = new GetMethod(memberUri2);
		int get2Result = executeMethod(get2, "adam", "test");
		assertEquals(200, get2Result);
		Document d = getResponseBodyAsDocument(get2);
		
		// test entry context
		
		// use "related" as test for expand to other content
		Element l = getLink(d, "related"); assertNotNull(l);
		// look for inline content
		List<Element> c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());

		l = getLink(d, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		// look for inline content
		c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());
		
		// create another member - check if pre-expanded links are properly filtered
		
		String anotherEntry = 
			"<atom:entry \n" +
			"	xmlns:ae='http://purl.org/atom/ext/'\n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member Testing Link Expansions are Removed</atom:title>\n" +
			"	<atom:link rel='related' href='"+memberUri+"'><ae:inline><atom:entry><atom:title>Spoof!</atom:title></atom:entry></ae:inline></atom:link>\n" +
			"</atom:entry>";
		PostMethod anotherPost = new PostMethod(collectionUri);
		setAtomRequestEntity(anotherPost, anotherEntry);
		int anotherPostResult = executeMethod(anotherPost, "adam", "test");
		assertEquals(201, anotherPostResult);
		
		Document d2 = getResponseBodyAsDocument(anotherPost);
		l = getLink(d2, "related"); assertNotNull(l);
		// look for inline content
		c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());

	}
	
	
	
	
	

	public void testExpansionInEntryInFeed() throws Exception {
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='entry-in-feed'>\n" +
			"			<atombeat:param name='match-rels' value='http://purl.org/atombeat/rel/security-descriptor'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>" +
			"</atom:feed>";
		
		// create the collection
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, feed);
		int putResult = executeMethod(put, "adam", "test");
		assertEquals(201, putResult);
		
		// create a member
		
		String entry = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member Testing Link Expansion</atom:title>\n" +
			"</atom:entry>";
		PostMethod post = new PostMethod(collectionUri);
		setAtomRequestEntity(post, entry);
		int postResult = executeMethod(post, "adam", "test");
		assertEquals(201, postResult);
		String memberUri = post.getResponseHeader("Location").getValue();
		
		// retrieve member
		
		GetMethod get2 = new GetMethod(memberUri);
		int get2Result = executeMethod(get2, "adam", "test");
		assertEquals(200, get2Result);
		Document d = getResponseBodyAsDocument(get2);
		
		// test entry context
		
		Element l = getLink(d, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		// look for inline content
		List<Element> c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(0, c.size());
		
		// list the collection
		
		GetMethod get1 = new GetMethod(collectionUri);
		int get1Result = executeMethod(get1, "adam", "test");
		assertEquals(200, get1Result);
		Document feedDoc = getResponseBodyAsDocument(get1);
		
		// test feed context
		
		l = getLink(feedDoc, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		// look for inline content
		c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(0, c.size());
		
		// test entry-in-feed context

		Element entryElm = getEntries(feedDoc).get(0);
		l = getLink(entryElm, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		// look for inline content
		c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());

	}
	
	
	public void testCircularExpansion() throws Exception {
		
		// create a collection with expansion configured
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='entry'>\n" +
			"			<atombeat:param name='match-rels' value='related self'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>" +
			"</atom:feed>";
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, feed);
		int putResult = executeMethod(put, "adam", "test");
		assertEquals(201, putResult);
		
		// create a first member
		
		String entry = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member 1</atom:title>\n" +
			"</atom:entry>";
		PostMethod post = new PostMethod(collectionUri);
		setAtomRequestEntity(post, entry);
		int postResult = executeMethod(post, "adam", "test");
		assertEquals(201, postResult);
		String memberUri = post.getResponseHeader("Location").getValue();

		// create a member in the new collection
		
		String entry2 = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member Testing Link Expansion</atom:title>\n" +
			"	<atom:link rel='related' href='"+memberUri+"'/>\n" +
			"</atom:entry>";
		PostMethod post2 = new PostMethod(collectionUri);
		setAtomRequestEntity(post2, entry2);
		int postResult2 = executeMethod(post2, "adam", "test");
		assertEquals(201, postResult2);
		String memberUri2 = post2.getResponseHeader("Location").getValue();
		
		// update first with link to second
		String entry1Updated = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member 1</atom:title>\n" +
			"	<atom:link rel='related' href='"+memberUri2+"'/>\n" +
			"</atom:entry>";
		PutMethod put2 = new PutMethod(memberUri);
		setAtomRequestEntity(put2, entry1Updated);
		int putResult2 = executeMethod(put2, "adam", "test");
		assertEquals(200, putResult2);
		
		// if this lot succeeds at all, then we've probably handled circular links ok, i.e., we haven't get stuch chasing our own tails
	}
	
	
	public void testExpansionWithQueryParams() throws Exception {
		
		// create a test member to link to
		
		String entry = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member To Link To</atom:title>\n" +
			"</atom:entry>";
		PostMethod post = new PostMethod(TEST_COLLECTION_URL);
		setAtomRequestEntity(post, entry);
		int postResult = executeMethod(post, "adam", "test");
		assertEquals(201, postResult);
		String memberUri = post.getResponseHeader("Location").getValue();

		// create a collection with expansion configured
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='entry'>\n" +
			"			<atombeat:param name='match-rels' value='related'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>" +
			"</atom:feed>";
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, feed);
		int putResult = executeMethod(put, "adam", "test");
		assertEquals(201, putResult);
		
		// create a member in the new collection
		
		String entry2 = 
			"<atom:entry \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member Testing Link Expansion</atom:title>\n" +
			"	<atom:link rel='related' href='"+memberUri+"?foo=bar&amp;baz=quux&amp;spong'/>\n" +
			"</atom:entry>";
		PostMethod post2 = new PostMethod(collectionUri);
		setAtomRequestEntity(post2, entry2);
		int postResult2 = executeMethod(post2, "adam", "test");
		assertEquals(201, postResult2);
		String memberUri2 = post2.getResponseHeader("Location").getValue();
		
		// retrieve new member, look for expansions
		
		GetMethod get2 = new GetMethod(memberUri2);
		int get2Result = executeMethod(get2, "adam", "test");
		assertEquals(200, get2Result);
		Document d = getResponseBodyAsDocument(get2);
		
		// test entry context
		
		// use "related" as test for expand to other content
		Element l = getLink(d, "related"); assertNotNull(l);
		// look for inline content
		List<Element> c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());

	}
	
	
	
	
}
