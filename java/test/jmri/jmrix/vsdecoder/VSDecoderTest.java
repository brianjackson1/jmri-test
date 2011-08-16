// RosterTest.java

package jmri.jmrix.vsdecoder;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests for the jmrix.vsdecoder package & jmrix.vsdecoder.VSDecoder class.
 * @author	Mark Underwood     Copyright (C) 2011
 * @version     $Revision: 17977 $
 */
public class VSDecoderTest extends TestCase {

    // Tests for the VSDecoder class...

    public void testCreate() {
	// do something
    }

    // from here down is testing infrastructure

    public VSDecoderTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
        String[] testCaseName = {"-noloading", VSDecoderTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(VSDecoderTest.class);
        suite.addTest(jmri.jmrix.vsdecoder.TriggerTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.BoolTriggerTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.ButtonTriggerTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.FloatTriggerTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.IntTriggerTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.NotchTriggerTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.VSDSoundTest.suite());
        suite.addTest(jmri.jmrix.vsdecoder.SoundBiteTest.suite());

        if (!System.getProperty("jmri.headlesstest","false").equals("true")) {
	    // Put swing tests here (?)

            //suite.addTest(jmri.jmrit.roster.CopyRosterItemActionTest.suite());
            //suite.addTest(jmri.jmrit.roster.RosterEntryPaneTest.suite());
            //suite.addTest(jmri.jmrit.roster.FunctionLabelPaneTest.suite());
            //suite.addTest(jmri.jmrit.roster.IdentifyLocoTest.suite());
        }
        
        return suite;
    }

    // The minimal setup for log4J
    protected void setUp() {
	//super.setUp();
	jmri.util.JUnitUtil.resetInstanceManager();
	apps.tests.Log4JFixture.setUp();
    }
    protected void tearDown() { apps.tests.Log4JFixture.tearDown(); }

}
