package org.cggh.chassis.spike.atomserver;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for org.cggh.chassis.spike.atomserver");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestAtomProtocol.class);
		suite.addTestSuite(TestHistoryProtocol.class);
		suite.addTestSuite(TestAtomProtocolWithDefaultSecurity.class);
		suite.addTestSuite(TestAclProtocol.class);
		//$JUnit-END$
		return suite;
	}

}
