/**
 * 
 */
package org.atombeat.it.plugin.conneg;

import java.io.IOException;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	
	
	
	public void testConneg() throws URISyntaxException {
		
		String accept, expect, actual;
		
		String[][] tests = {
				
				// if no accept header, expect atom
				{ null, "application/atom+xml" }, 
				
				// if you ask for atom, that's what you get
				{ "application/atom+xml", "application/atom+xml" },	
				
				// if you ask for html, that's what you get
				{ "text/html", "text/html" },	
				
				// if you ask for json, that's what you get
				{ "application/json", "application/json" },
				
				// if you ask for anything, you'll get html (help browsers like IE that don't know what they want)
				{ "*/*", "text/html" },	
				
				// expect html is slightly preferred over atom
				{ "application/atom+xml, text/html", "text/html" },
				
				// override slight server preference for html with strong client preference for atom
				{ "application/atom+xml;q=1.0, text/html;q=0.1", "application/atom+xml" }, 
				
				// what happens if two variants end up with the same score? first listed variant in server variant configuration wins
				{ "application/atom+xml;q=0.95, text/html;q=0.8", "text/html" }, 
				
				// make sure unsupported media types don't confuse the server
				{ "text/html; q=1.0, text/*; q=0.8, image/gif; q=0.6, image/jpeg; q=0.6, image/*; q=0.5, */*; q=0.1", "text/html" }, 
				
				// make sure additional mediatype parameters don't confuse the server
				// also, check that the quality value for */* is fiddled if no quality values are specified at all
				// (these are abdera 1.1.1 client's default accept header)
				{ "application/atom+xml;type=entry, application/atom+xml;type=feed, application/atom+xml, application/atomsvc+xml, application/atomcat+xml, application/xml, text/xml, */*" , "application/atom+xml"}, 

				// check that the quality value for text/* is fiddled if no quality values are specified at all 
				{ "application/atom+xml, text/*" , "application/atom+xml"} ,
				
				// firefox 3.6.13
				{ "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" , "text/html" }

		};
		
		for (String[] test : tests) {
			accept = test[0];
			expect = test[1];
			actual = doGetCollectionTest(accept); assertEquals(expect, actual);
			actual = doGetMemberTest(accept); assertEquals(expect, actual);
		}
		
	}
	
	
	private String doGetMemberTest(String accept) throws URISyntaxException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		// create a new member of the test collection
		
		Entry e = Abdera.getInstance().getFactory().newEntry();
		e.setTitle("conneg test");
		ClientResponse r = adam.post(TEST_COLLECTION_URL, e);
		assertEquals(201, r.getStatus());
		Document<Entry> d = r.getDocument();
		Entry f = d.getRoot();
		String u = f.getEditLinkResolvedHref().toASCIIString();
		r.release();
		
		// use http client library rather than abdera to make sure we control accept header
		
		GetMethod g = new GetMethod(u);
		if (accept != null)
			g.setRequestHeader("Accept", accept);
		int res = executeMethod(g, ADAM, PASSWORD);
		assertEquals(200, res);
		assertNotNull(g.getResponseHeader("Vary"));
		String contentType = g.getResponseHeader("Content-Type").getValue();
		g.releaseConnection();

		String mediaType = contentType.split(";")[0];
		return mediaType;

	}
	
	
	
	private String doGetCollectionTest(String accept) {
		
		// use http client library rather than abdera to make sure we control accept header
		
		GetMethod g = new GetMethod(TEST_COLLECTION_URL);
		if (accept != null)
			g.setRequestHeader("Accept", accept);
		int res = executeMethod(g, ADAM, PASSWORD);
		assertEquals(200, res);
		assertNotNull(g.getResponseHeader("Vary"));
		String contentType = g.getResponseHeader("Content-Type").getValue();
		g.releaseConnection();
		
		String mediaType = contentType.split(";")[0];
		return mediaType;
		
	}
	

	
	
	public void testNoAcceptableRepresentation() {
		
		// use http client library rather than abdera to make sure we control accept header
		
		GetMethod g = new GetMethod(TEST_COLLECTION_URL);
		g.setRequestHeader("Accept", "foo/bar");
		int res = executeMethod(g, ADAM, PASSWORD);
		assertEquals(406, res);
		g.releaseConnection();
		
	}
	

	
	
	public void testAlternateLinksDontProliferate() throws URISyntaxException {
		
		// we want to verify that alternate links are stripped on PUT or POST
		// so you don't end up with a proliferation after multiple updates
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		// first create a member
		
		Entry e = Abdera.getInstance().getFactory().newEntry();
		e.setTitle("testing alternate links are stripped");
		e.setSummary("this is a test to see if alternate links are stripped from request data");
		e.addLink("http://example.org/foobar", "alternate", "text/html", "i should be stripped", "foolang", -1);
		
		ClientResponse r = adam.post(TEST_COLLECTION_URL, e);
		assertEquals(201, r.getStatus());
		Document<Entry> d = r.getDocument();
		Entry f = d.getRoot();
		String l = f.getEditLinkResolvedHref().toASCIIString();

		// verify alternate links are present and correct
		IRI htmliri = f.getAlternateLinkResolvedHref("text/html", null);
		assertNotNull(htmliri);
		assertFalse(htmliri.toASCIIString().equals("http://example.org/foobar"));

		// store initial number of alternate links to test against later
		int n = f.getLinks("alternate").size();
		
		r.release();

		// now update member and look for proliferation
		ClientResponse s = adam.put(l, f);
		assertEquals(200, s.getStatus());
		d = s.getDocument();
		Entry g = d.getRoot();
		assertEquals(n, g.getLinks("alternate").size());
		
		s.release();

	}
	
	
	
	public void testJsonRepresentation() throws URISyntaxException, JSONException, IOException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		// create a new member of the test collection
		
		Entry e = Abdera.getInstance().getFactory().newEntry();
		e.setTitle("testing json representstation");
		e.setSummary("this is a test to check that element local names are used in json representation");
		ClientResponse r = adam.post(TEST_COLLECTION_URL, e);
		assertEquals(201, r.getStatus());
		Document<Entry> d = r.getDocument();
		Entry f = d.getRoot();
		
		IRI jsoniri = f.getAlternateLinkResolvedHref("application/json", null);
		assertNotNull(jsoniri);
		
		r.release();
		
		// get the json
		
		GetMethod jsonget = new GetMethod(jsoniri.toASCIIString());
		int jsonres = executeMethod(jsonget, ADAM, PASSWORD);
		assertEquals(200, jsonres);
		assertTrue(jsonget.getResponseHeader("Content-Type").getValue().startsWith("application/json"));

		JSONObject jo = new JSONObject(jsonget.getResponseBodyAsString());
		assertNotNull(jo);
		assertNotNull(jo.get("id"));
		assertNotNull(jo.get("published"));
		assertNotNull(jo.get("updated"));
		JSONArray links = jo.getJSONArray("link");
		assertNotNull(links);
		assertTrue(links.length()>0);
		for (int i=0; i<links.length(); i++) {
			JSONObject link = links.getJSONObject(i);
			assertNotNull(link.get("@rel"));
			assertNotNull(link.get("@href"));
		}
			
		jsonget.releaseConnection();

	}



}
