// SerialTurnoutTest.java

package jmri.jmrix.powerline;

import jmri.*;
import junit.framework.*;

/**
 * Tests for the jmri.jmrix.powerline.SerialTurnout class
 * @author			Bob Jacobsen Copyright 2008
 * @version			$Revision: 1.2 $
 */
public class SerialTurnoutTest extends AbstractTurnoutTest {

	private SerialTrafficControlScaffold tcis = new SerialTrafficControlScaffold();
    private SerialNode n = new SerialNode();

	public void setUp() {
		// prepare an interface
		tcis = new SerialTrafficControlScaffold();
        System.out.println("instance "+SerialTrafficController.instance());
		t = new SerialTurnout("PT4","t4");
	}

	public int numListeners() { return tcis.numListeners(); }

	public void checkThrownMsgSent() {
                
//                tcis.sendSerialMessage(tcis.nextWrite(), null); // force outbound message; normally done by poll loop
//		Assert.assertTrue("message sent", tcis.outbound.size()>0);
//		Assert.assertEquals("content", "41 54 08", tcis.outbound.elementAt(tcis.outbound.size()-1).toString());  // THROWN message
	}

	public void checkClosedMsgSent() {
//                tcis.sendSerialMessage(tcis.nextWrite(), null); // force outbound message; normally done by poll loop
//		Assert.assertTrue("message sent", tcis.outbound.size()>0);
//		Assert.assertEquals("content", "41 54 00", tcis.outbound.elementAt(tcis.outbound.size()-1).toString());  // CLOSED message
	}

	// from here down is testing infrastructure

	public SerialTurnoutTest(String s) {
		super(s);
	}

	// Main entry point
	static public void main(String[] args) {
		String[] testCaseName = {SerialTurnoutTest.class.getName()};
		junit.swingui.TestRunner.main(testCaseName);
	}

	// test suite from all defined tests
	public static Test suite() {
		TestSuite suite = new TestSuite(SerialTurnoutTest.class);
		return suite;
	}

	 static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(SerialTurnoutTest.class.getName());

}
