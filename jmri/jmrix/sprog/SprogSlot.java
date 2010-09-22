// SprogSlot.java

package jmri.jmrix.sprog;

import jmri.NmraPacket;

/**
 * Represents information for a DCC Command Station Queue entry where each entry
 * is a DCC packet to be transmitted to the rails
 * <P>
 * A SlotListener can be registered to hear of changes in this slot.  All
 * changes in values will result in notification.
 * <P>
 * @author			Andrew Crosland Copyright (C) 2006
 * @author			Andrew Berridge 2010
 * @version			$Revision: 1.10 $
 */
 public class SprogSlot {
	 
   private boolean speedPacket = false;

   public SprogSlot(int num) {
     payload = new byte[SprogConstants.MAX_PACKET_LENGTH];
     payload[0] = 0;
     payload[1] = 0;
     payload[2] = 0;
     repeat = -1;
     addr = 0;
     spd = 0;
     forward = true;
     status = SprogConstants.SLOT_FREE;
     slot = num;
     opsPkt = false;
   }

   private byte[] payload;
   // repeat of -1 is a persistent entry, ie a loco slot
   private int repeat;
   private int addr;
   private int spd;
   private boolean forward;
   private int status;
   private int slot;
   private boolean opsPkt;
   
   private boolean f0to4Packet = false;
   public boolean isF0to4Packet() {
	return f0to4Packet;
}

public boolean isF5to8Packet() {
	return f5to8Packet;
}

public boolean isF9to12Packet() {
	return f9to12Packet;
}

private boolean f5to8Packet = false;
   private boolean f9to12Packet = false;
     
   private boolean repeatF0 = false;
   private boolean repeatF1 = false;
   private boolean repeatF2 = false;
   private boolean repeatF3 = false;
   private boolean repeatF4 = false;
   private boolean repeatF5 = false;
   private boolean repeatF6 = false;
   private boolean repeatF7 = false;
   private boolean repeatF8 = false;
   private boolean repeatF9 = false;
   private boolean repeatF10 = false;
   private boolean repeatF11 = false;
   private boolean repeatF12 = false;
   

     /**
      * Set the contents of the slot. Intended for accessory packets
      *
      * @param address int
      * @param payload byte[]
      * @param repeat int
      */
     public void set(int address, byte [] payload, int repeat) {
       addr = address;

       // Arrays.copyOf(payload, payload.length), a Java 1.6 construct
       this.payload = new byte[payload.length];
       for (int i = 0; i<payload.length; i++) this.payload[i] = payload[i];
       
       this.setRepeat(repeat);
       status = SprogConstants.SLOT_IN_USE;
     }
   
     public void setAccessoryPacket(int address, boolean closed, int repeats ) {
    	 this.payload = NmraPacket.accDecoderPkt(address, closed);
    	 this.addr = address + 10000;
    	 this.repeat = repeats;
    	 status = SprogConstants.SLOT_IN_USE; 
     }
     public boolean isSpeedPacket() { return speedPacket; }
     
     public void setSpeed(int address, int speed, boolean forward) {
    	 addr = address;
    	 spd = speed;
    	 this.speedPacket = true;
    	 this.f0to4Packet = false;
    	 this.forward = forward;
    	 this.payload = jmri.NmraPacket.speedStep128Packet(address,
    	          (address >= SprogConstants.LONG_START), spd, forward);
    	 status = SprogConstants.SLOT_IN_USE;
     }
     
     public void setOps(int address, boolean longAddr, int cv, int val) {
         payload = NmraPacket.opsCvWriteByte(address, longAddr, cv, val );
         this.repeat = SprogConstants.OPS_REPEATS;
         this.opsPkt = true;
         status = SprogConstants.SLOT_IN_USE; 
     }
     
     public void f5to8packet(int address, 
    		 boolean f5, boolean f5Momentary,
    		 boolean f6, boolean f6Momentary,
    		 boolean f7, boolean f7Momentary,
    		 boolean f8, boolean f8Momentary) {
    	 
    	 this.f5to8Packet = true;
    	 this.addr = address;
    	 
    	 //Were we repeating any functions which we are now not?
    	 if ((this.repeatF5 && !f5) 
    			 || (this.repeatF6 && !f6)
    			 || (this.repeatF7 && !f7) 
    			 || (this.repeatF8 && !f8)) {
    		 this.repeat = 3; //Then repeat 3 times
    	 }
    	 
    	 if (!f5Momentary && f5) {
    		 this.repeatF5 = true;
    	 } else this.repeatF5 = false;
    	 if (!f6Momentary && f6) {
    		 this.repeatF6 = true;
    	 } else this.repeatF6 = false;
    	 if (!f7Momentary && f7) {
    		 this.repeatF7 = true;
    	 } else this.repeatF7 = false;
    	 if (!f8Momentary && f8) {
    		 this.repeatF8 = true;
    	 } else this.repeatF8 = false;
    	 
   
    	 this.payload = jmri.NmraPacket.function5Through8Packet(address, 
    			 (address >= SprogConstants.LONG_START),
    			 f5, f6, f7, f8);
    	 this.status = SprogConstants.SLOT_IN_USE;
    	 
     }
     
     public void f9to12packet(int address, 
    		 boolean f9, boolean f9Momentary,
    		 boolean f10, boolean f10Momentary,
    		 boolean f11, boolean f11Momentary,
    		 boolean f12, boolean f12Momentary) {
    	 
    	 this.f9to12Packet = true;
    	 this.addr = address;
    	 
    	 //Were we repeating any functions which we are now not?
    	 if ((this.repeatF9 && !f9) 
    			 || (this.repeatF10 && !f10)
    			 || (this.repeatF11 && !f11) 
    			 || (this.repeatF12 && !f12) ) {
    		 this.repeat = 3; //Then repeat 3 times
    	 }
    	 
    	 if (!f9Momentary && f9) {
    		 this.repeatF9 = true;
    	 } else this.repeatF9 = false;
    	 if (!f10Momentary && f10) {
    		 this.repeatF10 = true;
    	 } else this.repeatF10 = false;
    	 if (!f11Momentary && f11) {
    		 this.repeatF11 = true;
    	 } else this.repeatF11 = false;
    	 if (!f12Momentary && f12) {
    		 this.repeatF12 = true;
    	 } else this.repeatF12 = false;

    	 this.payload = jmri.NmraPacket.function9Through12Packet(address, 
    			 (address >= SprogConstants.LONG_START),
    			 f9, f10, f11, f12);
    	 this.status = SprogConstants.SLOT_IN_USE;
    	 
     }
     public void f0to4packet(int address, 
    		 boolean f0, boolean f0Momentary,
    		 boolean f1, boolean f1Momentary,
    		 boolean f2, boolean f2Momentary,
    		 boolean f3, boolean f3Momentary,
    		 boolean f4, boolean f4Momentary) {
    	 
    	 this.f0to4Packet = true;
    	 this.addr = address;
    	 
    	 //Were we repeating any functions which we are now not?
    	 if ((this.repeatF0 && !f0) 
    			 || (this.repeatF1 && !f1)
    			 || (this.repeatF2 && !f2) 
    			 || (this.repeatF3 && !f3) 
    			 || (this.repeatF4 && !f4)) {
    		 this.repeat = 3; //Then repeat 3 times
    	 }
    	 
    	 if (!f0Momentary && f0) {
    		 this.repeatF0 = true;
    	 } else this.repeatF0 = false;
    	 if (!f1Momentary && f1) {
    		 this.repeatF1 = true;
    	 } else this.repeatF1 = false;
    	 if (!f2Momentary && f2) {
    		 this.repeatF2 = true;
    	 } else this.repeatF2 = false;
    	 if (!f3Momentary && f3) {
    		 this.repeatF3 = true;
    	 } else this.repeatF3 = false;
    	 if (!f4Momentary && f4) {
    		 this.repeatF4 = true;
    	 } else this.repeatF4 = false;
    	 this.payload = jmri.NmraPacket.function0Through4Packet(address, 
    			 (address >= SprogConstants.LONG_START),
    			 f0, f1, f2, f3, f4);
    	 this.status = SprogConstants.SLOT_IN_USE;
    	 
     }
     
     public boolean isFinished() {
    	 if (this.isF0to4Packet()) {
    		 if ((this.repeatF0 || this.repeatF1 || this.repeatF2 || this.repeatF3 || this.repeatF4)) return false;
    	 }
    	 if (this.isF5to8Packet()) {
    		 if ((this.repeatF5 || this.repeatF6 || this.repeatF7 || this.repeatF8 )) return false;
    	 }
    	 if (this.isF9to12Packet()) {
    		 if ((this.repeatF9 || this.repeatF10 || this.repeatF11 || this.repeatF12)) return false;
    	 }
    	 if (this.isSpeedPacket() && this.status == SprogConstants.SLOT_IN_USE) return false;
    	 if (this.repeat > 0 && this.status == SprogConstants.SLOT_IN_USE) return false;
    	 /* Finished - clear and return true */
    	 this.clear(); 
    	 return true;
     }
     
     public void eStop() {
    	 this.setSpeed(this.locoAddr(), 1, this.forward); 
     }

     // Access methods
     public void clear() {
       status = SprogConstants.SLOT_FREE;
       addr = 0;
       spd = 0;
       speedPacket = false;
       f0to4Packet = false;
       payload[0] = 0;
       payload[1] = 0;
       payload[2] = 0;
       opsPkt = false;
     }

     public boolean isLongAddress() { return (getAddr() >= 100); }
     public boolean isFree() { return (status == SprogConstants.SLOT_FREE); }
     public int slotStatus() { return status; }
     public int getRepeat() { return repeat; }
     public void setRepeat(int r) { repeat = r; }
     private int doRepeat() {
       if (repeat > 0) {
         log.debug("Slot "+slot+"repeats");
         repeat--;
         if (repeat == 0) {
           log.debug("Clear slot "+slot+"due to repeats exhausted");
           this.clear();
         }
       }
       return repeat;
     }
     public int speed() { return spd; }
     public int locoAddr() { return addr; }
     public int getAddr() { return addr; }
     public void setAddr(int a) { addr = a;}
     public boolean isForward() { return forward; }
     public boolean isOpsPkt() { return opsPkt; }
     
     /**
      * Get the payload of this slot. Note - if this slot has a number of repeats, calling this 
      * method will also decrement the internal repeat counter
      * @return a byte array containing the payload of this slot
      */
     public byte[] getPayload() {

       // p = Arrays.copyOf(payload, getPayloadLength()), a Java 1.6 construct
       byte [] p = new byte[getPayloadLength()];
       for (int i = 0; i<getPayloadLength(); i++) p[i] = payload[i];

       //decrement repeat counter if appropriate
       doRepeat();
       return p;
       
     }

    public int getSlotNumber() { return slot; }

    private int getPayloadLength() { return this.payload.length;}
    
    private long lastUpdateTime ; // Time of last update for detecting stale slots

    public long getLastUpdateTime() { return lastUpdateTime ; }

    /**
     * Get the address from the packet
     * @return int
     */
    @SuppressWarnings("unused")
	private int addressFromPacket() {
      if (isFree()) { return -1; }
      // First deal with possible extended address
      if ((payload[0] & 0xC0) == 0xC0) { return (payload[0]<<8 | payload[1]); }
      return payload[0];
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SprogSlot.class.getName());
}


/* @(#)SprogSlot.java */
