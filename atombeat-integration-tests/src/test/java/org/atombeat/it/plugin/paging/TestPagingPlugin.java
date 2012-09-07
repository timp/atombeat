/**
 * 
 */
package org.atombeat.it.plugin.paging;

import java.net.URISyntaxException;

import javax.xml.namespace.QName;


import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.CONTENT_URL;
import static org.atombeat.it.AtomTestUtils.SERVICE_URL;
import static org.atombeat.it.AtomTestUtils.ADAM;
import static org.atombeat.it.AtomTestUtils.PASSWORD;
import static org.atombeat.it.AtomTestUtils.REALM;
import static org.atombeat.it.AtomTestUtils.SCHEME_BASIC;
import static org.atombeat.it.AtomTestUtils.createTestCollection;
import static org.atombeat.it.AtomTestUtils.executeMethod;





/**
 * @author aliman
 *
 */
public class TestPagingPlugin extends TestCase {

	
	
	
	
	private String collectionUri;
	public static final String OPENSEARCHNS = "http://a9.com/-/spec/opensearch/1.1/";
	public static final QName TOTALRESULTS = new QName(OPENSEARCHNS, "totalResults");
	public static final QName STARTINDEX = new QName(OPENSEARCHNS, "startIndex");
	public static final QName ITEMSPERPAGE = new QName(OPENSEARCHNS, "itemsPerPage");
	public static final QName TLINK = new QName("http://purl.org/atombeat/xmlns", "tlink");
	public static final String REL_PAGE = "http://purl.org/atombeat/rel/page";



