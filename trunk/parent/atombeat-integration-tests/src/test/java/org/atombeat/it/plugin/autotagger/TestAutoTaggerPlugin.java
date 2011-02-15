package org.atombeat.it.plugin.autotagger;

import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.*;




public class TestAutoTaggerPlugin extends TestCase {


	
	
	
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

	
	

	public void testFixedExclusiveTaggers() throws Exception {
		
		// fixed exclusive taggers will ensure all within scope have the given category, and no other from the same scheme
		
		String feed = 
			"<atom:feed \n" +
			"	xmlns:atom='http://www.w3.org/2005/Atom'\n" +
			"	xmlns:atombeat='http://purl.org/atombeat/xmlns'>\n" +
			"	<atom:title type='text'>Collection Testing Automatic Categorisation</atom:title>\n" +
			"	<atombeat:config-taggers>\n" +
			"		<atombeat:tagger type='fixed-exclusive' scope='member' scheme='http://example.org/scheme' term='foo' label='Foo'/>\n" +
			"		<atombeat:tagger type='fixed-exclusive' scope='media-link' scheme='http://example.org/scheme' term='bar' label='Bar'/>\n" +
			"	</atombeat:config-taggers>" +
			"</atom:feed>";
		
		// create the collection
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod put = new PutMethod(collectionUri);
		setAtomRequestEntity(put, feed);
		int putResult = executeMethod(put, "adam", "test");
		assertEquals(201, putResult);
		
		// create a member
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		Factory factory = Abdera.getInstance().getFactory();
		
		Entry e1 = factory.newEntry();
		e1.setTitle("test 1");
		RequestOptions request = new RequestOptions();
		ClientResponse response = adam.post(collectionUri, e1, request);
		assertEquals(201, response.getStatus());
		org.apache.abdera.model.Document<Entry> d = response.getDocument();
		e1 = d.getRoot();
		List<Category> cats = e1.getCategories("http://example.org/scheme");
		assertEquals(1, cats.size());
		assertEquals("foo", cats.get(0).getTerm());
		assertEquals("Foo", cats.get(0).getLabel());
		
		// try to remove fixed and add other
		
		Entry e2 = factory.newEntry();
		e2.setTitle("test 2");
		e2.addCategory("http://example.org/scheme", "spong", "Spong");
		request = new RequestOptions();
		response = adam.put(e1.getEditLinkResolvedHref().toASCIIString(), e2, request);
		assertEquals(200, response.getStatus());
		d = response.getDocument();
		e2 = d.getRoot();
		cats = e2.getCategories("http://example.org/scheme");
		assertEquals(1, cats.size());
		assertEquals("foo", cats.get(0).getTerm());
		assertEquals("Foo", cats.get(0).getLabel());
		response.release();
		
		// create a media resource
		
		PostMethod method = new PostMethod(collectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method);
		assertEquals(201, result);
		String location = method.getResponseHeader("Location").getValue();
		
		response = adam.get(location);
		d = response.getDocument();
		Entry e3 = d.getRoot();
		cats = e3.getCategories("http://example.org/scheme");
		assertEquals(1, cats.size());
		assertEquals("bar", cats.get(0).getTerm());
		assertEquals("Bar", cats.get(0).getLabel());

		// try to remove fixed and add other
		
		Entry e4 = factory.newEntry();
		e4.setTitle("test 4");
		e4.addCategory("http://example.org/scheme", "spong", "Spong");
		request = new RequestOptions();
		response = adam.put(e3.getEditLinkResolvedHref().toASCIIString(), e4, request);
		assertEquals(200, response.getStatus());
		d = response.getDocument();
		e4 = d.getRoot();
		cats = e4.getCategories("http://example.org/scheme");
		assertEquals(1, cats.size());
		assertEquals("bar", cats.get(0).getTerm());
		assertEquals("Bar", cats.get(0).getLabel());
		response.release();
		
	}
	
	

	
}
