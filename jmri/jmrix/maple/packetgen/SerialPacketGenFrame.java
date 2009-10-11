// SerialPacketGenFrame.java

package jmri.jmrix.maple.packetgen;

import jmri.util.StringUtil;
import jmri.jmrix.maple.SerialMessage;
import jmri.jmrix.maple.SerialReply;
import jmri.jmrix.maple.SerialTrafficController;
import jmri.jmrix.maple.InputBits;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * Frame for user input of serial messages
 * @author	Bob Jacobsen   Copyright (C) 2002, 2003
 * @version	$Revision: 1.3 $
 */
public class SerialPacketGenFrame extends jmri.util.JmriJFrame implements jmri.jmrix.maple.SerialListener {

    // member declarations
    javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
    javax.swing.JButton sendButton = new javax.swing.JButton();
    javax.swing.JTextField packetTextField = new javax.swing.JTextField(12);

    javax.swing.JButton pollButton = new javax.swing.JButton("Send poll");
    javax.swing.JTextField uaAddrField = new javax.swing.JTextField(5);

    public SerialPacketGenFrame() {
        super();
    }

    public void initComponents() throws Exception {
        // the following code sets the frame's initial state

        jLabel1.setText("Command:");
        jLabel1.setVisible(true);

        sendButton.setText("Send");
        sendButton.setVisible(true);
        sendButton.setToolTipText("Send packet");

        packetTextField.setText("");
        packetTextField.setToolTipText("Enter command as hexadecimal bytes separated by a space");
        packetTextField.setMaximumSize(
                                       new Dimension(packetTextField.getMaximumSize().width,
                                                     packetTextField.getPreferredSize().height
                                                     )
                                       );

        setTitle("Send Maple serial command");
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        getContentPane().add(jLabel1);
        getContentPane().add(packetTextField);
        getContentPane().add(sendButton);


        sendButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    sendButtonActionPerformed(e);
                }
            });

        getContentPane().add(new JSeparator(JSeparator.HORIZONTAL));

        // add poll message buttons
        JPanel pane3 = new JPanel();
        pane3.setLayout(new FlowLayout());
        pane3.add(new JLabel("UA:"));
        pane3.add(uaAddrField);
        pane3.add(pollButton);
        uaAddrField.setText("0");
        uaAddrField.setToolTipText("Enter node address (decimal integer)");
        getContentPane().add(pane3);

        pollButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    pollButtonActionPerformed(e);
                }
            });
        pollButton.setToolTipText("Send poll request (first command only, if multiple commands)");

        // pack for display
        pack();
    }

    public void pollButtonActionPerformed(java.awt.event.ActionEvent e) {
		int endAddr = InputBits.instance().getNumInputBits();
		if (endAddr>99) endAddr = 99;
        SerialMessage msg = SerialMessage.getPoll(Integer.valueOf(uaAddrField.getText()).intValue(),1,endAddr);
        SerialTrafficController.instance().sendSerialMessage(msg, this);
    }

    public void sendButtonActionPerformed(java.awt.event.ActionEvent e) {
        SerialTrafficController.instance().sendSerialMessage(createPacket(packetTextField.getText()), this);
    }

    SerialMessage createPacket(String s) {
        // gather bytes in result
        byte b[] = StringUtil.bytesFromHexString(s);
        if (b.length == 0) return null;  // no such thing as a zero-length message
        SerialMessage m = new SerialMessage(b.length);
        for (int i=0; i<b.length; i++) m.setElement(i, b[i]);
        return m;
    }

    public void  message(SerialMessage m) {}  // ignore replies
    public void  reply(SerialReply r) {} // ignore replies
}
