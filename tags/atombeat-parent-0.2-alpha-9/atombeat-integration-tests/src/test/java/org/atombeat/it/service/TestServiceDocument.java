/**
 * 
 */
package org.atombeat.it.service;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.CONTENT_URL;
import static org.atombeat.it.AtomTestUtils.SERVICE_URL;
import static org.atombeat.it.AtomTestUtils.ADAM;
import static org.atombeat.it.AtomTestUtils.PASSWORD;
import static org.atombeat.it.AtomTestUtils.REALM;
import static org.atombeat.it.AtomTestUtils.SCHEME_BASIC;
import static org.atombeat.it.AtomTestUtils.executeMethod;
import static org.atombeat.it.AtomTestUtils.setAtomRequestEntity;





/**
 * @author aliman
 *
 */
public class TestServiceDocument extends TestCase {

	
	
	
	
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

	
	
	
	public void testGetServiceDocument() throws URISyntaxException {

		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		RequestOptions request = new RequestOptions();
		ClientResponse response = adam.get(SERVICE_URL, request);
		
		assertEquals(200, response.getStatus());
		assertTrue(response.getHeader("Content-Type").startsWith("application/atomsvc+xml"));
		
		Document<Service> serviceDoc = response.getDocument();
		Service service = serviceDoc.getRoot();
		assertNotNull(service);

		// verify the workspace
		List<Workspace> workspaces = service.getWorkspaces();
		assertEquals(1, workspaces.size()); // expect just one
		Workspace workspace = workspaces.get(0);
		assertNotNull(workspace.getTitle());
	
		// verify collections
		List<Collection> collections = workspace.getCollections();
		assertTrue(collections.size() > 0);
		for (Collection c : collections) {
			assertNotNull(c.getTitle());
			assertNotNull(c.getHref());
		}
		
		response.release();

	}

	
	
	public void testHiddenCollection() throws URISyntaxException {
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());
		PutMethod method = new PutMethod(collectionUri);
		String content = 
			"<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\">" +
				"<atom:title>Test Collection</atom:title>" +
				"<app:collection xmlns:app=\"http://www.w3.org/2007/app\">" +
					"<f:features xmlns:f=\"http://purl.org/atompub/features/1.0\">" +
						"<f:feature ref=\"http://purl.org/atombeat/feature/HiddenFromServiceDocument\"/>" +
					"</f:features>" +
				"</app:collection>" +
			"</atom:feed>";
		setAtomRequestEntity(method, content);
		int result = executeMethod(method, ADAM, PASSWORD);
		assertEquals(201, result);
		String location = method.getResponseHeader("Location").getValue();
		
		// now look for the collection in the service document
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		RequestOptions request = new RequestOptions();
		ClientResponse response = adam.get(SERVICE_URL, request);
		assertEquals(200, response.getStatus());
		Document<Service> serviceDoc = response.getDocument();
		Service service = serviceDoc.getRoot();
		boolean hidden = true;
		for (Workspace w : service.getWorkspaces()) {
			for (Collection c : w.getCollections()) {
				if (c.getResolvedHref().toString().equals(location)) {
					hidden = false;
				}
			}
		}
		assertTrue(hidden);

	}


}
