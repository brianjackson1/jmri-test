// SE8cSignalHead.java

package jmri.implementation;

import jmri.InstanceManager;
import jmri.Turnout;
import jmri.util.NamedBeanHandle;

/**
 * Extend jmri.SignalHead for signals implemented by an SE8c
 * <P>
 * This implementation writes out to the physical signal when
 * it's commanded to change appearance, and updates its internal state
 * when it hears commands from other places.
 * <P>To get a complete set of aspects, we assume that the
 * SE8C board has been configured such that the 4th aspect is "dark".
 * We then do flashing aspects by commanding the lit appearance to change.
 * <P>
 * We can't assume any form of numbering for Turnouts to address the digits,
 * so we take two turnout names as arguments.  As a convenience, we 
 * manage the user names if they're not already set.
 * <P>The algorithms in this class are a collaborative effort of Digitrax, Inc
 * and Bob Jacobsen.
 *
 * @author			Bob Jacobsen Copyright (C) 2002
 * @version			$Revision: 1.4 $
 */
public class SE8cSignalHead extends DefaultSignalHead {

    /**
     * Primary ctor
     * @parameter lowTO Lower-numbered Turnout reference
     * @parameter highTO higher-numbered Turnout reference
     */
    public SE8cSignalHead(NamedBeanHandle<Turnout> lowTO, 
                            NamedBeanHandle<Turnout> highTO,
                            String userName) {
        // create systemname
        super(makeSystemName(lowTO,highTO), userName);
        this.lowTurnout = lowTO;
        this.highTurnout = highTO;
        systemName = makeSystemName(lowTO,highTO);
        init();
    }

    /**
     * Primary ctor without user name
     * @parameter lowTO Lower-numbered Turnout reference
     * @parameter highTO higher-numbered Turnout reference
     */
    public SE8cSignalHead(NamedBeanHandle<Turnout> lowTO, 
                            NamedBeanHandle<Turnout> highTO) {
        // create systemname
        super(makeSystemName(lowTO,highTO));
        this.lowTurnout = lowTO;
        this.highTurnout = highTO;
        systemName = makeSystemName(lowTO,highTO);
        init();
    }

    /**
     * Ctor for specifying system name
     * @parameter lowTO Lower-numbered Turnout reference
     * @parameter highTO higher-numbered Turnout reference
     */
    public SE8cSignalHead(String sname, NamedBeanHandle<Turnout> lowTO, 
                            NamedBeanHandle<Turnout> highTO,
                            String userName) {
        // create systemname
        super(sname, userName);
        this.lowTurnout = lowTO;
        this.highTurnout = highTO;
        systemName = sname;
        init();
    }

    /**
     * Ctor for specifying system name
     * @parameter lowTO Lower-numbered Turnout reference
     * @parameter highTO higher-numbered Turnout reference
     */
    public SE8cSignalHead(String sname, NamedBeanHandle<Turnout> lowTO, 
                            NamedBeanHandle<Turnout> highTO) {
        // create systemname
        super(sname);
        this.lowTurnout = lowTO;
        this.highTurnout = highTO;
        systemName = sname;
        init();
    }

    /** 
     * Implement convention for making a system name.
     * Must pass arguments, as used before object is complete
     */
    static String makeSystemName(NamedBeanHandle<Turnout> lowTO, 
                            NamedBeanHandle<Turnout> highTO) {
        return ("IH:SE8c:\""+lowTO.getName()+"\";\""+highTO.getName()+"\"").toUpperCase();
    }
    
    /**
     * Compatibility ctor
     * @parameter pNumber number (address) of low turnout
     */
    public SE8cSignalHead(int pNumber, String userName) {
        super("LH"+pNumber, userName);
        this.lowTurnout = makeHandle(pNumber);
        this.highTurnout = makeHandle(pNumber+1);
        systemName = "LH"+pNumber;
        init();
    }
    
    /** 
     * Create a handle from a raw number.
     * Static, so can be referenced before ctor complete.
     */
    static NamedBeanHandle<Turnout> makeHandle(int i) {
        String number = ""+i;
        return new NamedBeanHandle<Turnout>(
            number,
            InstanceManager.turnoutManagerInstance().provideTurnout(number)
        );
    }
    
    /**
     * Compatibility ctor
     * @parameter pNumber number (address) of low turnout
     */
    public SE8cSignalHead(int pNumber) {
        super("LH"+pNumber);
        this.lowTurnout = makeHandle(pNumber);
        this.highTurnout = makeHandle(pNumber+1);
        systemName = "LH"+pNumber;
        init();
    }

    NamedBeanHandle<Turnout> lowTurnout;
    NamedBeanHandle<Turnout> highTurnout;

    void init() {
        // basic operation, nothing but ON messages needed
        lowTurnout.getBean().setBinaryOutput(true);
        highTurnout.getBean().setBinaryOutput(true);

        // ensure default appearance
        mAppearance = DARK;  // start turned off
        updateOutput();
    }

    public String getSystemName() { 
        return systemName;
    }
    String systemName;
    
    // Handle a request to change state by sending a LocoNet command
    protected void updateOutput()  {
        if (!mLit) {
            highTurnout.getBean().setCommandedState(Turnout.CLOSED);
        } else if ( !mFlashOn &&
                    ( (mAppearance == FLASHGREEN) ||
                      (mAppearance == FLASHYELLOW) ||
                      (mAppearance == FLASHRED) ) ) {
            // flash says to make output dark; 
            // flashing-but-lit is handled below
            highTurnout.getBean().setCommandedState(Turnout.CLOSED);
        } else {
            // which of the four states?
            switch (mAppearance) {
            case FLASHRED:
            case RED:
                lowTurnout.getBean().setCommandedState(Turnout.THROWN);
                break;
            case FLASHYELLOW:
            case YELLOW:
                highTurnout.getBean().setCommandedState(Turnout.THROWN);
                break;
            case FLASHGREEN:
            case GREEN:
                lowTurnout.getBean().setCommandedState(Turnout.CLOSED);
                break;
            case DARK:
                highTurnout.getBean().setCommandedState(Turnout.CLOSED);
                break;
            default:
                log.error("Invalid state request: "+mAppearance);
                return;
            }
        }
    }
    
    public NamedBeanHandle<Turnout> getLow() { return lowTurnout; }
    public NamedBeanHandle<Turnout> getHigh() { return highTurnout; }

    public void dispose() {
    }
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SE8cSignalHead.class.getName());
}

/* @(#)SE8cSignalHead.java */
