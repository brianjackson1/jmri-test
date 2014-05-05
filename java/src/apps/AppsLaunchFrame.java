// AppsLaunchFrame.java
package apps;

import apps.gui3.TabbedPreferences;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.help.SwingHelpUtilities;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import jmri.IdTagManager;
import jmri.InstanceManager;
import jmri.JmriException;
import jmri.JmriPlugin;
import jmri.NamedBeanHandleManager;
import jmri.UserPreferencesManager;
import jmri.configurexml.ConfigXmlManager;
import jmri.configurexml.swing.DialogErrorHandler;
import jmri.implementation.AbstractShutDownTask;
import jmri.jmrit.DebugMenu;
import jmri.jmrit.ToolsMenu;
import jmri.jmrit.decoderdefn.DecoderIndexFile;
import jmri.jmrit.decoderdefn.PrintDecoderListAction;
import jmri.jmrit.display.PanelMenu;
import jmri.jmrit.display.layoutEditor.BlockValueFile;
import jmri.jmrit.jython.Jynstrument;
import jmri.jmrit.jython.JynstrumentFactory;
import jmri.jmrit.jython.RunJythonScript;
import jmri.jmrit.operations.OperationsMenu;
import jmri.jmrit.revhistory.FileHistory;
import jmri.jmrit.roster.swing.RosterMenu;
import jmri.jmrit.signalling.EntryExitPairs;
import jmri.jmrit.withrottle.WiThrottleCreationAction;
import jmri.jmrix.ActiveSystemsMenu;
import jmri.jmrix.ConnectionConfig;
import jmri.jmrix.ConnectionStatus;
import jmri.jmrix.JmrixConfigPane;
import jmri.managers.DefaultIdTagManager;
import jmri.managers.DefaultShutDownManager;
import jmri.managers.DefaultUserMessagePreferences;
import jmri.plaf.macosx.Application;
import jmri.plaf.macosx.PreferencesHandler;
import jmri.plaf.macosx.QuitHandler;
import jmri.profile.Profile;
import jmri.profile.ProfileManager;
import jmri.profile.ProfileManagerDialog;
import jmri.util.FileUtil;
import jmri.util.HelpUtil;
import jmri.util.JmriJFrame;
import jmri.util.Log4JUtil;
import jmri.util.PythonInterp;
import jmri.util.SystemType;
import jmri.util.WindowMenu;
import jmri.util.iharder.dnd.FileDrop;
import jmri.util.iharder.dnd.FileDrop.Listener;
import jmri.util.swing.FontComboUtil;
import jmri.util.swing.JFrameInterface;
import jmri.util.swing.SliderSnap;
import jmri.util.swing.WindowInterface;
import jmri.web.server.WebServerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for main frame (window) of traditional-style JMRI applications
 * <P>
 * This is for launching after the system is initialized, so it does none of that.
 *
 * @author	Bob Jacobsen Copyright 2003, 2007, 2008, 2010, 2014
 * @author Dennis Miller Copyright 2005
 * @author Giorgio Terdina Copyright 2008
 * @author Matthew Harris Copyright (C) 2011
 * @version $Revision$
 */
public class AppsLaunchFrame extends jmri.util.JmriJFrame {

    static String profileFilename;

    public AppsLaunchFrame(AppsLaunchPane containedPane, String name) {
        super(name);

        // Create a WindowInterface object based on this frame (maybe pass it in?)
        JFrameInterface wi = new JFrameInterface(this);

        // Create a menu bar
        menuBar = new JMenuBar();

        // Create menu categories and add to the menu bar, add actions to menus
        createMenus(menuBar, wi, containedPane);

        setJMenuBar(menuBar);
        add(containedPane);

        // handle window close
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        // pack and center this frame
        pack();
        Dimension screen = getToolkit().getScreenSize();
        Dimension size = getSize();
        setLocation((screen.width - size.width) / 2, (screen.height - size.height) / 2);
    }


