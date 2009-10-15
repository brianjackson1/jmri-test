// InputBitsTest.java

package jmri.jmrix.maple;

import jmri.Sensor;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit tests for the InputBits class
 * @author		Dave Duchamp  2009
 * @version		$Revision: 1.2 $
 */
public class InputBitsTest extends TestCase {
		
    private InputBits ibit = new InputBits();
       
    public void testConstructor1() {
        Assert.assertNotNull("check instance", ibit.instance());
    }

    public void testAccessors() {
        ibit.setNumInputBits(72);
		ibit.setTimeoutTime(1500);
        Assert.assertEquals("check numInputBits", 72, ibit.getNumInputBits());
        Assert.assertEquals("check timeoutTime", 1500, ibit.getTimeoutTime());
    }
	
    public void testMarkChangesInitial() {
        SerialSensor s1 = new SerialSensor("KS1","a");
        Assert.assertEquals("check bit number",1,SerialAddress.getBitFromSystemName("KS1"));
        SerialSensor s2 = new SerialSensor("KS2","ab");
        SerialSensor s3 = new SerialSensor("KS3","abc");
		SerialSensor s6 = new SerialSensor("KS6","abcd");
        ibit.registerSensor(s1, 0);
        ibit.registerSensor(s2, 1);
        ibit.registerSensor(s3, 2);
		ibit.registerSensor(s6, 5);
        Assert.assertEquals("check lastUsedSensor", 5, ibit.getLastSensor());
        // from UNKNOWN, 1st poll goes to new state
        SerialReply r = new SerialReply();
        // set reply all zero
        for (int i=5; i<5+48; i++) r.setElement(i, '0');

        // and interpret
        ibit.markChanges(r);
        ibit.makeChanges();

        Assert.assertEquals("check s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("check s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("check s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("check s6", Sensor.INACTIVE, s6.getKnownState());
    }
	
    public void testForceUnknown() {
        SerialSensor s1 = new SerialSensor("KS1","a");
        SerialSensor s2 = new SerialSensor("KS2","ab");
        SerialSensor s3 = new SerialSensor("KS3","abc");
		SerialSensor s6 = new SerialSensor("KS6","abcd");
        ibit.registerSensor(s1, 0);
        ibit.registerSensor(s2, 1);
        ibit.registerSensor(s3, 2);
		ibit.registerSensor(s6, 5);
        SerialReply r = new SerialReply();
        // set reply all zero
        for (int i=5; i<5+48; i++) r.setElement(i, '0');
        ibit.markChanges(r);
        ibit.makeChanges();
        Assert.assertEquals("check inactive s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("check inactive s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("check inactive s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("check inactive s6", Sensor.INACTIVE, s6.getKnownState());
		// force all sensors to unknown state
		ibit.forceSensorsUnknown();
        Assert.assertEquals("check unknown s1", Sensor.UNKNOWN, s1.getKnownState());
        Assert.assertEquals("check unknown s2", Sensor.UNKNOWN, s2.getKnownState());
        Assert.assertEquals("check unknown s3", Sensor.UNKNOWN, s3.getKnownState());
        Assert.assertEquals("check unknown s6", Sensor.UNKNOWN, s6.getKnownState());
	}
		
    public void testMarkChangesDebounce() {
        SerialSensor s1 = new SerialSensor("KS1","a");
        SerialSensor s2 = new SerialSensor("KS2","ab");
        SerialSensor s3 = new SerialSensor("KS3","abc");
        SerialSensor s4 = new SerialSensor("KS4","abcd");
        ibit.registerSensor(s1, 0);
        ibit.registerSensor(s2, 1);
        ibit.registerSensor(s3, 2);
        ibit.registerSensor(s4, 3);
        // from UNKNOWN, 1st poll goes to new state
        SerialReply r = new SerialReply();
        // set reply all zero
        for (int i=5; i<5+48; i++) r.setElement(i, '0');
        // and interpret
        ibit.markChanges(r);
		ibit.makeChanges();
        Assert.assertEquals("poll0 s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("poll0 s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("poll0 s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("poll0 s4", Sensor.INACTIVE, s4.getKnownState());
		
        // check out OR ing of bits among different panels
        ibit.markChanges(r);
		r.setElement(6, '1');
        ibit.markChanges(r);
		r.setElement(6, '0');
        ibit.markChanges(r);
		ibit.makeChanges();
        Assert.assertEquals("poll1 s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("poll1 s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("poll1 s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("poll1 s4", Sensor.INACTIVE, s4.getKnownState());
        ibit.markChanges(r);
		r.setElement(6, '1');
        ibit.markChanges(r);
		r.setElement(6, '0');
        ibit.markChanges(r);
		ibit.makeChanges();
        Assert.assertEquals("poll10 s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("poll10 s2", Sensor.ACTIVE, s2.getKnownState());
        Assert.assertEquals("poll10 s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("poll10 s4", Sensor.INACTIVE, s4.getKnownState());
        // single poll shouldn't change
        ibit.markChanges(r);
        ibit.markChanges(r);
        ibit.markChanges(r);
		ibit.makeChanges();
        Assert.assertEquals("poll2 s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("poll2 s2", Sensor.ACTIVE, s2.getKnownState());
        Assert.assertEquals("poll2 s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("poll2 s4", Sensor.INACTIVE, s4.getKnownState());
        // 2nd poll should, but only if same
        r.setElement(5, '1');  
        r.setElement(6, '0');  
        r.setElement(7, '1');  
        ibit.markChanges(r);
        r.setElement(5, '0');  
        r.setElement(7, '0');  		
        ibit.markChanges(r);
        ibit.markChanges(r);
		ibit.makeChanges();
        Assert.assertEquals("poll3 s1", Sensor.INACTIVE, s1.getKnownState());
        Assert.assertEquals("poll3 s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("poll3 s3", Sensor.INACTIVE, s3.getKnownState());
        Assert.assertEquals("poll3 s4", Sensor.INACTIVE, s4.getKnownState());
        // 3rd poll changes last two
        r.setElement(5, '1');  
        r.setElement(6, '0');  
        r.setElement(7, '1');  
        ibit.markChanges(r);
        r.setElement(5, '0');  
        r.setElement(7, '0');  		
        ibit.markChanges(r);
        ibit.markChanges(r);
		ibit.makeChanges();
        Assert.assertEquals("poll4 s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("poll4 s1", Sensor.ACTIVE, s1.getKnownState());
        Assert.assertEquals("poll4 s2", Sensor.INACTIVE, s2.getKnownState());
        Assert.assertEquals("poll4 s3", Sensor.ACTIVE, s3.getKnownState());
        Assert.assertEquals("poll4 s4", Sensor.INACTIVE, s4.getKnownState());
    }
	
    // from here down is testing infrastructure
    public InputBitsTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
        String[] testCaseName = {"-noloading", InputBitsTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(InputBitsTest.class);
        return suite;
    }

    // The minimal setup for log4J
    protected void setUp() { apps.tests.Log4JFixture.setUp(); }
    protected void tearDown() { apps.tests.Log4JFixture.tearDown(); }

}
