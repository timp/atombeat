/**
 * 
 */
package org.atombeat.it.plugin.paging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;


import org.apache.abdera.Abdera;
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
public class TestPagingPlugin extends TestCase {

	
	
	
	
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
			e.setTitle("entry in paged feed");
			ClientResponse r = adam.post(collectionUri, e);
			assertEquals(201, r.getStatus());
		}
		
		// retrieve a feed, expect no paging links
		ClientResponse r1 = adam.get(collectionUri);
		Document<Feed> d1 = r1.getDocument();
		Feed f1 = d1.getRoot();
		assertEquals(10, f1.getEntries().size());
		assertNull(f1.getLink("next"));
		assertNull(f1.getLink("previous"));
		assertNotNull(f1.getLink("first"));
		assertNotNull(f1.getLink("last"));
		// TODO finish this
	}
	
}