    /**
     * Create default menubar.
     * <P>
     * This does not include the development menu.
     *
     * @param menuBar
     * @param wi
     */
    protected void createMenus(JMenuBar menuBar, WindowInterface wi, AppsLaunchPane pane) {
        // the debugging statements in the following are
        // for testing startup time
        log.debug("start building menus");

        fileMenu(menuBar, wi);
        editMenu(menuBar, wi);
        toolsMenu(menuBar, wi);
        rosterMenu(menuBar, wi);
        panelMenu(menuBar, wi);
        // check to see if operations in main menu
        if (jmri.jmrit.operations.setup.Setup.isMainMenuEnabled()) {
            operationsMenu(menuBar, wi);
        }
        systemsMenu(menuBar, wi);
        scriptMenu(menuBar, wi);
        debugMenu(menuBar, wi, pane);
        menuBar.add(new WindowMenu(wi)); // * GT 28-AUG-2008 Added window menu
        helpMenu(menuBar, wi, pane);
        log.debug("end building menus");
    }

    protected void fileMenu(JMenuBar menuBar, WindowInterface wi) {
        JMenu fileMenu = new JMenu(Bundle.getMessage("MenuFile"));
        menuBar.add(fileMenu);

        fileMenu.add(new PrintDecoderListAction(Bundle.getMessage("MenuPrintDecoderDefinitions"), wi.getFrame(), false));
        fileMenu.add(new PrintDecoderListAction(Bundle.getMessage("MenuPrintPreviewDecoderDefinitions"), wi.getFrame(), true));

    }

    /**
     * Set the location of the window-specific help for the preferences pane.
     * Made a separate method so if can be overridden for application specific
     * preferences help
     */
    protected void setPrefsFrameHelp(JmriJFrame f, String l) {
        f.addHelpMenu(l, true);
    }

    Action prefsAction;
    protected void editMenu(JMenuBar menuBar, WindowInterface wi) {

        JMenu editMenu = new JMenu(Bundle.getMessage("MenuEdit"));
        menuBar.add(editMenu);

        // cut, copy, paste
        AbstractAction a;
        a = new DefaultEditorKit.CutAction();
        a.putValue(Action.NAME, Bundle.getMessage("MenuItemCut"));
        editMenu.add(a);
        a = new DefaultEditorKit.CopyAction();
        a.putValue(Action.NAME, Bundle.getMessage("MenuItemCopy"));
        editMenu.add(a);
        a = new DefaultEditorKit.PasteAction();
        a.putValue(Action.NAME, Bundle.getMessage("MenuItemPaste"));
        editMenu.add(a);

        // prefs
        prefsAction = new apps.gui3.TabbedPreferencesAction(Bundle.getMessage("MenuItemPreferences"));
        // Put prefs in Apple's prefered area on Mac OS X
        if (SystemType.isMacOSX()) {
            Application.getApplication().setPreferencesHandler(new PreferencesHandler() {
                @Override
                public void handlePreferences(EventObject eo) {
                    prefsAction.actionPerformed(null);
                }
            });
        }
        // Include prefs in Edit menu if not on Mac OS X or not using Aqua Look and Feel
        if (!SystemType.isMacOSX() || !UIManager.getLookAndFeel().isNativeLookAndFeel()) {
            editMenu.addSeparator();
            editMenu.add(prefsAction);
        }

    }

    protected void toolsMenu(JMenuBar menuBar, WindowInterface wi) {
        menuBar.add(new ToolsMenu(Bundle.getMessage("MenuTools")));
    }

    protected void operationsMenu(JMenuBar menuBar, WindowInterface wi) {
        menuBar.add(new OperationsMenu());
    }

    protected void rosterMenu(JMenuBar menuBar, WindowInterface wi) {
        menuBar.add(new RosterMenu(Bundle.getMessage("MenuRoster"), RosterMenu.MAINMENU, this));
    }

