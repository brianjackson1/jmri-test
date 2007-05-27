// NceTrafficController.java

package jmri.jmrix.nce;

import jmri.jmrix.AbstractMRListener;
import jmri.jmrix.AbstractMRMessage;
import jmri.jmrix.AbstractMRReply;
import jmri.jmrix.AbstractMRTrafficController;
//import jmri.jmrix.nce.serialdriver.SerialDriverAdapter;

/**
 * Converts Stream-based I/O to/from NCE messages.  The "NceInterface"
 * side sends/receives message objects.
 * <P>
 * The connection to
 * a NcePortController is via a pair of *Streams, which then carry sequences
 * of characters for transmission.     Note that this processing is
 * handled in an independent thread.
 * <P>
 * This handles the state transistions, based on the
 * necessary state in each message.
 *
 * @author			Bob Jacobsen  Copyright (C) 2001
 * @version			$Revision: 1.18 $
 */
public class NceTrafficController extends AbstractMRTrafficController implements NceInterface {

	public NceTrafficController() {
        super();
    }

    // The methods to implement the NceInterface

    public synchronized void addNceListener(NceListener l) {
        this.addListener(l);
    }

    public synchronized void removeNceListener(NceListener l) {
        this.removeListener(l);
    }

	protected int enterProgModeDelayTime() {
		// we should to wait at least a second after enabling the programming track
		return 1000;
	}

    /**
     * Forward a NceMessage to all registered NceInterface listeners.
     */
    protected void forwardMessage(AbstractMRListener client, AbstractMRMessage m) {
        ((NceListener)client).message((NceMessage)m);
    }

    /**
     * Forward a NceReply to all registered NceInterface listeners.
     */
    protected void forwardReply(AbstractMRListener client, AbstractMRReply r) {
        ((NceListener)client).reply((NceReply)r);
    }

    NceSensorManager mSensorManager = null;
    public void setSensorManager(NceSensorManager m) { mSensorManager = m; }
    public NceSensorManager getSensorManager() { return mSensorManager; }
    
    
    /**
	 * Check NCE EPROM and start NCE CS accessory memory poll
	 */
	protected AbstractMRMessage pollMessage() {
		
		// Check to see if command options are valid
		if (NceMessage.commandOptionSet == false){
			if (log.isDebugEnabled())log.debug("Command options are not valid yet!!");
			return null;
		}

		// Have we checked the EPROM revision yet?
		if (pollEprom == null) {
			// No, do it this time
			// after the 1st time through, don't repeat checking the EPROM
			pollEprom = new NceEpromChecker();
			return pollEprom.NceEpromPoll();
		}

		// Start NCE memory poll for accessory states
		if (pollHandler == null)
			pollHandler = new NceTurnoutMonitor();

		// minimize impact to NCE CS
		mWaitBeforePoll = NceTurnoutMonitor.POLL_TIME; // default = 25

		return pollHandler.pollMessage();

	}

	NceEpromChecker pollEprom = null;
	NceTurnoutMonitor pollHandler = null;
    
 
    protected AbstractMRListener pollReplyHandler() {
        // First time through, handle reply by checking EPROM revision
    	if (pollHandler == null) return pollEprom;
    	else return pollHandler;
    }

    /**
     * Forward a preformatted message to the actual interface.
     */
    public void sendNceMessage(NceMessage m, NceListener reply) {
        sendMessage(m, reply);
    }

    protected void forwardToPort(AbstractMRMessage m, AbstractMRListener reply) {
        replyBinary = m.isBinary();
        replyLen = ((NceMessage)m).getReplyLen();
        super.forwardToPort(m, reply);
    }
    
    protected int replyLen;
    protected boolean replyBinary;
    protected boolean unsolicitedSensorMessageSeen = false;
    
    protected AbstractMRMessage enterProgMode() {
        return NceMessage.getProgMode();
    }
    protected AbstractMRMessage enterNormalMode() {
        return NceMessage.getExitProgMode();
    }

    /**
     * static function returning the NceTrafficController instance to use.
     * @return The registered NceTrafficController instance for general use,
     *         if need be creating one.
     */
    static public NceTrafficController instance() {
        if (self == null) {
            if (log.isDebugEnabled()) log.debug("creating a new NceTrafficController object");
            self = new NceTrafficController();
        }
        return self;
    }

    static protected NceTrafficController self = null;
    protected void setInstance() { self = this; }

    protected AbstractMRReply newReply() { 
        NceReply reply = new NceReply();
        reply.setBinary(replyBinary);
        return reply;
    }

    protected boolean endOfMessage(AbstractMRReply msg) {
        // first try boolean
        if (replyBinary) {
            if (msg.getElement(0) == 0x61 && replyLen != 16 ) {
                return msg.getNumDataElements() >= 3;
            } else if (msg.getNumDataElements() >= replyLen ) {
                return true;
            } else 
                return false;
        } else {
            // detect that the reply buffer ends with "COMMAND: " (note ending space)
            int num = msg.getNumDataElements();
            // ptr is offset of last element in NceReply
            int ptr = num-1;
            if ( (num >= 9) && 
                (msg.getElement(ptr)   == ' ') &&
                (msg.getElement(ptr-1) == ':') &&
                (msg.getElement(ptr-2) == 'D') )
                    return true;
                    
            // this got harder with the new PROM at the beginning of 2005.
            // It doesn't always send the "COMMAND: " prompt at the end
            // of each response. Try for the error message:
            else if ( (num >= 19) && 
                // don't check space,NL at end of buffer
                (msg.getElement(ptr-2)  == '*') &&
                (msg.getElement(ptr-3)  == '*') &&
                (msg.getElement(ptr-4)  == '*') &&
                (msg.getElement(ptr-5)  == '*') &&
                (msg.getElement(ptr-6)  == ' ') &&
                (msg.getElement(ptr-7)  == 'D') &&
                (msg.getElement(ptr-8)  == 'O') &&
                (msg.getElement(ptr-9)  == 'O') &&
                (msg.getElement(ptr-10) == 'T') &&
                (msg.getElement(ptr-11) == 'S') &&
                (msg.getElement(ptr-12) == 'R') )
                    return true;
            
            // otherwise, it's not the end
            return false;
        }
    }
    
    static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(NceTrafficController.class.getName());
}


/* @(#)NceTrafficController.java */





