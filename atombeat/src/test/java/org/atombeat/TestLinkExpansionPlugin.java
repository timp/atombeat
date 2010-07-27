package org.atombeat;

import java.io.IOException;
import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;
import static org.atombeat.AtomTestUtils.*;




public class TestLinkExpansionPlugin extends TestCase {


	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		String installUrl = BASE_URI + "admin/setup-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method, "adam", "test");
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}
		
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	

	public void testExpansionInFeed() throws IOException {
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='feed'>\n" +
			"			<atombeat:param name='match-rels' value='http://purl.org/atombeat/rel/security-descriptor'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>" +
			"</atom:feed>";
		
		// create the collection
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
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
		
	}
	
	
	
	

	public void testExpansionInEntry() throws IOException {
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Link Expansion</atom:title>\n" +
			"	<atombeat:config-link-expansion>\n" +
			"		<atombeat:config context='entry'>\n" +
			"			<atombeat:param name='match-rels' value='self http://purl.org/atombeat/rel/security-descriptor'/>\n" +
			"		</atombeat:config>\n" +
			"	</atombeat:config-link-expansion>" +
			"</atom:feed>";
		
		// create the collection
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
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
		assertEquals(1, c.size());
		
		// use "self" as test for expand to other content
		l = getLink(d, "self"); assertNotNull(l);
		// look for inline content
		c = getChildrenByTagNameNS(l, "http://purl.org/atom/ext/", "inline");
		assertEquals(1, c.size());
		
	}
	
	
	
	
	

	public void testExpansionInEntryInFeed() throws IOException {
		
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
		
		String collectionUri = CONTENT_URI + Double.toString(Math.random());
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
	
	
	
	
}
