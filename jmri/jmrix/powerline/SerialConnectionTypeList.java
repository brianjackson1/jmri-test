// SerialConnectionTypeList.java

package jmri.jmrix.powerline;


/**
 * Returns a list of valid Powerline Connection Types
 * <P>
 * @author      Bob Jacobsen   Copyright (C) 2010
 * @author      Kevin Dickerson    Copyright (C) 2010
 * @version	$Revision: 1.2 $
 *
 */
public class SerialConnectionTypeList  implements jmri.jmrix.ConnectionTypeList {

    public String[] getAvailableProtocolClasses() { 
        return new String[] {
              "jmri.jmrix.powerline.cm11.ConnectionConfig",
              "jmri.jmrix.powerline.cp290.ConnectionConfig",
              "jmri.jmrix.powerline.insteon2412s.ConnectionConfig",
              "jmri.jmrix.powerline.simulator.ConnectionConfig"
        };
    }

}

