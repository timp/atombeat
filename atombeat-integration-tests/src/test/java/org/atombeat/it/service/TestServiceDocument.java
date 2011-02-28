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

import junit.framework.TestCase;
import static org.atombeat.it.AtomTestUtils.SERVICE_URL;
import static org.atombeat.it.AtomTestUtils.ADAM;
import static org.atombeat.it.AtomTestUtils.PASSWORD;
import static org.atombeat.it.AtomTestUtils.REALM;
import static org.atombeat.it.AtomTestUtils.SCHEME_BASIC;
import static org.atombeat.it.AtomTestUtils.executeMethod;





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



}
