package org.atombeat.it.plugin.linkextensions;

import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.*;




public class TestLinkExtensionsPlugin_Allow extends TestCase {

	
	
	
	
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

	
	


	public void testAllowAttribute() throws IOException {
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing @allow Link Extensions</atom:title>\n" +
			"	<atombeat:config-link-extensions>\n" +
			"		<atombeat:extension-attribute \n" +
			"			name='allow' \n" +
			"			namespace='http://purl.org/atombeat/xmlns'>\n" +
			"			<atombeat:config context='feed'>\n" +
			"				<atombeat:param name='match-rels' value='self http://purl.org/atombeat/rel/security-descriptor'/>\n" +
			"			</atombeat:config>\n" +
			"			<atombeat:config context='entry'>\n" +
			"				<atombeat:param name='match-rels' value='*'/>\n" +
			"			</atombeat:config>\n" +
			"			<atombeat:config context='entry-in-feed'>\n" +
			"				<atombeat:param name='match-rels' value='edit'/>\n" +
			"			</atombeat:config>\n" +
			"		</atombeat:extension-attribute>\n" +
			"	</atombeat:config-link-extensions>" +
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
			"	<atom:title type='text'>Member Testing @allow Link Extensions</atom:title>\n" +
			"</atom:entry>";
		PostMethod post = new PostMethod(collectionUri);
		setAtomRequestEntity(post, entry);
		int postResult = executeMethod(post, "adam", "test");
		assertEquals(201, postResult);
		String memberUri = post.getResponseHeader("Location").getValue();
		
		// list the collection
		
		GetMethod get1 = new GetMethod(collectionUri);
		int get1Result = executeMethod(get1, "adam", "test");
		assertEquals(200, get1Result);
		Document feedDoc = getResponseBodyAsDocument(get1);
		
		// test feed context
		
		Element l = getLink(feedDoc, "self"); assertNotNull(l);
		String a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow");
		assertNotNull(a); assertFalse(a.equals(""));
		assertTrue(a.contains("GET"));
		assertTrue(a.contains("POST"));
		assertTrue(a.contains("PUT"));
		assertFalse(a.contains("DELETE")); // cannot delete collections

		l = getLink(feedDoc, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow");
		assertNotNull(a); assertFalse(a.equals(""));
		assertTrue(a.contains("GET"));
		assertTrue(a.contains("PUT"));
		assertFalse(a.contains("POST"));
		assertFalse(a.contains("DELETE")); // cannot delete security descriptor
		
		// test entry-in-feed context
		
		Element entryElm = getEntries(feedDoc).get(0);
		
		// should be only edit link with allow attribute

		l = getLink(entryElm, "edit"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow"); 
		assertNotNull(a); assertFalse(a.equals(""));
		assertTrue(a.contains("GET"));
		assertFalse(a.contains("POST"));
		assertTrue(a.contains("PUT"));
		assertTrue(a.contains("DELETE"));

		l = getLink(entryElm, "self"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow"); 
		assertEquals("", a);

		l = getLink(entryElm, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow"); 
		assertEquals("", a);
		
		// test entry context
		
		GetMethod get2 = new GetMethod(memberUri);
		int get2Result = executeMethod(get2, "adam", "test");
		assertEquals(200, get2Result);
		Document d = getResponseBodyAsDocument(get2);
		entryElm = d.getDocumentElement();

		// should be all links

		l = getLink(entryElm, "edit"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow"); assertNotNull(a);
		assertTrue(a.contains("GET"));
		assertFalse(a.contains("POST"));
		assertTrue(a.contains("PUT"));
		assertTrue(a.contains("DELETE"));

		l = getLink(entryElm, "self"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow"); 
		assertTrue(a.contains("GET"));
		assertFalse(a.contains("POST"));
		assertTrue(a.contains("PUT"));
		assertTrue(a.contains("DELETE"));

		l = getLink(entryElm, "http://purl.org/atombeat/rel/security-descriptor"); assertNotNull(l);
		a = l.getAttributeNS("http://purl.org/atombeat/xmlns", "allow"); 
		assertTrue(a.contains("GET"));
		assertFalse(a.contains("POST"));
		assertTrue(a.contains("PUT"));
		assertFalse(a.contains("DELETE"));
		
		// create another member - check if atombeat:allow attributes are 
		// stripped prior to member creation (if not, should cause errors)
		
		String anotherEntry = 
			"<atom:entry \n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'\n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'>\n" +
			"	<atom:title type='text'>Member Testing @allow Link Extensions</atom:title>\n" +
			"	<atom:link rel='foo' href='"+memberUri+"' atombeat:allow='GET'/>\n" +
			"</atom:entry>";
		PostMethod anotherPost = new PostMethod(collectionUri);
		setAtomRequestEntity(anotherPost, anotherEntry);
		int anotherPostResult = executeMethod(anotherPost, "adam", "test");
		assertEquals(201, anotherPostResult);
		
	}
	
	
	
}
