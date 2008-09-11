// NceMacroRestore.java

package jmri.jmrix.nce.consist;

import javax.swing.*;

import java.io.*;
import jmri.util.StringUtil;

import jmri.jmrix.nce.NceBinaryCommand;
import jmri.jmrix.nce.NceMessage;
import jmri.jmrix.nce.NceReply;
import jmri.jmrix.nce.NceTrafficController;


/**
 * Restores NCE consists from a text file defined by NCE.
 * 
 *  NCE file format:
 * 
 * :F500 (16 bytes per line, grouped as 8 words with space delimiters)
 * :F510 
 *   .
 *   .
 * :FAF0
 * :0000
 * 
 * The restore routine checks that each line of the file begins with the appropriate consist address.
 * 
 * @author Dan Boudreau Copyright (C) 2007
 * @version $Revision: 1.3 $
 */


public class NceConsistRestore extends Thread implements jmri.jmrix.nce.NceListener{
	
	private static final int CS_CONSIST_MEM = 0xF500;	// start of NCE CS Consist memory 
	private static final int CONSIST_LNTH = 16;		// 16 bytes per consist line
	private static final int REPLY_1 = 1;			// reply length of 1 byte expected
	private static int replyLen = 0;				// expected byte length
	private static int waiting = 0;					// to catch responses not intended for this module
	private static boolean fileValid = false;		// used to flag status messages
	
	javax.swing.JLabel textConsist = new javax.swing.JLabel();
	javax.swing.JLabel consistNumber = new javax.swing.JLabel();
	
	public void run() {

		// Get file to read from
		JFileChooser fc = new JFileChooser(jmri.jmrit.XmlFile.userFileLocationDefault());
		fc.addChoosableFileFilter(new textFilter());
		int retVal = fc.showOpenDialog(null);
		if (retVal != JFileChooser.APPROVE_OPTION)
			return; // cancelled
		if (fc.getSelectedFile() == null)
			return; // cancelled
		File f = fc.getSelectedFile();
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(f));
		} catch (FileNotFoundException e) {
			return;
		}
		
		// create a status frame
	   	JPanel ps = new JPanel();
	   	jmri.util.JmriJFrame fstatus = new jmri.util.JmriJFrame("Consist Restore");
	   	fstatus.setLocationRelativeTo(null);
	   	fstatus.setSize (200,100);
	   	fstatus.getContentPane().add (ps);
	   	
	   	ps.add (textConsist);
	   	ps.add(consistNumber);
	   	
		textConsist.setText("Consist line number:");
        textConsist.setVisible(true);
        consistNumber.setVisible(true);

		// Now read the file and check the consist address
		waiting = 0;
		fileValid = false;					// in case we break out early
		int consistNum = 0;					// for user status messages
		int curConsist = CS_CONSIST_MEM;	// load the start address of the NCE consist memory
		byte[] consistData = new byte[CONSIST_LNTH]; 	// NCE Consist data
		String line = " ";

		while (line != null) {
			try {
				line = in.readLine();
			} catch (IOException e) {
				break;
			}
			
			consistNumber.setText(Integer.toString(consistNum++));
			
			if (line == null){				// while loop does not break out quick enough
				log.error("NCE consist file terminator :0000 not found");
				break;
			}
			if (log.isDebugEnabled()) {
				log.debug("consist " + line);
			}
			// check that each line contains the NCE memory address of the consist
			String consistAddr = ":" + Integer.toHexString(curConsist);
			String[] consistLine = line.split(" ");

			// check for end of consist terminator
			if (consistLine[0].equalsIgnoreCase(":0000")) {
				fileValid = true; // success!
				break;
			}

			if (!consistAddr.equalsIgnoreCase(consistLine[0])) {
				log.error("Restore file selected is not a vaild backup file");
				log.error("Consist memory address in restore file should be " + consistAddr
						+ " Consist address read " + consistLine[0]);
				break;
			}

			// consist file found, give the user the choice to continue
			if (curConsist == CS_CONSIST_MEM) {
				if (JOptionPane
						.showConfirmDialog(
								null,
								"Restore file found!  Restore can take over a minute, continue?",
								"NCE Consist Restore", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
					break;
				}
			}
			
			fstatus.setVisible (true);

			// now read the entire line from the file and create NCE message
			for (int i = 0; i < 8; i++) {
				int j = i << 1;				// i = word index, j = byte index

				byte b[] = StringUtil.bytesFromHexString(consistLine[i + 1]);

				consistData[j] = b[0];
				consistData[j + 1] = b[1];
			}

			NceMessage m = writeNceConsistMemory(curConsist, consistData);
			NceTrafficController.instance().sendNceMessage(m, this);

			curConsist += CONSIST_LNTH;

			// wait for write to NCE CS to complete
			if (waiting > 0) {
				synchronized (this) {
					try {
						wait(20000);
					} catch (InterruptedException e) {
					}
				}
			}
			// failed
			if (waiting > 0){
				log.error("timeout waiting for reply");
				break;
			}
		}
		try {
			in.close();
		} catch (IOException e) {
		}

		// kill status panel
		fstatus.dispose();

		if (fileValid) {
			JOptionPane.showMessageDialog(null, "Successful Restore!",
					"NCE Consist", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(null,
					"Restore failed. Check console for error messages. \r\n" +
					"If operating at 19,200 baud, try 9600 baud.",
					"NCE Consist", JOptionPane.ERROR_MESSAGE);
		}
	}

	// writes 16 bytes of NCE consist memory
	private static NceMessage writeNceConsistMemory(int curConsist, byte[] b) {

		replyLen = REPLY_1; // Expect 1 byte response
		waiting++;
		byte[] bl;

			bl = NceBinaryCommand.accMemoryWriteN(curConsist, 16);
			int j = bl.length - 16;
			for (int i = 0; i < 16; i++, j++)
				bl[j] = b[i];

		NceMessage m = NceMessage.createBinaryMessage(bl, REPLY_1);
		return m;
	}

	public void message(NceMessage m) {
	} // ignore replies

	public void reply(NceReply r) {
		if (log.isDebugEnabled()) {
			log.debug("waiting for " + waiting + " responses ");
		}
		if (waiting <= 0) {
			log.error("unexpected response");
			return;
		}
		waiting--;
		if (r.getNumDataElements() != replyLen) {
			log.error("reply length incorrect");
			return;
		}
		if (replyLen == REPLY_1) {
			// Looking for proper response
			int recChar = r.getElement(0);
			if (recChar != '!')
				log.error("reply incorrect");
		}
		
		// wake up restore thread
		if (waiting == 0) {
			synchronized (this) {
				notify();
			}
		}
	}
	
	private class textFilter extends javax.swing.filechooser.FileFilter {
		
		public boolean accept(File f){
			if (f.isDirectory())
			return true;
			String name = f.getName();
			if (name.matches(".*\\.txt"))
				return true;
			else
				return false;
		}
		
		public String getDescription() {
			return "Text Documents (*.txt)";
		}
	}

	static org.apache.log4j.Category log = org.apache.log4j.Category
			.getInstance(NceConsistRestore.class.getName());
}
