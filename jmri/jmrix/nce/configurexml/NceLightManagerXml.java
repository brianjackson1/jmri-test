// NceLightManagerXml.java

package jmri.jmrix.nce.configurexml;

import org.jdom.Element;
import jmri.jmrix.nce.*;

/**
 * Provides load and store functionality for
 * configuring NceLightManagers.
 * <P>
 * Uses the store method from the abstract base class, but
 * provides a load method here.
 * <P>
 * @author Dave Duchamp Copyright (c) 2010
 * @version $Revision: 1.1 $
 */
public class NceLightManagerXml extends jmri.managers.configurexml.AbstractLightManagerConfigXML {

    public NceLightManagerXml() {
        super();
    }

    public void setStoreElementClass(Element lights) {
        lights.setAttribute("class","jmri.jmrix.nce.configurexml.NceLightManagerXml");
    }

    public void load(Element element, Object o) {
        log.error("Invalid method called");
    }

    public boolean load(Element lights) {
        // load individual lights
        return loadLights(lights);
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NceLightManagerXml.class.getName());
}