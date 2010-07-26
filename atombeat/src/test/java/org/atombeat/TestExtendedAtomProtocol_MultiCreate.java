package org.atombeat;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import static org.atombeat.AtomTestUtils.*;

import junit.framework.TestCase;





public class TestExtendedAtomProtocol_MultiCreate extends TestCase {


	
	
	
	private static final String USER = "adam"; // should be allowed all operations
	private static final String PASS = "test";
	

	
	private DOMImplementationRegistry domImplRegistry;
	private DOMImplementationLS domImplLs;
	private LSSerializer lsWriter;

	

	
	private static Integer executeMethod(HttpMethod method) {
		
		return AtomTestUtils.executeMethod(method, USER, PASS);

	}

	
	
	private boolean setupForTest = false;
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		if ( !setupForTest ) {
			
			String setupUrl = BASE_URI + "admin/setup-for-test.xql";
			
			PostMethod method = new PostMethod(setupUrl);
			
			int result = executeMethod(method);
			
			if (result != 200) {
				throw new RuntimeException("setup failed: "+result);
			}

			domImplRegistry = DOMImplementationRegistry.newInstance();
			domImplLs = (DOMImplementationLS)domImplRegistry.getDOMImplementation("LS");
			lsWriter = domImplLs.createLSSerializer();
			lsWriter.getDomConfig().setParameter("xml-declaration", false);
			setupForTest = true;

		}
	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	
	
	public void testMultiCreateMember() {
		
		String collectionUri;
		GetMethod get1, get2;
		PostMethod post;
		int get1Result, get2Result, postResult;
		Document d; List<Element> l; 
		
		// create a collection to run test against
		
		collectionUri = createTestCollection(CONTENT_URI, USER, PASS);
		
		// do initial get on collection uri, expect to find no members
		
		get1 = new GetMethod(collectionUri);
		get1Result = executeMethod(get1);

		assertEquals(200, get1Result);
		d = getResponseBodyAsDocument(get1);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(0, l.size());
		
		// do post on collection uri with feed doc containing entries, expect to 
		// succeed and return feed doc with created entries
		
		post = new PostMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:entry>" +
					"<atom:title>Test Member</atom:title>" +
					"<atom:summary>This is a summary.</atom:summary>" +
				"</atom:entry>" +
				"<atom:entry>" +
					"<atom:title>Another Test Member</atom:title>" +
					"<atom:summary>This is another summary.</atom:summary>" +
				"</atom:entry>" +
			"</atom:feed>";
		setAtomRequestEntity(post, content);
		postResult = executeMethod(post); 
		assertEquals(200, postResult);
		d = getResponseBodyAsDocument(post);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, l.size());
		
		// do a second get on the collection uri, expect to find 2 members
		
		get2 = new GetMethod(collectionUri);
		get2Result = executeMethod(get2);
		assertEquals(200, get2Result);
		d = getResponseBodyAsDocument(get2);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, l.size());

	}

	
	
	public void testMultiCreateMedia() {
		
		String col1Uri, col2Uri;
		GetMethod get1, get2, get3;
		PostMethod post1, post2, post3;
		int get1Result, get2Result, get3Result, post1Result, post2Result, post3Result;
		Document d; List<Element> l;
		
		// set up two test collections

		col1Uri = createTestCollection(CONTENT_URI, USER, PASS);
		col2Uri = createTestCollection(CONTENT_URI, USER, PASS);
		
		// set up first collection with some media 

		post1 = new PostMethod(col1Uri);
		setTextPlainRequestEntity(post1, "This is a test.");
		post1Result = executeMethod(post1);
		assertEquals(201, post1Result);

		post2 = new PostMethod(col1Uri);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(post2, content, contentType);
		post2Result = executeMethod(post2);
		assertEquals(201, post2Result);
		
		// check second collection is empty
		
		get1 = new GetMethod(col2Uri);
		get1Result = executeMethod(get1);
		assertEquals(200, get1Result);
		d = getResponseBodyAsDocument(get1);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(0, l.size());
		
		// get feed from first collection
		
		get2 = new GetMethod(col1Uri);
		get2Result = executeMethod(get2);
		assertEquals(200, get2Result);
		d = getResponseBodyAsDocument(get2);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, l.size());
		
		// post feed from first collection to second collection 
		
		post3 = new PostMethod(col2Uri);

		String str = lsWriter.writeToString(d);
		setAtomRequestEntity(post3, str);
		post3Result = executeMethod(post3);
		assertEquals(200, post3Result);
		d = getResponseBodyAsDocument(post3);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, l.size());
		
		// get second collection to see change
		
		get3 = new GetMethod(col2Uri);
		get3Result = executeMethod(get3);
		assertEquals(200, get3Result);
		d = getResponseBodyAsDocument(get3);
		l = getChildrenByTagNameNS(d, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, l.size());
		
		// look for edit-media links, check same as content @src
		
		for (Element e : l) {
			String editMediaLocation = getEditMediaLocation(e);
			assertNotNull(editMediaLocation);
			String contentSrc = getContent(e).getAttribute("src");
			assertNotNull(contentSrc);
			assertEquals(editMediaLocation, contentSrc);
		}

	}

	
	
}



