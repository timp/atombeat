/**
 * 
 */
package org.atombeat.it.plugin.conneg;

import java.net.URISyntaxException;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.SERVICE_URL;
import static org.atombeat.it.AtomTestUtils.TEST_COLLECTION_URL;
import static org.atombeat.it.AtomTestUtils.ADAM;
import static org.atombeat.it.AtomTestUtils.PASSWORD;
import static org.atombeat.it.AtomTestUtils.REALM;
import static org.atombeat.it.AtomTestUtils.SCHEME_BASIC;
import static org.atombeat.it.AtomTestUtils.executeMethod;





/**
 * @author aliman
 *
 */
public class TestConnegPlugin extends TestCase {

	
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		// this guarantees there should be at least one collection
		String installUrl = SERVICE_URL + "admin/setup-for-test.xql";
		GetMethod method = new GetMethod(installUrl);
		int result = executeMethod(method, ADAM, PASSWORD);
		method.releaseConnection();
		if (result != 200) {
			throw new RuntimeException("setup failed: "+result);
		}
		
	}
	
	


	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	
	public void testAlternateLinksInEntry() throws URISyntaxException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		// create a new member of the test collection
		Entry e = Abdera.getInstance().getFactory().newEntry();
		e.setTitle("testing alternate links are present");
		e.setSummary("this is a test to see if alternate links are added to the entry");
		ClientResponse r = adam.post(TEST_COLLECTION_URL, e);
		assertEquals(201, r.getStatus());
		Document<Entry> d = r.getDocument();
		Entry f = d.getRoot();
		
		// verify alternate links are present
		
		IRI htmliri = f.getAlternateLinkResolvedHref("text/html", null);
		assertNotNull(htmliri);
		IRI jsoniri = f.getAlternateLinkResolvedHref("application/json", null);
		assertNotNull(jsoniri);
		
		r.release();
		
		// verify alternate links can be dereferenced
		
		GetMethod htmlget = new GetMethod(htmliri.toASCIIString());
		int htmlres = executeMethod(htmlget, ADAM, PASSWORD);
		assertEquals(200, htmlres);
		assertTrue(htmlget.getResponseHeader("Content-Type").getValue().startsWith("text/html"));
		htmlget.releaseConnection();

		GetMethod jsonget = new GetMethod(jsoniri.toASCIIString());
		int jsonres = executeMethod(jsonget, ADAM, PASSWORD);
		assertEquals(200, jsonres);
		assertTrue(jsonget.getResponseHeader("Content-Type").getValue().startsWith("application/json"));
		jsonget.releaseConnection();

	}
	



	public void testAlternateLinksInFeed() throws URISyntaxException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		// create a new member of the test collection, just to make life a bit more interesting
		
		Entry e = Abdera.getInstance().getFactory().newEntry();
		e.setTitle("testing alternate links are present");
		e.setSummary("this is a test to see if alternate links are added to the entry");
		ClientResponse r = adam.post(TEST_COLLECTION_URL, e);
		assertEquals(201, r.getStatus());
		r.release();
		
		// list the collection
		
		ClientResponse s = adam.get(TEST_COLLECTION_URL);
		Document<Feed> d = s.getDocument();
		Feed f = d.getRoot();
		
		// verify alternate links are present
		
		IRI htmliri = f.getAlternateLinkResolvedHref("text/html", null);
		assertNotNull(htmliri);
		IRI jsoniri = f.getAlternateLinkResolvedHref("application/json", null);
		assertNotNull(jsoniri);
		
		r.release();
		
		// verify alternate links can be dereferenced
		
		GetMethod htmlget = new GetMethod(htmliri.toASCIIString());
		int htmlres = executeMethod(htmlget, ADAM, PASSWORD);
		assertEquals(200, htmlres);
		assertTrue(htmlget.getResponseHeader("Content-Type").getValue().startsWith("text/html"));
		htmlget.releaseConnection();

		GetMethod jsonget = new GetMethod(jsoniri.toASCIIString());
		int jsonres = executeMethod(jsonget, ADAM, PASSWORD);
		assertEquals(200, jsonres);
		assertTrue(jsonget.getResponseHeader("Content-Type").getValue().startsWith("application/json"));
		jsonget.releaseConnection();

	}
	



}
