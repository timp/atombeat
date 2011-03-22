/**
 * 
 */
package org.atombeat.it.plugin.unzip;

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

		// test the initial response
		assertEquals(201, response.getStatus());
		Document<Entry> d = response.getDocument();
		Entry e = d.getRoot();
		assertNotNull(e.getEditMediaLink());
		assertNotNull(e.getContentElement());
		
		// look for "down" link to zip contents
		assertEquals(1, e.getLinks("down").size());
		Link down = e.getLinks("down").get(0);
		String downuri = down.getResolvedHref().toString();
		
		// retrieve down feed, check members - expect 3
		response.release();
		response = adam.get(downuri);
		assertEquals(200, response.getStatus());
		Document<Feed> fd = response.getDocument();
		Feed f = fd.getRoot();
		assertEquals(3, f.getEntries().size());
		
		// retrieve media content from members - expect ok
		for (Entry ml : f.getEntries()) {
			String src = ml.getContentSrc().toString();
			String emu = ml.getEditMediaLink().getResolvedHref().toString(); 
			String type = ml.getContentMimeType().toString();
			assertEquals(src, emu);
			response.release();
			response = adam.get(src);
			assertEquals(200, response.getStatus());
			assertEquals(type, response.getContentType().toString());
		}
		
		// TODO try updating a member - ok
		
		// TODO try updating media - not supported
	}
	
}