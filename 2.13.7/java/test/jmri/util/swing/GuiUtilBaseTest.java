// GuiUtilBaseTest.java

package jmri.util.swing;

import junit.framework.*;

/**
 * Checks of JMRI XML Schema for GUI definition files.
 * 
 * @author Bob Jacobsen Copyright 2011
 * @since 2.9.3
 * @version $Revision$
 */
public class GuiUtilBaseTest extends jmri.configurexml.LoadFileTestBase {

    public void testRealFiles() {
        // should probably be a tree search
        doDirectory("xml/config/apps/decoderpro");
        doDirectory("xml/config/apps/demo");
        doDirectory("xml/config/apps/panelpro");
        doDirectory("xml/config/parts/jmri/jmrix/loconet");
    }
    
    void doDirectory(String pathName) {
        java.io.File dir = new java.io.File(pathName);
        java.io.File[] files = dir.listFiles();
        for (int i=0; i<files.length; i++) {
            if (files[i].getName().endsWith("xml")) {
                validate(files[i]);
            }
        }
    }
    
    // from here down is testing infrastructure

    public GuiUtilBaseTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
		String[] testCaseName = {"-noloading", GuiUtilBaseTest.class.getName()};
		junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(GuiUtilBaseTest.class);
        return suite;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GuiUtilBaseTest.class.getName());
}
