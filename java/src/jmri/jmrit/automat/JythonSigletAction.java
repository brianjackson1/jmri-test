// JythonSigletAction.java
package jmri.jmrit.automat;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

/**
 * Swing action to create and register a JythonSiglet object
 *
 * @author	Bob Jacobsen Copyright (C) 2003, 2007
 * @version $Revision$
 */
public class JythonSigletAction extends AbstractAction {

    /**
     *
     */
    private static final long serialVersionUID = -7767646852999362415L;

    public JythonSigletAction(String s, JPanel who) {
        super(s);
        _who = who;
    }

    JPanel _who;

    public void actionPerformed(ActionEvent e) {
        // create a SampleAutomaton
        JFileChooser fci = jmri.jmrit.XmlFile.userFileChooser("Python script files", "py");
        fci.setDialogTitle("Find desired script file");
        int retVal = fci.showOpenDialog(_who);
        // handle selection or cancel
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = fci.getSelectedFile();
            // create an object to handle script and run
            (new JythonSiglet(file.toString())).start();
        }
    }
}

/* @(#)JythonSigletAction.java */
