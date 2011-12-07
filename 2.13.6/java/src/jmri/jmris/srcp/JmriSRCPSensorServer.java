//JmriSRCPSensorServer.java

package jmri.jmris.srcp;

import java.io.*;

import jmri.Sensor;
import jmri.InstanceManager;
import jmri.jmris.AbstractSensorServer;

/**
 * SRCP Server interface between the JMRI Sensor manager and a
 * network connection
 * @author          Paul Bender Copyright (C) 2011
 * @version         $Revision$
 */

public class JmriSRCPSensorServer extends AbstractSensorServer {

   private DataOutputStream output;

   public JmriSRCPSensorServer(DataInputStream inStream,DataOutputStream outStream){
        output=outStream;
    }


    /*
     * Protocol Specific Abstract Functions
     */

     public void sendStatus(String sensorName,int Status) throws IOException
     {
        int bus=0;
	int address=0;
        java.util.List<Object> list
            = jmri.InstanceManager.getList(jmri.jmrix.SystemConnectionMemo.class);
        for (Object memo: list)
        {
	   String prefix=((jmri.jmrix.SystemConnectionMemo) memo).getSystemPrefix();
	   if(sensorName.startsWith(prefix)){
		try{
                    address=Integer.parseInt(sensorName.substring(prefix.length()+1));
		   break;
                } catch(NumberFormatException ne){
			// we expect this if the prefix doesn't match
                }
           }
           bus++;
        }

	if(bus>list.size()){
 	   output.writeBytes("499 ERROR unspecified error\n\r");
	   return;
	}
	
        if(Status==Sensor.ACTIVE){
		output.writeBytes("100 INFO" + bus + " FB " + address + " 1\n\r");
        } else if (Status==Sensor.INACTIVE){
		output.writeBytes("100 INFO" + bus + " FB " + address + " 0\n\r");
        } else {
               //  unknown state
		output.writeBytes("416 ERROR no data\n\r");
        }

     }

     public void sendStatus(int bus, int address) throws IOException
     {
       log.debug("send Status called with bus " +bus +" and address " +address);
       java.util.List<Object> list
            = jmri.InstanceManager.getList(jmri.jmrix.SystemConnectionMemo.class);
       Object memo=null;
       try {
         memo = list.get(bus-1);
       } catch( java.lang.IndexOutOfBoundsException obe) {
         output.writeBytes("412 ERROR wrong value\n\r");
         return;
       }
       String sensorName=((jmri.jmrix.SystemConnectionMemo)memo).getSystemPrefix()
                          + "S" + address;
       int Status=InstanceManager.sensorManagerInstance().provideSensor(sensorName).getKnownState();
        if(Status==Sensor.ACTIVE){
		output.writeBytes("100 INFO" + bus + " FB " + address + " 1\n\r");
        } else if (Status==Sensor.INACTIVE){
		output.writeBytes("100 INFO" + bus + " FB " + address + " 0\n\r");
        } else {
               //  unknown state
		output.writeBytes("416 ERROR no data\n\r");
        }
     }

     public void sendErrorStatus(String sensorName) throws IOException {
 	output.writeBytes("499 ERROR unspecified error\n\r");
     }

     public void parseStatus(String statusString) throws jmri.JmriException,java.io.IOException {
	  output.writeBytes("499 ERROR unspecified error\n\r");
     }

   /*
    * for SRCP, we're doing the parsing elsewhere, so we just need to build
    * the correct string from the provided compoents.
    */
    public void parseStatus(int bus,int address,int value) throws jmri.JmriException,java.io.IOException {
       log.debug("parse Status called with bus " +bus +" address " +address
                 + " and value " +value);
       java.util.List<Object> list
            = jmri.InstanceManager.getList(jmri.jmrix.SystemConnectionMemo.class);
       Object memo;
       try {
         memo = list.get(bus-1);
       } catch( java.lang.IndexOutOfBoundsException obe) {
         output.writeBytes("412 ERROR wrong value\n\r");
         return;
       }
       String sensorName=((jmri.jmrix.SystemConnectionMemo)memo).getSystemPrefix()
                          + "S" + address;
	    if(value==0){
                   if(log.isDebugEnabled())
                      log.debug("Setting Sensor INACTIVE");
                   setSensorInactive(sensorName);
            } else if(value==1){
                   if(log.isDebugEnabled())
                      log.debug("Setting Sensor ACTIVE");
                   setSensorActive(sensorName);
            }
            //sendStatus(bus,address);
    }

    // update state as state of sensor changes
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        // If the Commanded State changes, show transition state as "<inconsistent>"
        if (e.getPropertyName().equals("KnownState")) {
            try {
               String Name=((jmri.Sensor)e.getSource()).getSystemName();
               java.util.List<Object> List=jmri.InstanceManager.getList(jmri.jmrix.SystemConnectionMemo.class);
               int i=0;
               int address;
               for(Object memo : List){
                   String prefix=((jmri.jmrix.SystemConnectionMemo)memo).getClass().getName();
                   if(Name.startsWith(prefix))
                   {
                      address=Integer.parseInt(Name.substring(prefix.length()));
                      sendStatus(i,address);
                      break;
                   }
                   i++;
               }
            } catch(java.io.IOException ie) {
                  log.error("Error Sending Status");
            }
        }
     }


    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JmriSRCPSensorServer.class.getName());

}
