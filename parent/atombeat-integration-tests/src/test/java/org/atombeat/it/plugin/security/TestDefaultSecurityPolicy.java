package org.atombeat.it.plugin.security;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import org.atombeat.it.AtomTestUtils;
import static org.atombeat.it.AtomTestUtils.*;

import junit.framework.TestCase;




public class TestDefaultSecurityPolicy extends TestCase {


	
	
	
	private String testCollectionUri = null;
	
	private DOMImplementationRegistry domImplRegistry;
	private DOMImplementationLS domImplLs;
	private LSSerializer lsWriter;

	

	
	
	
	
	protected void setUp() throws Exception {
		super.setUp();

		String installUrl = SERVICE_URL + "admin/setup-for-test.xql";
		
		GetMethod method = new GetMethod(installUrl);
		
		int result = executeMethod(method, "adam", "test");
		
		if (result != 200) {
			throw new RuntimeException("installation failed: "+result);
		}
	
		testCollectionUri = createTestCollection(CONTENT_URL, "adam", "test");

		domImplRegistry = DOMImplementationRegistry.newInstance();
		domImplLs = (DOMImplementationLS)domImplRegistry.getDOMImplementation("LS");
		lsWriter = domImplLs.createLSSerializer();
		lsWriter.getDomConfig().setParameter("xml-declaration", false);

	}
	
	
	

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	
	public void testUserWithAdministratorRoleCanRetrieveService() throws URISyntaxException {
		
//		GetMethod get = new GetMethod(SERVICE_URL);
//		int result = executeMethod(get, "adam", "test");
//		assertEquals(200, result);
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		RequestOptions request = new RequestOptions();
		ClientResponse response = adam.get(SERVICE_URL, request);
		
		assertEquals(200, response.getStatus());
		
	}
	
	
	
	public void testUserWithoutAdministratorRoleCannotRetrieveService() throws URISyntaxException {
		
//		GetMethod get = new GetMethod(SERVICE_URL);
//		int result = executeMethod(get, "ursula", "test");
//		assertEquals(403, result);
		
		AbderaClient ursula = new AbderaClient();
		ursula.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(URSULA, PASSWORD));
		
		RequestOptions request = new RequestOptions();
		ClientResponse response = ursula.get(SERVICE_URL, request);
		
		assertEquals(403, response.getStatus());
		
