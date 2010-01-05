// RpsPositionIconTest.java

package jmri.jmrix.rps;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit tests for the rps.Reading class.
 * @author	Bob Jacobsen Copyright 2006
 * @version	$Revision: 1.5 $
 */
public class RpsPositionIconTest extends TestCase {

	public void testCtorAndID() throws Exception {
        // init test system
        new Engine(){ void reset() { _instance = null; }}.reset();

        new jmri.configurexml.ConfigXmlManager().load(new java.io.File("java/test/jmri/jmrix/rps/LocationTestPanel.xml"));        

        // and push a good measurement
        Reading loco = new Reading("27", null);
        Measurement m = new Measurement(loco, 0.0, 0.0, 0.0, 0.133, 5, "source");
        Distributor.instance().submitMeasurement(m);
        
  	}
        
	// from here down is testing infrastructure

	public RpsPositionIconTest(String s) {
            super(s);
	}

	// Main entry point
	static public void main(String[] args) {
            String[] testCaseName = {RpsPositionIconTest.class.getName()};
            junit.swingui.TestRunner.main(testCaseName);
	}

	// test suite from all defined tests
	public static Test suite() {
            TestSuite suite = new TestSuite(RpsPositionIconTest.class);
            return suite;
	}

}
