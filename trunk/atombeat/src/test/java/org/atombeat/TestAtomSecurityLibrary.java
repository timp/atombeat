package org.atombeat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.methods.GetMethod;

import junit.framework.TestCase;
import static org.atombeat.AtomTestUtils.*;




public class TestAtomSecurityLibrary extends TestCase {

	
	
	public void testAtomSecurityLibrary() throws IOException {
		
		// run the xquery test case
		
		String testUrl = LIB_URI + "test-atom-security.xql";
		GetMethod get = new GetMethod(testUrl);
		int result = executeMethod(get, "adam", "test");
		assertEquals(200, result);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()));
		String currentTest = null;
		
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
		
			if (line.startsWith("test"))
				currentTest = line;
			else {
				String message = currentTest + ": " + line;
				assertFalse(message, line.contains("fail"));
			}
			
		}
		
		get.releaseConnection();
		
	}
	
	
	
}