		response.release();

	}
	
	
	
	public void testCollectionsFilteredInServiceDocument() throws URISyntaxException {

		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));
		
		RequestOptions request = new RequestOptions();
		ClientResponse response = adam.get(SERVICE_URL, request);
		
		assertEquals(200, response.getStatus());
		
		org.apache.abdera.model.Document<Service> doc = response.getDocument();
		Service service = doc.getRoot();
		Workspace workspace = service.getWorkspaces().get(0);
		List<Collection> collections = workspace.getCollections();
		assertTrue(collections.size()>0);
		
		response.release();
		
		// laura (limited reader) should be able to retrieve the service doc but collections should be filtered out
		
		AbderaClient laura = new AbderaClient();
		laura.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(LAURA, PASSWORD));
		
		request = new RequestOptions();
		response = laura.get(SERVICE_URL, request);
		
		assertEquals(200, response.getStatus());
		
		doc = response.getDocument();
		service = doc.getRoot();
		workspace = service.getWorkspaces().get(0);
		collections = workspace.getCollections();
		assertEquals(0, collections.size());
		
		response.release();

	}

	
	
	
	
	public void testUserWithAdministratorRoleCanCreateCollections() {
		
		// we expect the default global ACL to allow only users with the
		// ROLE_ADMINISTRATOR role to create collections
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());

		PutMethod method = new PutMethod(collectionUri);

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "adam" to be defined in the example
		// security config to be assigned the ROLE_ADMINISTRATOR role
		
		int result = executeMethod(method, "adam", "test");
		
		assertEquals(201, result);

	}
	
	
	
	
	public void testUserWithAdministratorRoleCanUpdateCollections() {
		
		// we expect the default global ACL to allow only users with the
		// ROLE_ADMINISTRATOR role to update collections
		
		String collectionUri = createTestCollection(CONTENT_URL, "adam", "test");

		PutMethod method = new PutMethod(collectionUri);

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection - Updated</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "adam" to be defined in the example
		// security config to be assigned the ROLE_ADMINISTRATOR role
		
		int result = executeMethod(method, "adam", "test");
		
		assertEquals(200, result);

	}
	
	
	
	
	public void testUserWithoutAdministratorRoleCannotCreateCollections() {
		
		// we expect the default global ACL to allow only users with the
		// ROLE_ADMINISTRATOR role to create collections
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());

		PutMethod method = new PutMethod(collectionUri);

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "rebecca" to be defined in the example
		// security config but NOT to be assigned the ROLE_ADMINISTRATOR role
		
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(403, result);

	}
	
	
	
	
	public void testUserWithoutAdministratorRoleCannotUpdateCollections() {
		
		String collectionUri = createTestCollection(CONTENT_URL, "adam", "test");

		// we expect the default global ACL to allow only users with the
		// ROLE_ADMINISTRATOR role to update collections
		
		PutMethod method = new PutMethod(collectionUri);

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection - Updated</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "rebecca" to be defined in the example
		// security config but NOT to be assigned the ROLE_ADMINISTRATOR role
		
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(403, result);

	}
	
	
	
	
	public void testUserWithAdministratorRoleCanCreateCollectionsViaLegacyPost() {
		
		// we expect the default global ACL to allow only users with the
		// ROLE_ADMINISTRATOR role to create collections
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());

		PostMethod method = new PostMethod(collectionUri);

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "adam" to be defined in the example
		// security config to be assigned the ROLE_ADMINISTRATOR role
		
		int result = executeMethod(method, "adam", "test");
		
		assertEquals(201, result);

	}
	
	
	
	

	public void testUserWithoutAdministratorRoleCannotCreateCollectionsViaLegacyPost() {
		
		// we expect the default global ACL to allow only users with the
		// ROLE_ADMINISTRATOR role to create collections
		
		String collectionUri = CONTENT_URL + Double.toString(Math.random());

		PostMethod method = new PostMethod(collectionUri);

		String content = "<atom:feed xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Collection</atom:title></atom:feed>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "rebecca" to be defined in the example
		// security config *not* to be assigned the ROLE_ADMINISTRATOR role
		
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(403, result);

	}
	
	
	
	

	
	public void testUserWithAuthorRoleCanCreateAtomEntries() {
		
		// we expect the default collection ACL to allow users with the
		// ROLE_AUTHOR role to create atom entries in any collection
		 
		PostMethod method = new PostMethod(testCollectionUri);
		
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry</atom:title><atom:summary>this is a test</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "austin" to be defined in the example
		// security config to be assigned the ROLE_AUTHOR role
		
		int result = executeMethod(method, "austin", "test");
		
		assertEquals(201, result);
		
	}
	
	
	
	
	public void testUserWithoutAuthorRoleCannotCreateAtomEntries() {
		
		// we expect the default collection ACL to allow only users with the
		// ROLE_AUTHOR role to create atom entries in any collection
		 
		PostMethod method = new PostMethod(testCollectionUri);
		
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry</atom:title><atom:summary>this is a test</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "rebecca" to be defined in the example
		// security config but NOT to be assigned the ROLE_AUTHOR role
		
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(403, result);
		
	}
	
	
	
	
	public void testUserWithEditorRoleCanUpdateAtomEntries() throws Exception {

		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "austin", "test");

		// we expect the default collection ACL to allow only users with the
		// ROLE_EDITOR role to update any atom entries in any collection

		PutMethod method = new PutMethod(entryUri);

		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "edwina" to be defined in the example
		// security config to be assigned the ROLE_EDITOR role
		
		int result = executeMethod(method, "edwina", "test");
		
		assertEquals(200, result);

	}
	
	
	
	
	public void testUserWithoutEditorRoleCannotUpdateAtomEntries() throws Exception {

		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "austin", "test");

		// we expect the default collection ACL to allow only users with the
		// ROLE_EDITOR role to update any atom entries in any collection

		PutMethod method = new PutMethod(entryUri);

		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		
		// we expect the user "rebecca" to be defined in the example
		// security config but NOT to be assigned the ROLE_EDITOR role
		
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(403, result);

	}
	
	
	
	public void testAuthorCanUpdateTheirOwnEntry() throws Exception {

		// we expect "audrey" is assigned the ROLE_AUTHOR so can create entries
		
		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");

		PutMethod method = new PutMethod(entryUri);

		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		
		// we expect the default resource ACL to allow users to update entries
		// they created

		int result = executeMethod(method, "audrey", "test");
		
		assertEquals(200, result);

	}



	public void testAuthorCannotUpdateAnotherAuthorsEntry() throws Exception {

		// we expect "audrey" is assigned the ROLE_AUTHOR so can create entries
		
		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");

		PutMethod method = new PutMethod(entryUri);

		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method, content);
		
		// we expect the default resource ACL to allow users to update entries
		// they created, but not to allow them to update other authors' entries

		int result = executeMethod(method, "austin", "test");
		
		assertEquals(403, result);

	}
	
	
	
	public void testUserWithReaderRoleCanListCollections() throws Exception {

		// setup test
		createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");
		
		// list collection, expecting user "rebecca" to be in role ROLE_READER
		GetMethod method = new GetMethod(testCollectionUri);
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(200, result);
		
	}
	
	
	
	public void testUserWithoutReaderRoleCannotListCollections() throws Exception {

		// setup test
		createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");
		
		// list collection, expecting user "ursula" not to be in role ROLE_READER
		GetMethod method = new GetMethod(testCollectionUri);
		int result = executeMethod(method, "ursula", "test");
		
		assertEquals(403, result);
		
	}
	
	
	
	
	public void testUserWithReaderRoleCanRetrieveEntry() throws Exception {
		
		// setup test
		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");
		
		// retrieve entry, expecting user "rebecca" to be in role ROLE_READER
		GetMethod method = new GetMethod(entryUri);
		int result = executeMethod(method, "rebecca", "test");
		
		assertEquals(200, result);
	}
	
	
	
	public void testUserWithoutReaderRoleCannotRetrieveEntry() throws Exception {
		
		// setup test
		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");
		
		// list collection, expecting user "ursula" not to be in role ROLE_READER
		GetMethod method = new GetMethod(entryUri);
		int result = executeMethod(method, "ursula", "test");
		
		assertEquals(403, result);

	}
	
	
	
	public void testUserWithAuthorRoleCanCreateMedia() {
		
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, "audrey", "test");
		
		// we expect the user "audrey" to be defined in the example
		// security config to be assigned the ROLE_AUTHOR role
		
		assertEquals(201, result);

	}



	public void testUserWithoutAuthorRoleCannotCreateMedia() {
		
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, "rebecca", "test");
		
		// we expect the user "rebecca" to be defined in the example
		// security config to not be assigned the ROLE_AUTHOR role
		
		assertEquals(403, result);

	}



	public void testUserWithDataAuthorRoleCanCreateMediaWithSpecificMediaType() {
		
		PostMethod method = new PostMethod(testCollectionUri);
		InputStream content = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		String contentType = "application/vnd.ms-excel";
		setInputStreamRequestEntity(method, content, contentType);
		int result = executeMethod(method, "daniel", "test");
		
		// we expect the user "daniel" to be defined in the example
		// security config to be assigned the ROLE_DATA_AUTHOR role
		
		assertEquals(201, result);

	}



	public void testUserWithDataAuthorRoleCannotCreateMediaWithSpecificMediaType() {
		
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		int result = executeMethod(method, "daniel", "test");
		
		// we expect the user "daniel" to be defined in the example
		// security config to be assigned the ROLE_DATA_AUTHOR role
		
		assertEquals(403, result);

	}
	
	
	
	public void testUserWithReaderRoleCanRetrieveMediaLinkEntry() {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		String entryUri = method.getResponseHeader("Location").getValue();
		
		// retrieve entry, expecting user "rebecca" to be in role ROLE_READER
		GetMethod method2 = new GetMethod(entryUri);
		int result = executeMethod(method2, "rebecca", "test");
		
		assertEquals(200, result);

	}
	
	
	
	public void testUserWithoutReaderRoleCannotRetrieveMediaLinkEntry() {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		String entryUri = method.getResponseHeader("Location").getValue();
		
		// retrieve entry, expecting user "rebecca" to be in role ROLE_READER
		GetMethod method2 = new GetMethod(entryUri);
		int result = executeMethod(method2, "ursula", "test");
		
		assertEquals(403, result);

	}
	
	
	
	public void testUserWithReaderRoleCanRetrieveMediaResource() throws Exception {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);
		
		// retrieve media resource, expecting user "rebecca" to be in role ROLE_READER
		GetMethod method2 = new GetMethod(mediaLocation);
		int result = executeMethod(method2, "rebecca", "test");
		
		assertEquals(200, result);

	}
	
	
	
	public void testUserWithoutReaderRoleCannotRetrieveMediaResource() throws Exception {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);
		
		// retrieve media resource, expecting user "rebecca" to be in role ROLE_READER
		GetMethod method2 = new GetMethod(mediaLocation);
		int result = executeMethod(method2, "ursula", "test");
		
		assertEquals(403, result);

	}
	
	
	
	public void testUserWithMediaEditorRoleCanUpdateMediaResources() throws Exception {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);

		// try to update media resource
		PutMethod method2 = new PutMethod(mediaLocation);
		String media2 = "This is a test - updated.";
		setTextPlainRequestEntity(method2, media2);
		int result = executeMethod(method2, "melanie", "test");
		
		assertEquals(200, result);

	}
	
	
	
	
	public void testUserWithoutMediaEditorRoleCannotUpdateMediaResources() throws Exception {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);

		// try to update media resource
		PutMethod method2 = new PutMethod(mediaLocation);
		String media2 = "This is a test - updated.";
		setTextPlainRequestEntity(method2, media2);
		int result = executeMethod(method2, "rebecca", "test");
		
		assertEquals(403, result);

	}
	
	
	
	

	public void testAuthorsCanRetrieveEntryTheyCreated() throws Exception {
		
		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");

		GetMethod method = new GetMethod(entryUri);
		
		// we expect the default resource ACL to allow users to retrieve entries
		// they created

		int result = executeMethod(method, "audrey", "test");
		
		assertEquals(200, result);

	}



	public void testAuthorsCannotRetrieveAnotherAuthorsEntry() throws Exception {
		
		String entryUri = createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");

		GetMethod method = new GetMethod(entryUri);
		
		// we expect the default resource ACL to allow users to retrieve entries
		// they created, but not to allow them to retrieve other authors' entries

		int result = executeMethod(method, "austin", "test");
		
		assertEquals(403, result);

	}
	
	
	
	public void testAuthorsCanListCollectionButOnlyRetrieveEntriesTheyCreated() throws Exception {

		createTestMemberAndReturnLocation(testCollectionUri, "audrey", "test");
		createTestMemberAndReturnLocation(testCollectionUri, "austin", "test");
		
		GetMethod method = new GetMethod(testCollectionUri);
		int result = executeMethod(method, "audrey", "test");
		
		assertEquals(200, result);

		Document feedDoc = getResponseBodyAsDocument(method);
		
		List<Element> entries = getChildrenByTagNameNS(feedDoc, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries.size());
		
		method = new GetMethod(testCollectionUri);
		result = executeMethod(method, "austin", "test");
		
		assertEquals(200, result);

		feedDoc = getResponseBodyAsDocument(method);
		
		entries = getChildrenByTagNameNS(feedDoc, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(1, entries.size());
		
		// but readers should be able to retrieve all entries
		
		method = new GetMethod(testCollectionUri);
		result = executeMethod(method, "rebecca", "test");
		
		assertEquals(200, result);

		feedDoc = getResponseBodyAsDocument(method);
		
		entries = getChildrenByTagNameNS(feedDoc, "http://www.w3.org/2005/Atom", "entry");
		assertEquals(2, entries.size());
		
	}


	
	public void testAuthorsCanRetrieveMediaResourceTheyCreated() throws Exception {

		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);

		// try to retrieve media resource
		GetMethod method2 = new GetMethod(mediaLocation);
		int result = executeMethod(method2, "audrey", "test");
		
		assertEquals(200, result);

	}

	
	
	public void testAuthorsCannotRetrieveAnotherAuthorsMediaResource() throws Exception {


		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);

		// try to retrieve media resource
		GetMethod method2 = new GetMethod(mediaLocation);
		int result = executeMethod(method2, "austin", "test");
		
		assertEquals(403, result);

	}

	
	
	public void testAuthorCanUpdateTheirOwnMediaResources() throws Exception {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);

		// try to update media resource
		PutMethod method2 = new PutMethod(mediaLocation);
		String media2 = "This is a test - updated.";
		setTextPlainRequestEntity(method2, media2);
		int result = executeMethod(method2, "audrey", "test");
		
		assertEquals(200, result);

	}
	
	
	
	
	public void testAuthorCannotUpdateAnotherAuthorsMediaResources() throws Exception {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);

		// try to update media resource
		PutMethod method2 = new PutMethod(mediaLocation);
		String media2 = "This is a test - updated.";
		setTextPlainRequestEntity(method2, media2);
		int result = executeMethod(method2, "austin", "test");
		
		assertEquals(403, result);

	}
	
	
	
	
	public void testAuthorCanUpdateTheirOwnMediaLinkEntries() {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		String location = method.getResponseHeader("Location").getValue();
		
		// try to update media link entry
		PutMethod method2 = new PutMethod(location);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method2, content);
		int result = executeMethod(method2, "audrey", "test");

		// we expect the default resource ACL to allow users to update media
		// link entries they created
		
		assertEquals(200, result);

	}
	
	
	
	
	public void testAuthorCannotUpdateAnotherAuthorsMediaLinkEntries() {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		String location = method.getResponseHeader("Location").getValue();
		
		// try to update media link entry
		PutMethod method2 = new PutMethod(location);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method2, content);
		int result = executeMethod(method2, "austin", "test");

		assertEquals(403, result);

	}
	
	
	
	
	public void testUserWithEditorRoleCanUpdateMediaLinkEntries() {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		String location = method.getResponseHeader("Location").getValue();
		
		// try to update media link entry
		PutMethod method2 = new PutMethod(location);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method2, content);
		int result = executeMethod(method2, "edwina", "test");

		assertEquals(200, result);

	}
	
	
	
	public void testUserWithoutEditorRoleCannotUpdateMediaLinkEntries() {
		
		// setup test
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a test.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		String location = method.getResponseHeader("Location").getValue();
		
		// try to update media link entry
		PutMethod method2 = new PutMethod(location);
		String content = "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\"><atom:title>Test Entry - Edited</atom:title><atom:summary>this is a test, edited</atom:summary></atom:entry>";
		setAtomRequestEntity(method2, content);
		int result = executeMethod(method2, "rebecca", "test");

		assertEquals(403, result);

	}
	

	
	public void testUserWithAuthorRoleCanCreateMediaWithMultipartRequest() {
		
		PostMethod method = createMultipartRequest(testCollectionUri);
		int result = executeMethod(method, "audrey", "test");
		assertEquals(201, result);
		
	}
	
	
	
	public void testUserWithoutAuthorRoleCannotCreateMediaWithMultipartRequest() {
		
		PostMethod method = createMultipartRequest(testCollectionUri);
		int result = executeMethod(method, "rebecca", "test");
		assertEquals(403, result);
		
	}
	
	
	
	
	private PostMethod createMultipartRequest(String collectionUri) {
		
		PostMethod post = new PostMethod(collectionUri);
		File file = new File(this.getClass().getClassLoader().getResource("spreadsheet1.xls").getFile());
		FilePart fp = createFilePart(file, "spreadsheet1.xls", "application/vnd.ms-excel", "media");
		StringPart sp = new StringPart("summary", "this is a great spreadsheet");
		Part[] parts = { fp , sp };
		setMultipartRequestEntity(post, parts);
		return post;
		
	}



	
	public void testUserWithMediaEditorRoleCanDeleteMediaResource() throws Exception {
		
		// setup test
		String collectionUri = createTestCollection(CONTENT_URL, "adam", "test");
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1, "rebecca", "test");
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2, "rebecca", "test");
		assertEquals(200, resultGet2);
		
		// now try delete on media location
		DeleteMethod delete = new DeleteMethod(mediaLocation);
		int result = executeMethod(delete, "melanie", "test");
		assertEquals(204, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3, "rebecca", "test");
		assertEquals(404, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4, "rebecca", "test");
		assertEquals(404, resultGet4);
		
	}


	
	
	public void testUserWithoutMediaEditorRoleCannotDeleteMediaResource() throws Exception {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URL, "adam", "test");
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1, "rebecca", "test");
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2, "rebecca", "test");
		assertEquals(200, resultGet2);
		
		// now try delete on media location
		DeleteMethod delete = new DeleteMethod(mediaLocation);
		int result = executeMethod(delete, "edwina", "test");
		assertEquals(403, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3, "rebecca", "test");
		assertEquals(200, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4, "rebecca", "test");
		assertEquals(200, resultGet4);
		
}

	
	
	
	public void testUserWithMediaEditorRoleCanDeleteMediaResourceViaMediaLinkLocation() throws Exception {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URL, "adam", "test");
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1, "rebecca", "test");
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2, "rebecca", "test");
		assertEquals(200, resultGet2);
		
		// now try delete on media location
		DeleteMethod delete = new DeleteMethod(mediaLinkLocation);
		int result = executeMethod(delete, "melanie", "test");
		assertEquals(204, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3, "rebecca", "test");
		assertEquals(404, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4, "rebecca", "test");
		assertEquals(404, resultGet4);
		
	}

	
	
	
	public void testUserWithoutMediaEditorRoleCannotDeleteMediaResourceViaMediaLinkLocation() throws Exception {

		// setup test
		String collectionUri = createTestCollection(CONTENT_URL, "adam", "test");
		Document mediaLinkDoc = createTestMediaResourceAndReturnMediaLinkEntry(collectionUri, "audrey", "test");
		String mediaLocation = getEditMediaLocation(mediaLinkDoc);
		String mediaLinkLocation = getEditLocation(mediaLinkDoc);
		
		// now try to get media
		GetMethod get1 = new GetMethod(mediaLocation);
		int resultGet1 = executeMethod(get1, "rebecca", "test");
		assertEquals(200, resultGet1);
		
		// now try to get media link
		GetMethod get2 = new GetMethod(mediaLinkLocation);
		int resultGet2 = executeMethod(get2, "rebecca", "test");
		assertEquals(200, resultGet2);
		
		// now try delete on media location
		DeleteMethod delete = new DeleteMethod(mediaLinkLocation);
		int result = executeMethod(delete, "edwina", "test");
		assertEquals(403, result);
		
		// now try to get media again
		GetMethod get3 = new GetMethod(mediaLocation);
		int resultGet3 = executeMethod(get3, "rebecca", "test");
		assertEquals(200, resultGet3);
		
		// now try to get media link again
		GetMethod get4 = new GetMethod(mediaLinkLocation);
		int resultGet4 = executeMethod(get4, "rebecca", "test");
		assertEquals(200, resultGet4);
		
	}

	
	
	public void testUserWithoutReaderRoleCannotRetrieveHistoryOrRevision() throws Exception {
		
		String collectionUri = createTestVersionedCollection(CONTENT_URL, "adam", "test");
		Document d = createTestMemberAndReturnDocument(collectionUri, "austin", "test");
		String historyLocation = getLinkHref(d, "history");
		assertNotNull(historyLocation);
		
		// try to retrieve history as reader
		GetMethod get1 = new GetMethod(historyLocation);
		int get1result = executeMethod(get1, "rebecca", "test");
		assertEquals(200, get1result);
		
		// pick out revision from history feed
		Document historyFeedDoc = getResponseBodyAsDocument(get1);
		Element entry = (Element) historyFeedDoc.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry").item(0);
		String revLocation = getLinkHref(entry, "this-revision");
		assertNotNull(revLocation);
		
		// try to retrieve history as user
		GetMethod get2 = new GetMethod(historyLocation);
		int get2result = executeMethod(get2, "ursula", "test");
		assertEquals(403, get2result);
		
		// retrieve revision as reader
		GetMethod get3 = new GetMethod(revLocation);
		int get3result = executeMethod(get3, "rebecca", "test");
		assertEquals(200, get3result);
		
		// retrieve revision as user
		GetMethod get4 = new GetMethod(revLocation);
		int get4result = executeMethod(get4, "ursula", "test");
		assertEquals(403, get4result);
		
	}

	
	
	
	public void testAuthorCannotStealAnotherAuthorsMediaResourcesViaMultiCreate() throws Exception {

		// this test exposes a security flaw involving the atombeat "multicreate" 
		// protocol extension - when posting a feed containing a media-link
		// entry, atombeat will copy the entry and the associated media resource
		// to the new collection, but should check that the requesting user
		// has permission to retrieve the media resource first, otherwise
		// the user could steal a media resource they do not have access to, 
		// given the media resource URI
		
		// create media resource as Audrey
		PostMethod method = new PostMethod(testCollectionUri);
		String media = "This is a secret message.";
		setTextPlainRequestEntity(method, media);
		executeMethod(method, "audrey", "test");
		Document doc = AtomTestUtils.getResponseBodyAsDocument(method);
		String mediaLocation = AtomTestUtils.getEditMediaLocation(doc);
		
		// double check Austin is not allowed to retrieve the media resource
		GetMethod get0 = new GetMethod(mediaLocation);
		int get0result = executeMethod(get0, "austin", "test");
		assertEquals(403, get0result);
		
		// list collection as Audrey
		GetMethod get1 = new GetMethod(testCollectionUri);
		executeMethod(get1, "audrey", "test");
		Document d1 = getResponseBodyAsDocument(get1);
		List<Element> entries = getEntries(d1);
		assertEquals(1, entries.size());
		
		// list collection as Austin
		GetMethod get2 = new GetMethod(testCollectionUri);
		executeMethod(get2, "austin", "test");
		Document d2 = getResponseBodyAsDocument(get2);
		entries = getEntries(d2);
		assertEquals(0, entries.size());

		// try to multi-create as Austin using Audrey's feed
		String feed = lsWriter.writeToString(d1);
		PostMethod post2 = new PostMethod(testCollectionUri);
		setAtomRequestEntity(post2, feed);
		int post2Result = executeMethod(post2, "austin", "test");
		assertEquals(200, post2Result); // should succeed, but should not copy media resources
		
		// list collection as Austin
		GetMethod get3 = new GetMethod(testCollectionUri);
		executeMethod(get3, "austin", "test");
		Document d3 = getResponseBodyAsDocument(get3);
		entries = getEntries(d3); assertEquals(1, entries.size());
		
		String newMediaLocation = getEditMediaLocation(entries.get(0));
		assertNull(newMediaLocation); // edit-media link should have been stripped
		
		String contentLocation = getAtomContent(d3).getAttribute("src"); // content element should remain tho, pointing to original media resource
 		
		// try to GET Audrey's media resource as Austin
		GetMethod get4 = new GetMethod(contentLocation);
		int get4result = executeMethod(get4, "austin", "test");

		assertFalse("This is a secret message.".equals(get4.getResponseBodyAsString().trim()));
		assertEquals(403, get4result);
		assertEquals(mediaLocation, contentLocation); // should be same, i.e., media not copied to new location

	}
	
	
	
}