	protected void setUp() throws Exception {
		super.setUp();

		// this guarantees workspace acl is installed
		String installUrl = SERVICE_URL + "admin/setup-for-test.xql";
		GetMethod method = new GetMethod(installUrl);
		int result = executeMethod(method, ADAM, PASSWORD);
		method.releaseConnection();
		if (result != 200) {
			throw new RuntimeException("setup failed: "+result);
		}
		
		// create a paging collection
		Header[] headers = {};
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-paging=\"true\">" +
				"<atom:title>Test Collection With Paging</atom:title>" +
				"<atombeat:config-paging default-page-size=\"20\" max-page-size=\"50\"/>" +
			"</atom:feed>";
		collectionUri = createTestCollection(CONTENT_URL, ADAM, PASSWORD, headers, content);
		
	}
	
	


	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	
	public void testPaging() throws URISyntaxException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));

		// add 10 entries
		for (int i=0; i<10; i++) {
			Entry e = Abdera.getInstance().getFactory().newEntry();
			e.setTitle("entry "+(i+1)+" in paged feed");
			ClientResponse re = adam.post(collectionUri, e);
			assertEquals(201, re.getStatus());
			re.release();
		}
		
		Element totalResults, startIndex, itemsPerPage;
		
		// retrieve a feed, check paging links and opensearch elements
		ClientResponse r1 = adam.get(collectionUri);
		assertEquals(200, r1.getStatus());
		Document<Feed> d1 = r1.getDocument();
		Feed f1 = d1.getRoot();
		assertEquals(10, f1.getEntries().size());
		assertNull(f1.getLink("next"));
		assertNull(f1.getLink("previous"));
		assertNotNull(f1.getLink("first"));
		assertNotNull(f1.getLink("last"));
		totalResults = f1.getExtension(TOTALRESULTS);
		assertEquals(10, Integer.parseInt(totalResults.getText()));
		startIndex = f1.getExtension(STARTINDEX);
		assertEquals(1, Integer.parseInt(startIndex.getText()));
		itemsPerPage = f1.getExtension(ITEMSPERPAGE);
		assertEquals(20, Integer.parseInt(itemsPerPage.getText()));
		r1.release();
		
		// retrieve first and last links, expect same feed with 10 entries
		String first, last; ClientResponse r; Document<Feed> d; Feed f;
		first = f1.getLink("first").getResolvedHref().toString();
		r = adam.get(first);
		assertEquals(200, r.getStatus());
		d = r.getDocument();
		f = d.getRoot();
		assertEquals(10, f.getEntries().size());
		last = f1.getLink("last").getResolvedHref().toString();
		r.release();
		r = adam.get(last);
		assertEquals(200, r.getStatus());
		d = r.getDocument();
		f = d.getRoot();
		assertEquals(10, f.getEntries().size());
		r.release();
		
		// add 20 more entries
		for (int i=0; i<20; i++) {
			Entry e = Abdera.getInstance().getFactory().newEntry();
			e.setTitle("entry "+(10+i+1)+" in paged feed");
			ClientResponse re = adam.post(collectionUri, e);
			assertEquals(201, re.getStatus());
			re.release();
		}
		
		// retrieve a feed, check paging links
		ClientResponse r2 = adam.get(collectionUri);
		assertEquals(200, r2.getStatus());
		Document<Feed> d2 = r2.getDocument();
		Feed f2 = d2.getRoot();
		assertEquals(20, f2.getEntries().size());
		assertNotNull(f2.getLink("next"));
		assertNull(f2.getLink("previous"));
		assertNotNull(f2.getLink("first"));
		assertNotNull(f2.getLink("last"));
		totalResults = f2.getExtension(TOTALRESULTS);
		assertEquals(30, Integer.parseInt(totalResults.getText()));
		startIndex = f2.getExtension(STARTINDEX);
		assertEquals(1, Integer.parseInt(startIndex.getText()));
		itemsPerPage = f2.getExtension(ITEMSPERPAGE);
		assertEquals(20, Integer.parseInt(itemsPerPage.getText()));
		r2.release();
		
		// retrieve next link, expect feed with 10 entries and previous link
		ClientResponse r3 = adam.get(f2.getLink("next").getResolvedHref().toString());
		assertEquals(200, r3.getStatus());
		Document<Feed> d3 = r3.getDocument();
		Feed f3 = d3.getRoot();
		assertEquals(10, f3.getEntries().size());
		assertNull(f3.getLink("next"));
		assertNotNull(f3.getLink("previous"));
		assertNotNull(f3.getLink("first"));
		assertNotNull(f3.getLink("last"));
		totalResults = f3.getExtension(TOTALRESULTS);
		assertEquals(30, Integer.parseInt(totalResults.getText()));
		startIndex = f3.getExtension(STARTINDEX);
		assertEquals(21, Integer.parseInt(startIndex.getText()));
		itemsPerPage = f3.getExtension(ITEMSPERPAGE);
		assertEquals(20, Integer.parseInt(itemsPerPage.getText()));
		r3.release();
		
		// retrieve previous link, expect feed with 20 entries
		ClientResponse r4 = adam.get(f3.getLink("previous").getResolvedHref().toString());
		assertEquals(200, r4.getStatus());
		Document<Feed> d4 = r4.getDocument();
		Feed f4 = d4.getRoot();
		assertEquals(20, f4.getEntries().size());
		assertNotNull(f4.getLink("next"));
		assertNull(f4.getLink("previous"));
		assertNotNull(f4.getLink("first"));
		assertNotNull(f4.getLink("last"));
		totalResults = f4.getExtension(TOTALRESULTS);
		assertEquals(30, Integer.parseInt(totalResults.getText()));
		startIndex = f4.getExtension(STARTINDEX);
		assertEquals(1, Integer.parseInt(startIndex.getText()));
		itemsPerPage = f4.getExtension(ITEMSPERPAGE);
		assertEquals(20, Integer.parseInt(itemsPerPage.getText()));
		r4.release();
		
	}
	
	
	
	public void testPagingTemplateLink() throws URISyntaxException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));

		// add 50 entries
		for (int i=0; i<50; i++) {
			Entry e = Abdera.getInstance().getFactory().newEntry();
			e.setTitle("entry "+(i+1)+" in paged feed");
			ClientResponse re = adam.post(collectionUri, e);
			assertEquals(201, re.getStatus());
			re.release();
		}

		// retrieve a feed
		ClientResponse r1 = adam.get(collectionUri);
		assertEquals(200, r1.getStatus());
		Document<Feed> d1 = r1.getDocument();
		Feed f1 = d1.getRoot();
		assertEquals(20, f1.getEntries().size());
		Element tlink = f1.getExtension(TLINK);
		assertNotNull(tlink);
		assertEquals(REL_PAGE, tlink.getAttributeValue("rel"));
		String template = tlink.getAttributeValue("href");
		assertTrue(template.contains("{count}"));
		assertTrue(template.contains("{startPage}"));
		r1.release();
		
		// see if we can get a bigger feed
		String uri = template.replace("{count}", "40").replace("{startPage}", "1");
		ClientResponse r2 = adam.get(uri);
		assertEquals(200, r2.getStatus());
		Document<Feed> d2 = r2.getDocument();
		Feed f2 = d2.getRoot();
		assertEquals(40, f2.getEntries().size());
		r2.release();
		
		// check we can't override the max page size
		uri = template.replace("{count}", "10000").replace("{startPage}", "1");
		ClientResponse r3 = adam.get(uri);
		assertEquals(200, r3.getStatus());
		Document<Feed> d3 = r3.getDocument();
		Feed f3 = d3.getRoot();
		assertEquals(50, f3.getEntries().size());
		r3.release();
		
	}
	
	
	
}
