/**
 * 
 */
package org.atombeat.it.plugin.unzip;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;


import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
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
public class TestUnzipPlugin extends TestCase {

	
	
	
	
	private String collectionUri;



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
		
		// create a zip-aware collection
		Header[] headers = {};
		String content = 
			"<atom:feed " +
				"xmlns:atom=\"http://www.w3.org/2005/Atom\" " +
				"xmlns:atombeat=\"http://purl.org/atombeat/xmlns\" " +
				"atombeat:enable-unzip=\"true\">" +
				"<atom:title>Test Collection With Unzip</atom:title>" +
			"</atom:feed>";
		collectionUri = createTestCollection(CONTENT_URL, ADAM, PASSWORD, headers, content);
		
	}
	
	


	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	
	public void testCreateZipMedia() throws URISyntaxException {
		
		// create a zip media resource
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("test.zip");
		RequestOptions request = new RequestOptions();
		request.setContentType("application/zip");
		ClientResponse response = adam.post(collectionUri, content, request);

		// test the initial response - expect standard response for creating media resource
		assertEquals(201, response.getStatus());
		Document<Entry> d = response.getDocument();
		Entry e = d.getRoot();
		String parenturi = e.getEditLinkResolvedHref().toString();
		assertNotNull(e.getEditMediaLink());
		String parentmediauri = e.getEditMediaLinkResolvedHref().toString();
		assertNotNull(e.getContentElement());
		
		// look for "http://purl.org/atombeat/rel/package-members" link to zip contents
		assertEquals(1, e.getLinks("http://purl.org/atombeat/rel/package-members").size());
		Link packageMembersLink = e.getLinks("http://purl.org/atombeat/rel/package-members").get(0);
		String packageMembersUri = packageMembersLink.getResolvedHref().toString();
		
		// retrieve package members feed, check members - expect 3
		response.release();
		response = adam.get(packageMembersUri);
		assertEquals(200, response.getStatus());
		Document<Feed> fd = response.getDocument();
		Feed f = fd.getRoot();
		assertEquals(3, f.getEntries().size());
		
		// retrieve media content from members - expect ok
		for (Entry ml : f.getEntries()) {
			
			// check properties of the media-link entry
			String src = ml.getContentSrc().toString();
			String emu = ml.getEditMediaLink().getResolvedHref().toString(); 
			String type = ml.getContentMimeType().toString();
			assertEquals(src, emu);
			assertEquals(1, ml.getLinks("http://purl.org/atombeat/rel/member-of-package").size());
			Link up = ml.getLink("http://purl.org/atombeat/rel/member-of-package");
			assertEquals(parenturi, up.getResolvedHref().toString());
			assertEquals(1, ml.getLinks("http://purl.org/atombeat/rel/member-of-package-media").size());
			Link upmedia = ml.getLink("http://purl.org/atombeat/rel/member-of-package-media");
			assertEquals(parentmediauri, upmedia.getResolvedHref().toString());
			assertTrue(ml.getEditMediaLink().getLength() > 0);
			assertFalse(ml.getEditMediaLink().getAttributeValue("hash").contains("TODO"));
			
			// check can retrieve media content
			response.release();
			response = adam.get(src);
			assertEquals(200, response.getStatus());
			assertTrue(response.getContentType().toString().startsWith(type));
			
			// check cannot update media content
			RequestOptions options = new RequestOptions();
			options.setContentType("text/plain");
			response.release();
			response = adam.put(src, new ByteArrayInputStream("foobar".getBytes()), options);
			assertEquals(405, response.getStatus());
			
			// check can retrieve member
			response.release();
			response = adam.get(ml.getEditLinkResolvedHref().toString());
			assertEquals(200, response.getStatus());
			assertTrue(response.getContentType().toString().startsWith("application/atom+xml"));
			Document<Entry> mld = response.getDocument();
			ml = mld.getRoot();
			
			// check can update member
			ml.setTitle("changed");
			response.release();
			response = adam.put(ml.getEditLinkResolvedHref().toString(), ml);
			assertEquals(200, response.getStatus());
			mld = response.getDocument();
			ml = mld.getRoot();
			assertEquals(1, ml.getLinks("http://purl.org/atombeat/rel/member-of-package").size());
			up = ml.getLink("http://purl.org/atombeat/rel/member-of-package");
			assertEquals(parenturi, up.getResolvedHref().toString());
			assertEquals(1, ml.getLinks("http://purl.org/atombeat/rel/member-of-package-media").size());
			upmedia = ml.getLink("http://purl.org/atombeat/rel/member-of-package-media");
			assertEquals(parentmediauri, upmedia.getResolvedHref().toString());
			
		}
		
		// try deleting parent member, should cascade
		response.release();
		response = adam.delete(parenturi);
		assertTrue(200 <= response.getStatus() && response.getStatus() < 300);
		response.release();
		response = adam.get(parenturi); assertEquals(404, response.getStatus()); response.release();
		response = adam.get(parentmediauri); assertEquals(404, response.getStatus()); response.release();
		response = adam.get(packageMembersUri); assertEquals(404, response.getStatus()); response.release();
		for (Entry ml : f.getEntries()) {
			response = adam.get(ml.getEditLinkResolvedHref().toString()); assertEquals(404, response.getStatus()); response.release();
			response = adam.get(ml.getEditMediaLinkResolvedHref().toString()); assertEquals(404, response.getStatus()); response.release();
		}
		
	}
	
}