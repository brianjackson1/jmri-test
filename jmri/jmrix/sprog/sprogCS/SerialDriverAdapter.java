// SerialDriverAdapter.java

package jmri.jmrix.sprog.sprogCS;

import jmri.jmrix.sprog.SprogPortController;
import jmri.jmrix.sprog.SprogTrafficController;
import jmri.jmrix.sprog.SprogProgrammer;
import jmri.jmrix.sprog.SprogProgrammerManager;
import jmri.jmrix.sprog.SprogSlotManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

/**
 * Implements SerialPortAdapter for the Sprog system.
 * <P>
 * This connects
 * an Sprog command station via a serial com port.
 * Also used for the USB SPROG, which appears to the computer as a
 * serial port.
 * Normally controlled by the SerialDriverFrame class.
 * <P>
 * The current implementation only handles the 9,600 baud rate, and does
 * not use any other options at configuration time.
 *
 * @author	Andrew Crosland   Copyright (C) 2006
 * @version	$Revision: 1.8 $
 */
public class SerialDriverAdapter extends SprogPortController implements jmri.jmrix.SerialPortAdapter {

    SerialPort activeSerialPort = null;

    public String openPort(String portName, String appName)  {
        // open the port, check ability to set moderators
        try {
            // get and open the primary port
            CommPortIdentifier portID = CommPortIdentifier.getPortIdentifier(portName);
            try {
                activeSerialPort = (SerialPort) portID.open(appName, 2000);  // name of program, msec to wait
            }
            catch (PortInUseException p) {
                return handlePortBusy(p, portName, log);
            }

            // try to set it for comunication via SerialDriver
            try {
                activeSerialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            } catch (gnu.io.UnsupportedCommOperationException e) {
                log.error("Cannot set serial parameters on port "+portName+": "+e.getMessage());
                return "Cannot set serial parameters on port "+portName+": "+e.getMessage();
            }

            // set RTS high, DTR high
            activeSerialPort.setRTS(true);		// not connected in some serial ports and adapters
            activeSerialPort.setDTR(true);		// pin 1 in DIN8; on main connector, this is DTR

            // disable flow control; hardware lines used for signaling, XON/XOFF might appear in data
            //AJB removed Jan 2010: activeSerialPort.setFlowControlMode(0);

            // set timeout
            // activeSerialPort.enableReceiveTimeout(1000);
            log.debug("Serial timeout was observed as: "+activeSerialPort.getReceiveTimeout()
                      +" "+activeSerialPort.isReceiveTimeoutEnabled());

            // get and save stream
            serialStream = activeSerialPort.getInputStream();

            // purge contents, if any
            int count = serialStream.available();
            log.debug("input stream shows "+count+" bytes available");
            while ( count > 0) {
                serialStream.skip(count);
                count = serialStream.available();
            }

            // report status?
            if (log.isInfoEnabled()) {
                log.info(portName+" port opened at "
                         +activeSerialPort.getBaudRate()+" baud, sees "
                         +" DTR: "+activeSerialPort.isDTR()
                         +" RTS: "+activeSerialPort.isRTS()
                         +" DSR: "+activeSerialPort.isDSR()
                         +" CTS: "+activeSerialPort.isCTS()
                         +"  CD: "+activeSerialPort.isCD()
                         );
            }
            
            //AJB - add Sprog Traffic Controller as event listener
            try {
                activeSerialPort.addEventListener(SprogTrafficController.instance());
             } catch (TooManyListenersException e) {}
             
             // AJB - activate the DATA_AVAILABLE notifier
             activeSerialPort.notifyOnDataAvailable(true);
             
            opened = true;

        } catch (gnu.io.NoSuchPortException p) {
            return handlePortNotFound(p, portName, log);
        } catch (Exception ex) {
            log.error("Unexpected exception while opening port "+portName+" trace follows: "+ex);
            ex.printStackTrace();
            return "Unexpected error while opening port "+portName+": "+ex;
        }

        return null; // indicates OK return

    }

    public void setHandshake (int mode) {
      try {
        activeSerialPort.setFlowControlMode(mode);
      } catch (Exception ex) {
            log.error("Unexpected exception while setting COM port handshake mode trace follows: "+ex);
            ex.printStackTrace();
      }

    }

    /**
     * set up all of the other objects to operate with an Sprog command
     * station connected to this port
     */
    public void configure() {
        // connect to the traffic controller
        SprogTrafficController.instance().connectPort(this);

//        jmri.jmrix.sprog.SprogProgrammer.instance();  // create Programmer in InstanceManager
        jmri.InstanceManager.setProgrammerManager(new SprogProgrammerManager(new SprogProgrammer(), SprogProgrammerManager.OPS));

        jmri.InstanceManager.setPowerManager(new jmri.jmrix.sprog.SprogPowerManager());

        jmri.InstanceManager.setTurnoutManager(new jmri.jmrix.sprog.SprogTurnoutManager());

        jmri.InstanceManager.setSensorManager(new jmri.managers.InternalSensorManager());

//        jmri.InstanceManager.setCommandStation(new jmri.jmrix.sprog.SprogSoftCommandStation());
        // Start the command station queuing thread
        log.debug("start command station queuing thread");
        slotThread = new Thread(jmri.jmrix.sprog.SprogSlotManager.instance());
        slotThread.start();
        jmri.InstanceManager.setCommandStation(SprogSlotManager.instance());

        // start operation
        // sourceThread = new Thread(p);
        // sourceThread.start();
        //Removed by AJB Jan 2010
        //sinkThread = new Thread(SprogTrafficController.instance());
        //sinkThread.start();

        jmri.InstanceManager.setThrottleManager(new jmri.jmrix.sprog.SprogCSThrottleManager());

        jmri.jmrix.sprog.ActiveFlagCS.setActive();

    }

    private Thread sinkThread;
    private Thread slotThread;

    // base class methods for the SprogPortController interface
    public DataInputStream getInputStream() {
        if (!opened) {
            log.error("getInputStream called before load(), stream not available");
            return null;
        }
        return new DataInputStream(serialStream);
    }

    public DataOutputStream getOutputStream() {
        if (!opened) log.error("getOutputStream called before load(), stream not available");
        try {
            return new DataOutputStream(activeSerialPort.getOutputStream());
        }
     	catch (java.io.IOException e) {
            log.error("getOutputStream exception: "+e);
     	}
     	return null;
    }

    public boolean status() {return opened;}

    /**
     * Get an array of valid baud rates. This is currently only 19,200 bps
     */
    public String[] validBaudRates() {
        return new String[]{"9,600 bps","19,200 bps"};
    }

    /**
     * Since option 1 is not used for this, return an array with just a single string
     */
    public String[] validOption1() { return new String[]{""}; }

    /**
     * Option 1 not used, so return a null string.
     */
    public String option1Name() { return ""; }


    /**
     * Get an array of valid values for "option 2"; used to display valid options.
     * May not be null, but may have zero entries
     */
    public String[] validOption2() { return new String[]{""}; }

    /**
     * Get a String that says what Option 2 represents
     * May be an empty string, but will not be null
     */
    public String option2Name() { return ""; }

    private boolean opened = false;
    InputStream serialStream = null;

    static public SerialDriverAdapter instance() {
        if (mInstance == null) mInstance = new SerialDriverAdapter();
        return mInstance;
    }
    static SerialDriverAdapter mInstance = null;

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SerialDriverAdapter.class.getName());

}

/* @(#)SerialdriverAdapter.java */
