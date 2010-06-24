package org.atombeat;


import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"All AtomBeat Protocol Tests");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestStandardAtomProtocol_Fundamentals.class);
		suite.addTestSuite(TestStandardAtomProtocol_ETags.class);
		suite.addTestSuite(TestStandardAtomProtocol_Details.class);
		suite.addTestSuite(TestExtendedAtomProtocol_Collections.class);
		suite.addTestSuite(TestExtendedAtomProtocol_MultipartFormdata.class);
		suite.addTestSuite(TestExtendedAtomProtocol_MultiCreate.class);
		suite.addTestSuite(TestHistoryProtocol.class);
		suite.addTestSuite(TestDefaultSecurityPolicy.class);
		suite.addTestSuite(TestSecurityProtocol.class);
		//$JUnit-END$
		return suite;
	}

}
