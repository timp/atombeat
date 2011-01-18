package org.atombeat.it.content.extended;

import static org.atombeat.it.AtomTestUtils.ADAM;
import static org.atombeat.it.AtomTestUtils.PASSWORD;
import static org.atombeat.it.AtomTestUtils.REALM;
import static org.atombeat.it.AtomTestUtils.SCHEME_BASIC;
import static org.atombeat.it.AtomTestUtils.SERVICE_URL;
import static org.atombeat.it.AtomTestUtils.TEST_COLLECTION_URL;
import static org.atombeat.it.AtomTestUtils.createFilePart;
import static org.atombeat.it.AtomTestUtils.executeMethod;
import static org.atombeat.it.AtomTestUtils.setMultipartRequestEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Link;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import junit.framework.TestCase;

public class TestMediaLinkExtensions extends TestCase {

	
	
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

	
	
	public void testHashAttributeIsPresentAndCorrect() throws URISyntaxException, IOException, NoSuchAlgorithmException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));

		InputStream in = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		in = new DigestInputStream(in, md5);

		String contentType = "application/vnd.ms-excel";

		RequestOptions request = new RequestOptions();
		request.setContentType(contentType);
		
		ClientResponse response = adam.post(TEST_COLLECTION_URL, in, request);
		
		assertEquals(201, response.getStatus());
		
		Document<Entry> entryDoc = response.getDocument();
		Entry entry = entryDoc.getRoot();
		
		// verify hash attribute on edit-media link
		Link editMediaLink = entry.getEditMediaLink();
		String linkHash = editMediaLink.getAttributeValue("hash");
		assertNotNull(linkHash);
		
		// verify hash attribute on content element
		Content content = entry.getContentElement();
		String contentHash = content.getAttributeValue("hash");
		assertNotNull(contentHash);
		
		assertEquals(linkHash, contentHash);
		
		response.release();
		in.close();
		
		String signature = new BigInteger(1, md5.digest()).toString(16);
		String expectedHash = "md5:"+signature;
		assertTrue(linkHash.startsWith(expectedHash)); // allow for other hashes in future
		
	}
	

	
	
	public void testHashAttributeChangesAfterUpdateMedia() throws URISyntaxException, IOException, NoSuchAlgorithmException {
		
		AbderaClient adam = new AbderaClient();
		adam.addCredentials(SERVICE_URL, REALM, SCHEME_BASIC, new UsernamePasswordCredentials(ADAM, PASSWORD));

		InputStream in = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		in = new DigestInputStream(in, md5);

		String contentType = "application/vnd.ms-excel";

		RequestOptions request = new RequestOptions();
		request.setContentType(contentType);
		
		ClientResponse response = adam.post(TEST_COLLECTION_URL, in, request);
		
		assertEquals(201, response.getStatus());
		
		Document<Entry> entryDoc = response.getDocument();
		Entry entry = entryDoc.getRoot();
		
		// verify hash attribute on edit-media link
		Link editMediaLink = entry.getEditMediaLink();
		String location = editMediaLink.getHref().toASCIIString();
		String linkHash = editMediaLink.getAttributeValue("hash");
		assertNotNull(linkHash);
		
		// verify hash attribute on content element
		Content content = entry.getContentElement();
		String contentHash = content.getAttributeValue("hash");
		assertNotNull(contentHash);
		
		assertEquals(linkHash, contentHash);
		
		response.release();
		in.close();
		
		String signature = new BigInteger(1, md5.digest()).toString(16);
		String expectedHash = "md5:"+signature;

		// now put something different
		
		InputStream in_2 = this.getClass().getClassLoader().getResourceAsStream("spreadsheet2.xls");
		MessageDigest md5_2 = MessageDigest.getInstance("MD5");
		in_2 = new DigestInputStream(in_2, md5_2);

		ClientResponse response_2 = adam.put(location, in_2, request);
		
		assertEquals(200, response_2.getStatus());
		
		Document<Entry> entryDoc_2 = response_2.getDocument();
		Entry entry_2 = entryDoc_2.getRoot();
		
		Link editMediaLink_2 = entry_2.getEditMediaLink();
		String linkHash_2 = editMediaLink_2.getAttributeValue("hash");
		assertNotNull(linkHash_2);
		Content content_2 = entry_2.getContentElement();
		String contentHash_2 = content_2.getAttributeValue("hash");
		assertNotNull(contentHash_2);
		assertEquals(linkHash_2, contentHash_2);
		
		assertFalse(linkHash.equals(linkHash_2));
		
		response.release();
		in.close();
		
		String signature_2 = new BigInteger(1, md5_2.digest()).toString(16);
		String expectedHash_2 = "md5:"+signature_2;
		assertFalse(expectedHash.equals(expectedHash_2)); // hashes should be different
		assertTrue(linkHash_2.startsWith(expectedHash_2)); // allow for other hashes in future
		
	}
	

	
	
	public void testHashAttributeIsPresentAndCorrectAfterMultipartPost() throws Exception {
		
		// now create a new media resource by POSTing multipart/form-data to the collection URI
		PostMethod post = new PostMethod(TEST_COLLECTION_URL);
		File file = new File(this.getClass().getClassLoader().getResource("spreadsheet1.xls").getFile());
		FilePart fp = createFilePart(file, "spreadsheet1.xls", "application/vnd.ms-excel", "media");
		StringPart sp1 = new StringPart("summary", "this is a great spreadsheet");
		StringPart sp2 = new StringPart("category", "scheme=\"foo\"; term=\"bar\"; label=\"baz\"");
		Part[] parts = { fp , sp1 , sp2 };
		setMultipartRequestEntity(post, parts);
		int result = executeMethod(post);
		
		assertEquals(201, result);

		Abdera abdera = new Abdera();
		Document<Entry> entryDoc = abdera.getParser().parse(post.getResponseBodyAsStream());
		Entry entry = entryDoc.getRoot();
		
		// verify hash attribute on edit-media link
		Link editMediaLink = entry.getEditMediaLink();
		String linkHash = editMediaLink.getAttributeValue("hash");
		assertNotNull(linkHash);
		
		// verify hash attribute on content element
		Content content = entry.getContentElement();
		String contentHash = content.getAttributeValue("hash");
		assertNotNull(contentHash);
		
		assertEquals(linkHash, contentHash);
		
		post.releaseConnection();
		
		// have to read file again to get md5
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("spreadsheet1.xls");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		in = new DigestInputStream(in, md5);
		byte[] buffer = new byte[10000];
		while (true) {
			int amountRead = in.read(buffer);
			if (amountRead == -1) {
				break;
			}
		} 
		String signature = new BigInteger(1, md5.digest()).toString(16);
		String expectedHash = "md5:"+signature;
		assertTrue(linkHash.startsWith(expectedHash)); // allow for other hashes in future
		
	}

	
	
}
