// SerialNodeTest.java

package jmri.jmrix.cmri.serial;

import jmri.*;

import junit.framework.Test;
import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import jmri.jmrix.cmri.serial.SerialReply;

/**
 * JUnit tests for the SerialNode class
 * @author			Bob Jacobsen
 * @version			$Revision: 1.1 $
 */
public class SerialNodeTest extends TestCase {

    public void testMarkChanges() {
        SerialNode a = new SerialNode();
        SerialSensor s1 = new SerialSensor("1");
        SerialSensor s2 = new SerialSensor("2");
        SerialSensor s3 = new SerialSensor("3");
        a.registerSensor(s1, 0);
        a.registerSensor(s2, 1);
        a.registerSensor(s3, 2);
        SerialReply r = new SerialReply();
        a.markChanges(r);
        Assert.assertEquals("check s1", Sensor.ACTIVE, s1.getKnownState());
        Assert.assertEquals("check s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("check s3", Sensor.ACTIVE, s3.getKnownState());
    }

    // from here down is testing infrastructure
    public SerialNodeTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
        String[] testCaseName = {SerialNodeTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(SerialNodeTest.class);
        return suite;
    }

}
