package org.atombeat;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"All AtomBeat Protocol Tests");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestAtomProtocol.class);
		suite.addTestSuite(TestHistoryProtocol.class);
		suite.addTestSuite(TestAtomProtocolWithDefaultSecurity.class);
		suite.addTestSuite(TestSecurityProtocol.class);
		//$JUnit-END$
		return suite;
	}

}