    protected void panelMenu(JMenuBar menuBar, WindowInterface wi) {
        menuBar.add(PanelMenu.instance());
    }

    /**
     * Show only active systems in the menu bar.
     * <P>
     * Alternately, you might want to do
     * <PRE>
     *    menuBar.add(new jmri.jmrix.SystemsMenu());
     * </PRE>
     *
     * @param menuBar
     * @param wi
     */
    protected void systemsMenu(JMenuBar menuBar, WindowInterface wi) {
        ActiveSystemsMenu.addItems(menuBar);
    }

    protected void debugMenu(JMenuBar menuBar, WindowInterface wi, AppsLaunchPane pane) {
        JMenu d = new DebugMenu(pane);

        // also add some tentative items from jmrix
        d.add(new JSeparator());
        d.add(new jmri.jmrix.pricom.PricomMenu());
        d.add(new JSeparator());

        d.add(new jmri.jmrix.jinput.treecontrol.TreeAction());
        d.add(new jmri.jmrix.libusb.UsbViewAction());

        d.add(new JSeparator());
        d.add(new RunJythonScript("RailDriver Throttle", new File("jython/RailDriver.py")));

        // also add some tentative items from webserver
        d.add(new JSeparator());
        d.add(new WebServerAction());

        d.add(new JSeparator());
        d.add(new WiThrottleCreationAction());
        menuBar.add(d);

    }

    protected void scriptMenu(JMenuBar menuBar, WindowInterface wi) {
        // temporarily remove Scripts menu; note that "Run Script"
        // has been added to the Panels menu
        // JMenu menu = new JMenu("Scripts");
        // menuBar.add(menu);
        // menu.add(new jmri.jmrit.automat.JythonAutomatonAction("Jython script", this));
        // menu.add(new jmri.jmrit.automat.JythonSigletAction("Jython siglet", this));
    }

    protected void developmentMenu(JMenuBar menuBar, WindowInterface wi) {
        JMenu devMenu = new JMenu("Development");
        menuBar.add(devMenu);
        devMenu.add(new jmri.jmrit.symbolicprog.autospeed.AutoSpeedAction("Auto-speed tool"));
        devMenu.add(new JSeparator());
        devMenu.add(new jmri.jmrit.automat.SampleAutomatonAction("Sample automaton 1"));
        devMenu.add(new jmri.jmrit.automat.SampleAutomaton2Action("Sample automaton 2"));
        devMenu.add(new jmri.jmrit.automat.SampleAutomaton3Action("Sample automaton 3"));
        //devMenu.add(new JSeparator());
        //devMenu.add(new jmri.jmrix.serialsensor.SerialSensorAction("Serial port sensors"));
    }

    protected void helpMenu(JMenuBar menuBar, WindowInterface wi, AppsLaunchPane containedPane) {
        try {

            // create menu and standard items
            JMenu helpMenu = HelpUtil.makeHelpMenu(containedPane.windowHelpID(), true);

            // tell help to use default browser for external types
            SwingHelpUtilities.setContentViewerUI("jmri.util.ExternalLinkContentViewerUI");

            // use as main help menu 
            menuBar.add(helpMenu);

        } catch (Throwable e3) {
            log.error("Unexpected error creating help.", e3);
        }

    }


    /**
     * Provide access to a place where applications can expect the configuration
     * code to build run-time buttons.
     *
     * @see apps.CreateButtonPanel
     * @return null if no such space exists
     */
    static public JComponent buttonSpace() {
        return _buttonSpace;
    }
    static JComponent _buttonSpace = null;
    static AppConfigBase prefs;

    static public AppConfigBase getPrefs() {
        return prefs;
    }

    // GUI members
    private JMenuBar menuBar;
    
    static Logger log = LoggerFactory.getLogger(AppsLaunchFrame.class.getName());
}
