package jmri.jmrix.srcp.configurexml;

import jmri.jmrix.srcp.SRCPTurnoutManager;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides load and store functionality for configuring SRCPTurnoutManagers.
 * <P>
 * Uses the store method from the abstract base class, but provides a load
 * method here.
 *
 * @author Bob Jacobsen Copyright: Copyright (c) 2008
 * @version $Revision$
 */
public class SRCPTurnoutManagerXml extends jmri.managers.configurexml.AbstractTurnoutManagerConfigXML {

    public SRCPTurnoutManagerXml() {
        super();
    }

    public void setStoreElementClass(Element turnouts) {
        turnouts.setAttribute("class", this.getClass().getName());
    }

    public void load(Element element, Object o) {
        log.error("Invalid method called");
    }

    public boolean load(Element turnouts) {
        // create the master object
        SRCPTurnoutManager.instance();
        // load individual turnouts
        return loadTurnouts(turnouts);
    }

    // initialize logging
    static Logger log = LoggerFactory.getLogger(SRCPTurnoutManagerXml.class.getName());
}
