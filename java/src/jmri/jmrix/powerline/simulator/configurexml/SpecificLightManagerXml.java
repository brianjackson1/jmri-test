// SpecificLightManagerXml.java
package jmri.jmrix.powerline.simulator.configurexml;

/**
 * Simple class to allow configurexml to locate a persistance class for
 * {@link jmri.jmrix.powerline.SerialLightManager} and its subclasses.
 * <p>
 * This extends {@link jmri.jmrix.powerline.configurexml.SerialLightManagerXml}
 * without changing any of its behavior. This lets the configuration system
 * locate a mirror class for
 * {@link jmri.jmrix.powerline.cp290.SpecificLightManager}, but the actual
 * loading and storing will be done by
 * {@link jmri.jmrix.powerline.configurexml.SerialLightManagerXml} in terms of
 * which-ever manager object is currently installed.
 *
 * @author Bob Jacobsen 2008
 * @author Ken Cameron 2011
 * @version $Revision$
 */
public class SpecificLightManagerXml extends jmri.jmrix.powerline.configurexml.SerialLightManagerXml {

}
