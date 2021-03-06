// RosterFrame.java
package jmri.jmrit.roster.swing;

import apps.AppsBase;
import apps.gui3.Apps3;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.help.SwingHelpUtilities;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import jmri.AddressedProgrammerManager;
import jmri.GlobalProgrammerManager;
import jmri.InstanceManager;
import jmri.Programmer;
import jmri.UserPreferencesManager;
import jmri.jmrit.decoderdefn.DecoderFile;
import jmri.jmrit.decoderdefn.DecoderIndexFile;
import jmri.jmrit.progsupport.ProgModeSelector;
import jmri.jmrit.progsupport.ProgServiceModeComboBox;
import jmri.jmrit.roster.CopyRosterItemAction;
import jmri.jmrit.roster.DeleteRosterItemAction;
import jmri.jmrit.roster.ExportRosterItemAction;
import jmri.jmrit.roster.IdentifyLoco;
import jmri.jmrit.roster.PrintRosterEntry;
import jmri.jmrit.roster.Roster;
import jmri.jmrit.roster.RosterEntry;
import jmri.jmrit.roster.RosterEntrySelector;
import jmri.jmrit.roster.rostergroup.RosterGroupSelector;
import jmri.jmrit.symbolicprog.ProgDefault;
import jmri.jmrit.symbolicprog.tabbedframe.PaneOpsProgFrame;
import jmri.jmrit.symbolicprog.tabbedframe.PaneProgFrame;
import jmri.jmrit.symbolicprog.tabbedframe.PaneServiceProgFrame;
import jmri.jmrit.throttle.LargePowerManagerButton;
import jmri.jmrit.throttle.ThrottleFrame;
import jmri.jmrit.throttle.ThrottleFrameManager;
import jmri.jmrix.ActiveSystemsMenu;
import jmri.jmrix.ConnectionConfig;
import jmri.jmrix.ConnectionStatus;
import jmri.profile.ProfileManager;
import jmri.progdebugger.ProgDebugger;
import jmri.util.FileUtil;
import jmri.util.HelpUtil;
import jmri.util.WindowMenu;
import jmri.util.datatransfer.RosterEntrySelection;
import jmri.util.swing.JmriAbstractAction;
import jmri.util.swing.ResizableImagePanel;
import jmri.util.swing.WindowInterface;
import jmri.util.swing.XTableColumnModel;
import jmri.util.swing.multipane.TwoPaneTBWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A window for Roster management.
 *
 * TODO: Several methods are copied from PaneProgFrame and should be refactored
 * No programmer support yet (dummy object below) Color only covering borders No
 * reset toolbar support yet No glass pane support (See DecoderPro3Panes class
 * and usage below) Special panes (Roster entry, attributes, graphics) not
 * included How do you pick a programmer file? (hardcoded) Initialization needs
 * partial deferal, too for 1st pane to appear
 *
 * This class can be extended to allow the Window title to be branded. For an
 * example, see {@link apps.gui3.dp3.DecoderPro3Window}.
 *
 * @see jmri.jmrit.symbolicprog.tabbedframe.PaneSet
 *
 * @author Bob Jacobsen Copyright (C) 2010
 * @author Kevin Dickerson Copyright (C) 2011
 * @author Randall Wood Copyright (C) 2012
 * @version $Revision: 20027 $
 */
public class RosterFrame extends TwoPaneTBWindow implements RosterEntrySelector, RosterGroupSelector {

    /**
     *
     */
    private static final long serialVersionUID = 6820971027038275933L;
    static Logger log = LoggerFactory.getLogger(RosterFrame.class.getName());
    static ArrayList<RosterFrame> frameInstances = new ArrayList<RosterFrame>();
    protected boolean allowQuit = true;
    protected String baseTitle = "Roster";
    protected JmriAbstractAction newWindowAction;

    public RosterFrame() {
        this("Roster");
    }

    public RosterFrame(String name) {
        this(name,
                "xml/config/parts/jmri/jmrit/roster/swing/RosterFrameMenu.xml",
                "xml/config/parts/jmri/jmrit/roster/swing/RosterFrameToolBar.xml");
    }

    public RosterFrame(String name, String menubarFile, String toolbarFile) {
        super(name, menubarFile, toolbarFile);
        this.allowInFrameServlet = false;
        this.setBaseTitle(name);
        this.buildWindow();
    }
    int clickDelay = 0;
    JRadioButtonMenuItem contextEdit = new JRadioButtonMenuItem(Bundle.getMessage("Edit"));
    JRadioButtonMenuItem contextOps = new JRadioButtonMenuItem(Bundle.getMessage("ProgrammingOnMain"));
    JRadioButtonMenuItem contextService = new JRadioButtonMenuItem(Bundle.getMessage("ProgrammingTrack"));
    JTextPane dateUpdated = new JTextPane();
    JTextPane dccAddress = new JTextPane();
    JTextPane decoderFamily = new JTextPane();
    JTextPane decoderModel = new JTextPane();
    JRadioButton edit = new JRadioButton(Bundle.getMessage("EditOnly"));
    JTextPane filename = new JTextPane();
    JLabel firstHelpLabel;
    //int firstTimeAddedEntry = 0x00;
    int groupSplitPaneLocation = 0;
    RosterGroupsPanel groups;
    boolean hideGroups = false;
    boolean hideRosterImage = false;
    JTextPane id = new JTextPane();
    boolean inStartProgrammer = false;
    ResizableImagePanel locoImage;
    JTextPane maxSpeed = new JTextPane();
    JTextPane mfg = new JTextPane();
    ProgModeSelector modePanel = new ProgServiceModeComboBox();
    JTextPane model = new JTextPane();
    JLabel operationsModeProgrammerLabel = new JLabel();
    JRadioButton ops = new JRadioButton(Bundle.getMessage("ProgrammingOnMain"));
    ConnectionConfig opsModeProCon = null;
    JTextPane owner = new JTextPane();
    UserPreferencesManager p;
    JButton prog1Button = new JButton(Bundle.getMessage("Program"));
    JButton prog2Button = new JButton(Bundle.getMessage("BasicProgrammer"));
    ActionListener programModeListener;
    ProgDebugger programmer = new ProgDebugger();
    String programmer1 = Bundle.getMessage("Comprehensive");
    String programmer2 = Bundle.getMessage("Basic");
    java.util.ResourceBundle rb = java.util.ResourceBundle.getBundle("apps.AppsBundle");
    //current selected loco
    RosterEntry re;
    JTextPane roadName = new JTextPane();
    JTextPane roadNumber = new JTextPane();
    JPanel rosterDetailPanel = new JPanel();
    PropertyChangeListener rosterEntryUpdateListener;
    JSplitPane rosterGroupSplitPane;
    JButton rosterMedia = new JButton(Bundle.getMessage("LabelsAndMedia"));
    RosterTable rtable;
    ConnectionConfig serModeProCon = null;
    JRadioButton service = new JRadioButton(Bundle.getMessage("ProgrammingTrack"));
    JLabel serviceModeProgrammerLabel = new JLabel();
    JLabel statusField = new JLabel();
    Dimension summaryPaneDim = new Dimension(0, 170);
    JButton throttleLabels = new JButton(Bundle.getMessage("ThrottleLabels"));
    JButton throttleLaunch = new JButton(Bundle.getMessage("Throttle"));

    void additionsToToolBar() {
        //This value may return null if the DP3 window has been called from a the traditional JMRI menu frame
        if (Apps3.buttonSpace() != null) {
            getToolBar().add(Apps3.buttonSpace());
        }
        getToolBar().add(new LargePowerManagerButton(true));
        getToolBar().add(modePanel);
    }

    /**
     * For use when the DP3 window is called from another JMRI instance, set
     * this to prevent the DP3 from shutting down JMRI when the window is
     * closed.
     */
    protected void allowQuit(boolean quitAllowed) {
        if (allowQuit != quitAllowed) {
            newWindowAction = null;
            allowQuit = quitAllowed;
            groups.setNewWindowMenuAction(this.getNewWindowAction());
        }

        firePropertyChange("quit", "setEnabled", allowQuit);
        //if we are not allowing quit, ie opened from JMRI classic
        //then we must at least allow the window to be closed
        if (!allowQuit) {
            firePropertyChange("closewindow", "setEnabled", true);
        }
    }

    JPanel bottomRight() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        ButtonGroup progMode = new ButtonGroup();
        progMode.add(service);
        progMode.add(ops);
        progMode.add(edit);
        service.setEnabled(false);
        ops.setEnabled(false);
        edit.setEnabled(true);
        firePropertyChange("setprogservice", "setEnabled", false);
        firePropertyChange("setprogops", "setEnabled", false);
        firePropertyChange("setprogedit", "setEnabled", true);
        ops.setOpaque(false);
        service.setOpaque(false);
        edit.setOpaque(false);
        JPanel progModePanel = new JPanel();
        GridLayout buttonLayout = new GridLayout(3, 1, 0, 0);
        progModePanel.setLayout(buttonLayout);
        progModePanel.add(service);
        progModePanel.add(ops);
        progModePanel.add(edit);
        programModeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProgMode();
            }
        };
        service.addActionListener(programModeListener);
        ops.addActionListener(programModeListener);
        edit.addActionListener(programModeListener);
        service.setVisible(false);
        ops.setVisible(false);
        panel.add(progModePanel);
        JPanel buttonHolder = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.ipady = 20;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = 0;
        c.insets = new Insets(2, 2, 2, 2);
        buttonHolder.add(prog1Button, c);
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.ipady = 0;
        buttonHolder.add(rosterMedia, c);
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.ipady = 0;
        buttonHolder.add(throttleLaunch, c);
        //buttonHolder.add(throttleLaunch);
        panel.add(buttonHolder);
        prog1Button.setEnabled(false);
        prog1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (log.isDebugEnabled()) {
                    log.debug("Open programmer pressed");
                }
                startProgrammer(null, re, programmer1);
            }
        });
        /*
         * prog2Button.setEnabled(false); prog2Button.addActionListener( new
         * ActionListener() { public void
         * actionPerformed(java.awt.event.ActionEvent e) { if
         * (log.isDebugEnabled()) log.debug("Open programmer pressed");
         * startProgrammer(null, re, programmer2); } });
         */
        /*
         * throttleLabels.setEnabled(false); throttleLabels.addActionListener(
         * new ActionListener() { public void
         * actionPerformed(java.awt.event.ActionEvent e) { if
         * (log.isDebugEnabled()) log.debug("Open programmer pressed");
         * editMediaButton(); } });
         */
        rosterMedia.setEnabled(false);
        rosterMedia.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (log.isDebugEnabled()) {
                    log.debug("Open programmer pressed");
                }
                edit.setSelected(true);
                startProgrammer(null, re, "dp3" + File.separator + "MediaPane");
            }
        });
        throttleLaunch.setEnabled(false);
        throttleLaunch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (log.isDebugEnabled()) {
                    log.debug("Launch Throttle pressed");
                }
                if (!checkIfEntrySelected()) {
                    return;
                }
                ThrottleFrame tf = ThrottleFrameManager.instance().createThrottleFrame();
                tf.toFront();
                tf.getAddressPanel().setRosterEntry(re);
            }
        });
        return panel;
    }

    protected final void buildWindow() {
        //Additions to the toolbar need to be added first otherwise when trying to hide bits up during the initialisation they remain on screen
        additionsToToolBar();
        frameInstances.add(this);
        p = InstanceManager.getDefault(UserPreferencesManager.class);
        getTop().add(createTop());
        getBottom().setMinimumSize(summaryPaneDim);
        getBottom().add(createBottom());
        statusBar();
        systemsMenu();
        helpMenu(getMenu(), this);
        if ((!p.getSimplePreferenceState(this.getClass().getName() + ".hideGroups")) && !Roster.instance().getRosterGroupList().isEmpty()) {
            hideGroupsPane(false);
        } else {
            hideGroupsPane(true);
        }
        if (p.getSimplePreferenceState(this.getClass().getName() + ".hideSummary")) {
            //We have to set it to display first, then we can hide it.
            hideBottomPane(false);
            hideBottomPane(true);
        }
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent changeEvent) {
                JSplitPane sourceSplitPane = (JSplitPane) changeEvent.getSource();
                String propertyName = changeEvent.getPropertyName();
                if (propertyName.equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) {
                    int current = sourceSplitPane.getDividerLocation() + sourceSplitPane.getDividerSize();
                    int panesize = (int) (sourceSplitPane.getSize().getHeight());
                    if (panesize - current <= 1) {
                        hideBottomPane = true;
                    } else {
                        hideBottomPane = false;
                        //p.setSimplePreferenceState(DecoderPro3Window.class.getName()+".hideSummary",hideSummary);
                    }
                }
            }
        };
        updateProgrammerStatus();
        ConnectionStatus.instance().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if ((e.getPropertyName().equals("change")) || (e.getPropertyName().equals("add"))) {
                    updateProgrammerStatus();
                }
            }
        });
        InstanceManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("programmermanager")) {
                    updateProgrammerStatus();
                }
            }
        });
        getSplitPane().addPropertyChangeListener(propertyChangeListener);
        if (ProgDefault.getDefaultProgFile() != null) {
            programmer1 = ProgDefault.getDefaultProgFile();
        }

        String lastProg = (String) p.getProperty(getWindowFrameRef(), "selectedProgrammer");
        if (lastProg != null) {
            if (lastProg.equals("service") && service.isEnabled()) {
                service.setSelected(true);
                updateProgMode();
            } else if (lastProg.equals("ops") && ops.isEnabled()) {
                ops.setSelected(true);
                updateProgMode();
            } else if (lastProg.equals("edit") && edit.isEnabled()) {
                edit.setSelected(true);
                updateProgMode();
            }
        }
        if (frameInstances.size() > 1) {
            firePropertyChange("closewindow", "setEnabled", true);
            allowQuit(frameInstances.get(0).isAllowQuit());
        } else {
            firePropertyChange("closewindow", "setEnabled", false);
        }
    }

    boolean checkIfEntrySelected() {
        if (re == null) {
            JOptionPane.showMessageDialog(null, "Please select a loco from the roster first");
            return false;
        }
        return true;
    }

    //@TODO The disabling of the closewindow menu item doesn't quite work as this in only invoked on the closing window, and not the one that is left
    void closeWindow(WindowEvent e) {
        saveWindowDetails();
        //Save any changes made in the roster entry details
        Roster.writeRosterFile();
        if (allowQuit && frameInstances.size() == 1) {
            handleQuit(e);
        } else {
            //As we are not the last window open or we are not allowed to quit the application then we will just close the current window
            frameInstances.remove(this);
            super.windowClosing(e);
            if ((frameInstances.size() == 1) && (allowQuit)) {
                frameInstances.get(0).firePropertyChange("closewindow", "setEnabled", false);
            }
            dispose();
        }
    }

    protected void copyLoco() {
        CopyRosterItem act = new CopyRosterItem("Copy", this, re);
        act.actionPerformed(null);
    }

    JComponent createBottom() {
        locoImage = new ResizableImagePanel(null, 240, 160);
        locoImage.setBorder(BorderFactory.createLineBorder(Color.blue));
        locoImage.setOpaque(true);
        locoImage.setRespectAspectRatio(true);
        rosterDetailPanel.setLayout(new BorderLayout());
        rosterDetailPanel.add(locoImage, BorderLayout.WEST);
        rosterDetailPanel.add(rosterDetails(), BorderLayout.CENTER);
        rosterDetailPanel.add(bottomRight(), BorderLayout.EAST);
        if (p.getSimplePreferenceState(this.getClass().getName() + ".hideRosterImage")) {
            locoImage.setVisible(false);
            hideRosterImage = true;
        }
        rosterEntryUpdateListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                updateDetails();
            }
        };
        return rosterDetailPanel;
    }

    JComponent createTop() {
        Object selectedRosterGroup = p.getProperty(getWindowFrameRef(), "selectedRosterGroup");
        groups = new RosterGroupsPanel((selectedRosterGroup != null) ? selectedRosterGroup.toString() : null);
        groups.setNewWindowMenuAction(this.getNewWindowAction());
        setTitle(groups.getSelectedRosterGroup());
        final JPanel rosters = new JPanel();
        rosters.setLayout(new BorderLayout());
        // set up roster table
        rtable = new RosterTable(true, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rtable.setRosterGroup(this.getSelectedRosterGroup());
        rtable.setRosterGroupSource(groups);
        rosters.add(rtable, BorderLayout.CENTER);
        // add selection listener
        rtable.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    if (rtable.getSelectedRosterEntries().length == 1 && rtable.getTable().getSelectedRow() >= 0) {
                        log.debug("Selected row ", rtable.getTable().getSelectedRow());
                        locoSelected(rtable.getModel().getValueAt(rtable.getTable().getSelectedRow(), RosterTableModel.IDCOL).toString());
                    } else if (rtable.getSelectedRosterEntries().length > 1 || rtable.getTable().getSelectedRow() < 0) {
                        locoSelected(null);
                    } // leave last selected item visible if no selection
                }
            }
        });

        //Set all the sort and width details of the table first.
        String rostertableref = getWindowFrameRef() + ":roster";

        //Reorder the columns first
        for (int i = 0; i < rtable.getTable().getColumnCount(); i++) {
            String columnName = p.getTableColumnAtNum(rostertableref, i);
            if (columnName != null) {
                int originalLocation = -1;
                for (int j = 0; j < rtable.getTable().getColumnCount(); j++) {
                    if (rtable.getTable().getColumnName(j).equals(columnName)) {
                        originalLocation = j;
                        break;
                    }
                }
                if (originalLocation != -1 && (originalLocation != i)) {
                    rtable.getTable().moveColumn(originalLocation, i);
                }
            }
        }

        //Set column widths, sort order and hidden status
        XTableColumnModel tcm = rtable.getXTableColumnModel();
        Enumeration<TableColumn> en = tcm.getColumns(false);
        while (en.hasMoreElements()) {
            TableColumn tc = en.nextElement();
            String columnName = (String) tc.getHeaderValue();
            if (p.getTableColumnWidth(rostertableref, columnName) != -1) {
                int width = p.getTableColumnWidth(rostertableref, columnName);
                tc.setPreferredWidth(width);
            }
            int sort = p.getTableColumnSort(rostertableref, columnName);
            rtable.getModel().setSortingStatus(tc.getModelIndex(), sort);

            if (p.getTableColumnHidden(rostertableref, columnName)) {
                tcm.setColumnVisible(tc, false);
            } else if (p.getTableColumnOrder(rostertableref, columnName) != -1) {
                //Use Column order to determine if the column has previously been saved.
                tcm.setColumnVisible(tc, true);
            }
        }

        rtable.getTable().setDragEnabled(true);
        rtable.getTable().setTransferHandler(new TransferHandler() {
            /**
             *
             */
            private static final long serialVersionUID = -5029989951536106561L;

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.COPY;
            }

            @Override
            public Transferable createTransferable(JComponent c) {
                ArrayList<String> Ids = new ArrayList<String>(rtable.getTable().getSelectedRowCount());
                for (int i = 0; i < rtable.getTable().getSelectedRowCount(); i++) {
                    Ids.add(rtable.getModel().getValueAt(rtable.getTable().getSelectedRows()[i], RosterTableModel.IDCOL).toString());
                }
                return new RosterEntrySelection(Ids);
            }

            @Override
            public void exportDone(JComponent c, Transferable t, int action) {
                // nothing to do
            }
        });
        MouseListener rosterMouseListener = new rosterPopupListener();
        rtable.getTable().addMouseListener(rosterMouseListener);
        try {
            clickDelay = ((Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval")).intValue();
        } catch (Exception e) {
            try {
                clickDelay = ((Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt_multiclick_time")).intValue();
            } catch (Exception ex) {
                clickDelay = 500;
                log.error("Unable to get the double click speed, Using JMRI default of half a second" + e.toString());
            }
        }
        /*MouseListener mouseHeaderListener = new tableHeaderListener();
         rtable.getTable().getTableHeader().addMouseListener(mouseHeaderListener);*/
        // assemble roster/groups splitpane
        rosterGroupSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, groups, rosters);
        rosterGroupSplitPane.setOneTouchExpandable(true);
        rosterGroupSplitPane.setResizeWeight(0); // emphasis rosters
        Object w = p.getProperty(getWindowFrameRef(), "rosterGroupPaneDividerLocation");
        if (w != null) {
            groupSplitPaneLocation = (Integer) w;
            rosterGroupSplitPane.setDividerLocation(groupSplitPaneLocation);
        }
        if (!Roster.instance().getRosterGroupList().isEmpty()) {
            if (p.getSimplePreferenceState(this.getClass().getName() + ".hideGroups")) {
                hideGroupsPane(true);
            }
        } else {
            enableRosterGroupMenuItems(false);
        }
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent changeEvent) {
                JSplitPane sourceSplitPane = (JSplitPane) changeEvent.getSource();
                String propertyName = changeEvent.getPropertyName();
                if (propertyName.equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) {
                    int current = sourceSplitPane.getDividerLocation();
                    if (current <= 1) {
                        hideGroups = true;
                    } else {
                        hideGroups = false;
                    }
                    Integer last = (Integer) changeEvent.getNewValue();
                    if (current >= 2) {
                        groupSplitPaneLocation = current;
                    } else if (last >= 2) {
                        groupSplitPaneLocation = last;
                    }
                }
            }
        };
        groups.addPropertyChangeListener("selectedRosterGroup", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                p.setProperty(this.getClass().getName(), "selectedRosterGroup", pce.getNewValue());
                setTitle((String) pce.getNewValue());
            }
        });
        rosterGroupSplitPane.addPropertyChangeListener(propertyChangeListener);
        Roster.instance().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("RosterGroupAdded") && Roster.instance().getRosterGroupList().size() == 1) {
                    // if the pane is hidden, show it when 1st group is created
                    hideGroupsPane(false);
                    enableRosterGroupMenuItems(true);
                } else if (!rtable.isVisible() && (e.getPropertyName().equals("saved"))) {
                    if (firstHelpLabel != null) {
                        firstHelpLabel.setVisible(false);
                    }
                    rtable.setVisible(true);
                    rtable.resetColumnWidths();
                }
            }
        });
        if (Roster.instance().numEntries() == 0) {
            try {
                BufferedImage myPicture = ImageIO.read(FileUtil.findURL("resources/dp3first.gif", FileUtil.Location.INSTALLED));
                //rosters.add(new JLabel(new ImageIcon( myPicture )), BorderLayout.CENTER);
                firstHelpLabel = new JLabel(new ImageIcon(myPicture));
                rtable.setVisible(false);
                rosters.add(firstHelpLabel, BorderLayout.NORTH);
                //tableArea.add(firstHelpLabel);
                rtable.setVisible(false);
            } catch (IOException ex) {
                // handle exception...
            }
        }
        return rosterGroupSplitPane;
    }

    protected void deleteLoco() {
        DeleteRosterItemAction act = new DeleteRosterItemAction("Delete", (WindowInterface) this);
        act.actionPerformed(null);
    }

    void editMediaButton() {
        //Because of the way that programmers work, we need to use edit mode for displaying the media pane, so that the read/write buttons do not appear.
        boolean serviceSelected = service.isSelected();
        boolean opsSelected = ops.isSelected();
        edit.setSelected(true);
        startProgrammer(null, re, "dp3" + File.separator + "MediaPane");
        service.setSelected(serviceSelected);
        ops.setSelected(opsSelected);
    }

    protected void enableRosterGroupMenuItems(boolean enable) {
        firePropertyChange("groupspane", "setEnabled", enable);
        firePropertyChange("grouptable", "setEnabled", enable);
        firePropertyChange("deletegroup", "setEnabled", enable);
    }

    protected void exportLoco() {
        ExportRosterItem act = new ExportRosterItem(Bundle.getMessage("Export"), this, re);
        act.actionPerformed(null);
    }

    void formatTextAreaAsLabel(JTextPane pane) {
        pane.setOpaque(false);
        pane.setEditable(false);
        pane.setBorder(null);
    }

    /*=============== Getters and Setters for core properties ===============*/
    /**
     * @return Will closing the window quit JMRI?
     */
    public boolean isAllowQuit() {
        return allowQuit;
    }

    /**
     * @param allowQuit Set state to either close JMRI or just the roster window
     */
    public void setAllowQuit(boolean allowQuit) {
        allowQuit(allowQuit);
    }

    /**
     * @return the baseTitle
     */
    protected String getBaseTitle() {
        return baseTitle;
    }

    /**
     * @param baseTitle the baseTitle to set
     */
    protected final void setBaseTitle(String baseTitle) {
        String title = null;
        if (this.baseTitle == null) {
            title = this.getTitle();
        }
        this.baseTitle = baseTitle;
        if (title != null) {
            this.setTitle(title);
        }
    }

    /**
     * @return the newWindowAction
     */
    protected JmriAbstractAction getNewWindowAction() {
        if (newWindowAction == null) {
            newWindowAction = new RosterFrameAction("newWindow", this, allowQuit);
        }
        return newWindowAction;
    }

    /**
     * @param newWindowAction the newWindowAction to set
     */
    protected void setNewWindowAction(JmriAbstractAction newWindowAction) {
        this.newWindowAction = newWindowAction;
        this.groups.setNewWindowMenuAction(newWindowAction);
    }

    @Override
    public void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            title = Roster.ALLENTRIES;
        }
        if (this.baseTitle != null) {
            if (!title.equals(this.baseTitle) && !title.startsWith(this.baseTitle)) {
                super.setTitle(this.baseTitle + ": " + title);
            }
        } else {
            super.setTitle(title);
        }
    }

    @Override
    public Object getProperty(String key) {
        if (key.equalsIgnoreCase("selectedRosterGroup")) {
            return getSelectedRosterGroup();
        } else if (key.equalsIgnoreCase("hideSummary")) {
            return hideBottomPane;
        }
        // call parent getProperty method to return any properties defined
        // in the class heirarchy.
        return super.getProperty(key);
    }

    public Object getRemoteObject(String value) {
        return getProperty(value);
    }

    // cache selectedRosterEntries so that multiple calls to this
    // between selection changes will not require the creation of a new array
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP",
            justification = "Want to give access to mutable, original roster objects")
    public RosterEntry[] getSelectedRosterEntries() {
        return rtable.getSelectedRosterEntries();
    }

    @Override
    public String getSelectedRosterGroup() {
        return groups.getSelectedRosterGroup();
    }

    void handleQuit(WindowEvent e) {
        if (e != null && frameInstances.size() == 1) {
            final String rememberWindowClose = this.getClass().getName() + ".closeDP3prompt";
            if (!p.getSimplePreferenceState(rememberWindowClose)) {
                JPanel message = new JPanel();
                JLabel question = new JLabel(rb.getString("MessageLongCloseWarning"));
                final JCheckBox remember = new JCheckBox(rb.getString("MessageRememberSetting"));
                remember.setFont(remember.getFont().deriveFont(10.0F));
                message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
                message.add(question);
                message.add(remember);
                int result = JOptionPane.showConfirmDialog(null,
                        message,
                        rb.getString("MessageShortCloseWarning"),
                        JOptionPane.YES_NO_OPTION);
                if (remember.isSelected()) {
                    p.setSimplePreferenceState(rememberWindowClose, true);
                }
                if (result == JOptionPane.YES_OPTION) {
                    AppsBase.handleQuit();
                }
            } else {
                AppsBase.handleQuit();
            }
        } else if (frameInstances.size() > 1) {
            final String rememberWindowClose = this.getClass().getName() + ".closeMultipleDP3prompt";
            if (!p.getSimplePreferenceState(rememberWindowClose)) {
                JPanel message = new JPanel();
                JLabel question = new JLabel(rb.getString("MessageLongMultipleCloseWarning"));
                final JCheckBox remember = new JCheckBox(rb.getString("MessageRememberSetting"));
                remember.setFont(remember.getFont().deriveFont(10.0F));
                message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
                message.add(question);
                message.add(remember);
                int result = JOptionPane.showConfirmDialog(null,
                        message,
                        rb.getString("MessageShortCloseWarning"),
                        JOptionPane.YES_NO_OPTION);
                if (remember.isSelected()) {
                    p.setSimplePreferenceState(rememberWindowClose, true);
                }
                if (result == JOptionPane.YES_OPTION) {
                    AppsBase.handleQuit();
                }
            } else {
                AppsBase.handleQuit();
            }
            //closeWindow(null);
        }
    }

    protected void helpMenu(JMenuBar menuBar, final JFrame frame) {
        try {
            // create menu and standard items
            JMenu helpMenu = HelpUtil.makeHelpMenu("package.apps.gui3.dp3.DecoderPro3", true);
            // tell help to use default browser for external types
            SwingHelpUtilities.setContentViewerUI("jmri.util.ExternalLinkContentViewerUI");
            // use as main help menu
            menuBar.add(helpMenu);
        } catch (Throwable e3) {
            log.error("Unexpected error creating help: " + e3);
        }
    }

    protected void hideGroups() {
        boolean boo = !hideGroups;
        hideGroupsPane(boo);
    }

    public void hideGroupsPane(boolean hide) {
        if (hideGroups == hide) {
            return;
        }
        hideGroups = hide;
        if (hide) {
            groupSplitPaneLocation = rosterGroupSplitPane.getDividerLocation();
            rosterGroupSplitPane.setDividerLocation(1);
            rosterGroupSplitPane.getLeftComponent().setMinimumSize(new Dimension());
            if (Roster.instance().getRosterGroupList().isEmpty()) {
                rosterGroupSplitPane.setOneTouchExpandable(false);
                rosterGroupSplitPane.setDividerSize(0);
            }
        } else {
            rosterGroupSplitPane.setDividerSize(UIManager.getInt("SplitPane.dividerSize"));
            rosterGroupSplitPane.setOneTouchExpandable(true);
            if (groupSplitPaneLocation >= 2) {
                rosterGroupSplitPane.setDividerLocation(groupSplitPaneLocation);
            } else {
                rosterGroupSplitPane.resetToPreferredSizes();
            }
        }
    }

    protected void hideRosterImage() {
        hideRosterImage = !hideRosterImage;
        //p.setSimplePreferenceState(DecoderPro3Window.class.getName()+".hideRosterImage",hideRosterImage);
        if (hideRosterImage) {
            locoImage.setVisible(false);
        } else {
            locoImage.setVisible(true);
        }
    }

    protected void hideSummary() {
        boolean boo = !hideBottomPane;
        hideBottomPane(boo);
    }

    /**
     * An entry has been selected in the Roster Table, activate the bottom part
     * of the window
     */
    void locoSelected(String id) {
        if (id != null) {
            log.debug("locoSelected ID " + id);
            if (re != null) {
                //We remove the propertychangelistener if we had a previoulsy selected entry;
                re.removePropertyChangeListener(rosterEntryUpdateListener);
            }
            // convert to roster entry
            re = Roster.instance().entryFromTitle(id);
            re.addPropertyChangeListener(rosterEntryUpdateListener);
        } else {
            log.debug("Multiple selection");
            re = null;
        }
        updateDetails();
    }

    protected void newWindow() {
        this.newWindow(this.getNewWindowAction());
    }

    protected void newWindow(JmriAbstractAction action) {
        action.setWindowInterface(this);
        action.actionPerformed(null);
        firePropertyChange("closewindow", "setEnabled", true);
    }

    protected void printLoco(boolean boo) {
        PrintRosterEntry pre = new PrintRosterEntry(re, this, "programmers" + File.separator + programmer2 + ".xml");
        pre.printPanes(boo);
    }

    //Matches the first argument in the array against a locally know method
    @Override
    public void remoteCalls(String[] args) {
        args[0] = args[0].toLowerCase();
        if (args[0].equals("identifyloco")) {
            startIdentifyLoco();
        } else if (args[0].equals("printloco")) {
            if (checkIfEntrySelected()) {
                printLoco(false);
            }
        } else if (args[0].equals("printpreviewloco")) {
            if (checkIfEntrySelected()) {
                printLoco(true);
            }
        } else if (args[0].equals("exportloco")) {
            if (checkIfEntrySelected()) {
                exportLoco();
            }
        } else if (args[0].equals("basicprogrammer")) {
            if (checkIfEntrySelected()) {
                startProgrammer(null, re, programmer2);
            }
        } else if (args[0].equals("comprehensiveprogrammer")) {
            if (checkIfEntrySelected()) {
                startProgrammer(null, re, programmer1);
            }
        } else if (args[0].equals("editthrottlelabels")) {
            if (checkIfEntrySelected()) {
                startProgrammer(null, re, "dp3" + File.separator + "ThrottleLabels");
            }
        } else if (args[0].equals("editrostermedia")) {
            if (checkIfEntrySelected()) {
                startProgrammer(null, re, "dp3" + File.separator + "MediaPane");
            }
        } else if (args[0].equals("hiderosterimage")) {
            hideRosterImage();
        } else if (args[0].equals("summarypane")) {
            hideSummary();
        } else if (args[0].equals("copyloco")) {
            if (checkIfEntrySelected()) {
                copyLoco();
            }
        } else if (args[0].equals("deleteloco")) {
            if (checkIfEntrySelected()) {
                deleteLoco();
            }
        } else if (args[0].equals("setprogservice")) {
            service.setSelected(true);
        } else if (args[0].equals("setprogops")) {
            ops.setSelected(true);
        } else if (args[0].equals("setprogedit")) {
            edit.setSelected(true);
        } else if (args[0].equals("groupspane")) {
            hideGroups();
        } else if (args[0].equals("quit")) {
            saveWindowDetails();
            handleQuit(new WindowEvent(this, frameInstances.size()));
        } else if (args[0].equals("closewindow")) {
            closeWindow(null);
        } else if (args[0].equals("newwindow")) {
            newWindow();
        } else if (args[0].equals("resettablecolumns")) {
            rtable.resetColumnWidths();
        } else {
            log.error("method " + args[0] + " not found");
        }
    }

    JPanel rosterDetails() {
        JPanel panel = new JPanel();
        GridBagLayout gbLayout = new GridBagLayout();
        GridBagConstraints cL = new GridBagConstraints();
        GridBagConstraints cR = new GridBagConstraints();
        Dimension minFieldDim = new Dimension(30, 20);
        cL.gridx = 0;
        cL.gridy = 0;
        cL.ipadx = 3;
        cL.anchor = GridBagConstraints.EAST;
        cL.insets = new Insets(0, 0, 0, 15);
        JLabel row0Label = new JLabel(Bundle.getMessage("FieldID") + ":", JLabel.LEFT);
        gbLayout.setConstraints(row0Label, cL);
        panel.setLayout(gbLayout);
        panel.add(row0Label);
        cR.gridx = 1;
        cR.gridy = 0;
        cR.anchor = GridBagConstraints.WEST;
        id.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(id, cR);
        formatTextAreaAsLabel(id);
        panel.add(id);
        cL.gridy = 1;
        JLabel row1Label = new JLabel(Bundle.getMessage("FieldRoadName") + ":", JLabel.LEFT);
        gbLayout.setConstraints(row1Label, cL);
        panel.add(row1Label);
        cR.gridy = 1;
        roadName.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(roadName, cR);
        formatTextAreaAsLabel(roadName);
        panel.add(roadName);
        cL.gridy = 2;
        JLabel row2Label = new JLabel(Bundle.getMessage("FieldRoadNumber") + ":");
        gbLayout.setConstraints(row2Label, cL);
        panel.add(row2Label);
        cR.gridy = 2;
        roadNumber.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(roadNumber, cR);
        formatTextAreaAsLabel(roadNumber);
        panel.add(roadNumber);
        cL.gridy = 3;
        JLabel row3Label = new JLabel(Bundle.getMessage("FieldManufacturer") + ":");
        gbLayout.setConstraints(row3Label, cL);
        panel.add(row3Label);
        cR.gridy = 3;
        mfg.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(mfg, cR);
        formatTextAreaAsLabel(mfg);
        panel.add(mfg);
        cL.gridy = 4;
        JLabel row4Label = new JLabel(Bundle.getMessage("FieldOwner") + ":");
        gbLayout.setConstraints(row4Label, cL);
        panel.add(row4Label);
        cR.gridy = 4;
        owner.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(owner, cR);
        formatTextAreaAsLabel(owner);
        panel.add(owner);
        cL.gridy = 5;
        JLabel row5Label = new JLabel(Bundle.getMessage("FieldModel") + ":");
        gbLayout.setConstraints(row5Label, cL);
        panel.add(row5Label);
        cR.gridy = 5;
        model.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(model, cR);
        formatTextAreaAsLabel(model);
        panel.add(model);
        cL.gridy = 6;
        JLabel row6Label = new JLabel(Bundle.getMessage("FieldDCCAddress") + ":");
        gbLayout.setConstraints(row6Label, cL);
        panel.add(row6Label);
        cR.gridy = 6;
        dccAddress.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(dccAddress, cR);
        formatTextAreaAsLabel(dccAddress);
        panel.add(dccAddress);
        cL.gridy = 7;
        cR.gridy = 7;
        cL.gridy = 8;
        cR.gridy = 8;
        cL.gridy = 9;
        JLabel row9Label = new JLabel(Bundle.getMessage("FieldDecoderFamily") + ":");
        gbLayout.setConstraints(row9Label, cL);
        panel.add(row9Label);
        cR.gridy = 9;
        decoderFamily.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(decoderFamily, cR);
        formatTextAreaAsLabel(decoderFamily);
        panel.add(decoderFamily);
        cL.gridy = 10;
        JLabel row10Label = new JLabel(Bundle.getMessage("FieldDecoderModel") + ":");
        gbLayout.setConstraints(row10Label, cL);
        panel.add(row10Label);
        cR.gridy = 10;
        decoderModel.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(decoderModel, cR);
        formatTextAreaAsLabel(decoderModel);
        panel.add(decoderModel);
        cL.gridy = 11;
        cR.gridy = 11;
        cL.gridy = 12;
        JLabel row12Label = new JLabel(Bundle.getMessage("FieldFilename") + ":");
        gbLayout.setConstraints(row12Label, cL);
        panel.add(row12Label);
        cR.gridy = 12;
        filename.setMinimumSize(minFieldDim);
        gbLayout.setConstraints(filename, cR);
        formatTextAreaAsLabel(filename);
        panel.add(filename);
        cL.gridy = 13;
        /*
         * JLabel row13Label = new
         * JLabel(Bundle.getMessage("FieldDateUpdated")+":");
         * gbLayout.setConstraints(row13Label,cL); panel.add(row13Label);
         */
        cR.gridy = 13;
        /*
         * filename.setMinimumSize(minFieldDim);
         * gbLayout.setConstraints(dateUpdated,cR); panel.add(dateUpdated);
         */
        formatTextAreaAsLabel(dateUpdated);
        JPanel retval = new JPanel(new FlowLayout(FlowLayout.LEFT));
        retval.add(panel);
        return retval;
    }

    void saveWindowDetails() {
        p.setSimplePreferenceState(this.getClass().getName() + ".hideSummary", hideBottomPane);
        p.setSimplePreferenceState(this.getClass().getName() + ".hideGroups", hideGroups);
        p.setSimplePreferenceState(this.getClass().getName() + ".hideRosterImage", hideRosterImage);
        p.setProperty(getWindowFrameRef(), "selectedRosterGroup", groups.getSelectedRosterGroup());
        String selectedProgMode = "edit";
        if (service.isSelected()) {
            selectedProgMode = "service";
        }
        if (ops.isSelected()) {
            selectedProgMode = "ops";
        }
        p.setProperty(getWindowFrameRef(), "selectedProgrammer", selectedProgMode);
        //Method to save table sort, width and column order status
        String rostertableref = getWindowFrameRef() + ":roster";

        XTableColumnModel tcm = rtable.getXTableColumnModel();
        Enumeration<TableColumn> en = tcm.getColumns(false);
        while (en.hasMoreElements()) {
            TableColumn tc = en.nextElement();

            try {
                String columnName = (String) tc.getHeaderValue();
                int index = tcm.getColumnIndex(tc.getIdentifier(), false);
                p.setTableColumnPreferences(rostertableref, columnName, index, tc.getPreferredWidth(), rtable.getModel().getSortingStatus(tc.getModelIndex()), !tcm.isColumnVisible(tc));
            } catch (Exception e) {
                log.warn("unable to store settings for table column " + tc.getHeaderValue(), e);
            }
        }
        if (rosterGroupSplitPane.getDividerLocation() > 2) {
            p.setProperty(getWindowFrameRef(), "rosterGroupPaneDividerLocation", rosterGroupSplitPane.getDividerLocation());
        } else if (groupSplitPaneLocation > 2) {
            p.setProperty(getWindowFrameRef(), "rosterGroupPaneDividerLocation", groupSplitPaneLocation);
        }
    }

    /**
     * Identify locomotive complete, act on it by setting the GUI. This will
     * fire "GUI changed" events which will reset the decoder GUI.
     *
     * @param dccAddress
     */
    protected void selectLoco(int dccAddress, boolean isLong, int mfgId, int modelId) {
        // raise the button again
        //idloco.setSelected(false);
        // locate that loco
        inStartProgrammer = false;
        if (re != null) {
            //We remove the propertychangelistener if we had a previoulsy selected entry;
            re.removePropertyChangeListener(rosterEntryUpdateListener);
        }
        List<RosterEntry> l = Roster.instance().matchingList(null, null, Integer.toString(dccAddress), null, null, null, null);
        if (log.isDebugEnabled()) {
            log.debug("selectLoco found " + l.size() + " matches");
        }
        if (l.size() > 0) {
            if (l.size() > 1) {
                //More than one possible loco, so check long flag
                List<RosterEntry> l2 = new ArrayList<RosterEntry>();
                for (RosterEntry _re : l) {
                    if (_re.isLongAddress() == isLong) {
                        l2.add(_re);
                    }
                }
                if (l2.size() == 1) {
                    re = l2.get(0);
                } else {
                    if (l2.isEmpty()) {
                        l2 = l;
                    }
                    //Still more than one possible loco, so check against the decoder family
                    List<RosterEntry> l3 = new ArrayList<RosterEntry>();
                    List<DecoderFile> temp = DecoderIndexFile.instance().matchingDecoderList(null, null, "" + mfgId, "" + modelId, null, null);
                    ArrayList<String> decoderFam = new ArrayList<String>();
                    for (DecoderFile f : temp) {
                        if (!decoderFam.contains(f.getModel())) {
                            decoderFam.add(f.getModel());
                        }
                    }
                    for (RosterEntry _re : l2) {
                        if (decoderFam.contains(_re.getDecoderModel())) {
                            l3.add(_re);
                        }
                    }
                    if (l3.isEmpty()) {
                        //Unable to determine the loco against the manufacture therefore will be unable to further identify against decoder.
                        re = l2.get(0);
                    } else {
                        //We have no other options to match against so will return the first one we come across;
                        re = l3.get(0);
                    }
                }
            } else {
                re = l.get(0);
            }
            re.addPropertyChangeListener(rosterEntryUpdateListener);
            rtable.setSelection(re);
            updateDetails();
            rtable.moveTableViewToSelected();
        } else {
            log.warn("Read address " + dccAddress + ", but no such loco in roster"); //"No roster entry found"
            JOptionPane.showMessageDialog(this, "No roster entry found", "Address " + dccAddress + " was read from the decoder\nbut has not been found in the Roster", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Simple method to change over the programmer buttons, this should be
     * implemented button with the buttons in their own class etc, but this will
     * work for now. Basic button is button Id 1, comprehensive button is button
     * id 2
     */
    public void setProgrammerLaunch(int buttonId, String programmer, String buttonText) {
        if (buttonId == 1) {
            programmer1 = programmer;
            prog1Button.setText(buttonText);
        } else if (buttonId == 2) {
            programmer2 = programmer;
            prog2Button.setText(buttonText);
        }
    }

    public void setSelectedRosterGroup(String rosterGroup) {
        groups.setSelectedRosterGroup(rosterGroup);
    }

    protected void showPopup(MouseEvent e) {
        int row = rtable.getTable().rowAtPoint(e.getPoint());
        if (!rtable.getTable().isRowSelected(row)) {
            rtable.getTable().changeSelection(row, 0, false, false);
        }
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Program");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startProgrammer(null, re, programmer1);
            }
        });
        if (re == null) {
            menuItem.setEnabled(false);
        }
        popupMenu.add(menuItem);
        ButtonGroup group = new ButtonGroup();
        group.add(contextService);
        group.add(contextOps);
        group.add(contextEdit);
        JMenu progMenu = new JMenu("Programmer type");
        contextService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                service.setSelected(true);
                updateProgMode();
            }
        });
        progMenu.add(contextService);
        contextOps.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ops.setSelected(true);
                updateProgMode();
            }
        });
        progMenu.add(contextOps);
        contextEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                edit.setSelected(true);
                updateProgMode();
            }
        });
        if (service.isSelected()) {
            contextService.setSelected(true);
        } else if (ops.isSelected()) {
            contextOps.setSelected(true);
        } else {
            contextEdit.setSelected(true);
        }
        progMenu.add(contextEdit);
        popupMenu.add(progMenu);
        popupMenu.addSeparator();
        menuItem = new JMenuItem("Labels and Media");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editMediaButton();
            }
        });
        if (re == null) {
            menuItem.setEnabled(false);
        }
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("Throttle");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThrottleFrame tf = ThrottleFrameManager.instance().createThrottleFrame();
                tf.toFront();
                tf.getAddressPanel().getRosterEntrySelector().setSelectedRosterGroup(getSelectedRosterGroup());
                tf.getAddressPanel().setRosterEntry(re);
            }
        });
        if (re == null) {
            menuItem.setEnabled(false);
        }
        popupMenu.add(menuItem);
        popupMenu.addSeparator();
        menuItem = new JMenuItem("Duplicate");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyLoco();
            }
        });
        if (re == null) {
            menuItem.setEnabled(false);
        }
        popupMenu.add(menuItem);
        menuItem = new JMenuItem("Delete");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteLoco();
            }
        });
        popupMenu.add(menuItem);
        if (re == null) {
            menuItem.setEnabled(false);
        }

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Identify loco button pressed, start the identify operation This defines
     * what happens when the identify is done.
     */
    //taken out of CombinedLocoSelPane
    protected void startIdentifyLoco() {
        if (InstanceManager.getDefault(GlobalProgrammerManager.class) == null) {
            log.error("Identify loco called when no service mode programmer is available");
            JOptionPane.showMessageDialog(null, Bundle.getMessage("IdentifyError"));
            return;
        }
        // start identifying a loco
        final RosterFrame me = this;
        Programmer p = null;
        if (modePanel.isSelected()) p = modePanel.getProgrammer();
        if (p == null) {
            log.warn("Selector did not provide a programmer, use default");
            p = jmri.InstanceManager.programmerManagerInstance().getGlobalProgrammer();
        }
        IdentifyLoco ident = new IdentifyLoco(p) {
            private RosterFrame who = me;

            @Override
            protected void done(int dccAddress) {
                // if Done, updated the selected decoder
                who.selectLoco(dccAddress, !shortAddr, cv8val, cv7val);
            }

            @Override
            protected void message(String m) {
                statusField.setText(m);
            }

            @Override
            protected void error() {
                // raise the button again
                //idloco.setSelected(false);
            }
        };
        ident.start();
    }

    protected void startProgrammer(DecoderFile decoderFile, RosterEntry re, String filename) {
        if (inStartProgrammer) {
            log.debug("Call to start programmer has been called twice when the first call hasn't opened");
            return;
        }
        if (!checkIfEntrySelected()) {
            return;
        }
        try {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            inStartProgrammer = true;
            String title = re.getId();
            JFrame progFrame = null;
            if (edit.isSelected()) {
                progFrame = new PaneProgFrame(decoderFile, re, title, "programmers" + File.separator + filename + ".xml", null, false) {
                    /**
                     *
                     */
                    private static final long serialVersionUID = -5406468334024223092L;

                    @Override
                    protected JPanel getModePane() {
                        return null;
                    }
                };
            } else if (service.isSelected()) {
                progFrame = new PaneServiceProgFrame(decoderFile, re, title, "programmers" + File.separator + filename + ".xml", modePanel.getProgrammer()) {

                    /**
                     *
                     */
                    private static final long serialVersionUID = 1953980697520737653L;
                };
            } else if (ops.isSelected()) {
                int address = Integer.parseInt(re.getDccAddress());
                boolean longAddr = re.isLongAddress();
                Programmer pProg = InstanceManager.getDefault(AddressedProgrammerManager.class).getAddressedProgrammer(longAddr, address);
                progFrame = new PaneOpsProgFrame(decoderFile, re, title, "programmers" + File.separator + filename + ".xml", pProg);
            }
            if (progFrame == null) {
                return;
            }
            progFrame.pack();
            progFrame.setVisible(true);
        } finally {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        inStartProgrammer = false;
    }

    /*
     * This status bar needs sorting out properly
     */
    protected void statusBar() {
        addToStatusBox(serviceModeProgrammerLabel, null);
        addToStatusBox(operationsModeProgrammerLabel, null);
        JLabel programmerStatusLabel = new JLabel(Bundle.getMessage("ProgrammerStatus"));
        statusField.setText(Bundle.getMessage("Idle"));
        addToStatusBox(programmerStatusLabel, statusField);
        addToStatusBox(new JLabel(Bundle.getMessage("ActiveProfile", ProfileManager.defaultManager().getActiveProfile().getName())));
    }

    protected void systemsMenu() {
        ActiveSystemsMenu.addItems(getMenu());
        getMenu().add(new WindowMenu(this));
    }

    void updateDetails() {
        if (re == null) {
            String value = (rtable.getTable().getSelectedRowCount() > 1) ? "Multiple Items Selected" : "";
            filename.setText(value);
            dateUpdated.setText(value);
            decoderModel.setText(value);
            decoderFamily.setText(value);
            id.setText(value);
            roadName.setText(value);
            dccAddress.setText(value);
            roadNumber.setText(value);
            mfg.setText(value);
            model.setText(value);
            owner.setText(value);
            locoImage.setImagePath(value);
        } else {
            filename.setText(re.getFileName());
            dateUpdated.setText(re.getDateUpdated());
            decoderModel.setText(re.getDecoderModel());
            decoderFamily.setText(re.getDecoderFamily());
            dccAddress.setText(re.getDccAddress());
            id.setText(re.getId());
            roadName.setText(re.getRoadName());
            roadNumber.setText(re.getRoadNumber());
            mfg.setText(re.getMfg());
            model.setText(re.getModel());
            owner.setText(re.getOwner());
            locoImage.setImagePath(re.getImagePath());
            if (hideRosterImage) {
                locoImage.setVisible(false);
            } else {
                locoImage.setVisible(true);
            }
            prog1Button.setEnabled(true);
            prog2Button.setEnabled(true);
            throttleLabels.setEnabled(true);
            rosterMedia.setEnabled(true);
            throttleLaunch.setEnabled(true);
            updateProgMode();
        }
    }

    void updateProgMode() {
        String progMode;
        if (service.isSelected()) {
            progMode = "setprogservice";
        } else if (ops.isSelected()) {
            progMode = "setprogops";
        } else {
            progMode = "setprogedit";
        }
        firePropertyChange(progMode, "setSelected", true);
    }

    /*
     * this handles setting up and updating the GUI for the types of programmer
     * available.
     */
    protected void updateProgrammerStatus() {
        ConnectionConfig oldServMode = serModeProCon;
        ConnectionConfig oldOpsMode = opsModeProCon;

        GlobalProgrammerManager gpm = InstanceManager.getDefault(GlobalProgrammerManager.class);
        if (gpm != null) {
            String serviceModeProgrammer = gpm.getUserName();
            ArrayList<Object> connList = InstanceManager.configureManagerInstance().getInstanceList(ConnectionConfig.class);
            if (connList != null) {
                for (int x = 0; x < connList.size(); x++) {
                    ConnectionConfig conn = (ConnectionConfig) connList.get(x);
                    if (conn.getConnectionName() != null && conn.getConnectionName().equals(serviceModeProgrammer)) {
                        serModeProCon = conn;
                    }
                }
            }
        }
        AddressedProgrammerManager apm = InstanceManager.getDefault(AddressedProgrammerManager.class);
        if (apm != null) {
            //Ideally we should probably have the programmer manager reference the username configured in the system connection memo.
            //but as DP3 (jmri can not use mutliple programmers!) isn't designed for multi-connection enviroments this should be sufficient*/
            String opsModeProgrammer = apm.getUserName();
            ArrayList<Object> connList = InstanceManager.configureManagerInstance().getInstanceList(ConnectionConfig.class);
            if (connList != null) {
                for (int x = 0; x < connList.size(); x++) {
                    ConnectionConfig conn = (ConnectionConfig) connList.get(x);
                    if (conn.getConnectionName() != null && conn.getConnectionName().equals(opsModeProgrammer)) {
                        opsModeProCon = conn;
                    }
                }
            }
        }

        if (serModeProCon != null) {
            if (ConnectionStatus.instance().isConnectionOk(serModeProCon.getInfo())
                    && gpm != null) {
                serviceModeProgrammerLabel.setText(
                        Bundle.getMessage("ServiceModeProgOnline", serModeProCon.getConnectionName()));
                serviceModeProgrammerLabel.setForeground(new Color(0, 128, 0));
            } else {
                serviceModeProgrammerLabel.setText(
                        Bundle.getMessage("ServiceModeProgOffline", serModeProCon.getConnectionName()));
                serviceModeProgrammerLabel.setForeground(Color.red);
            }
            if (oldServMode == null) {
                contextService.setEnabled(true);
                contextService.setVisible(true);
                service.setEnabled(true);
                service.setVisible(true);
                firePropertyChange("setprogservice", "setEnabled", true);
            }
        } else {
            serviceModeProgrammerLabel.setText(Bundle.getMessage("NoServiceProgrammerAvailable"));
            serviceModeProgrammerLabel.setForeground(Color.red);
            if (oldServMode != null) {
                contextService.setEnabled(false);
                contextService.setVisible(false);
                service.setEnabled(false);
                service.setVisible(false);
                firePropertyChange("setprogservice", "setEnabled", false);
            }
        }

        if (opsModeProCon != null) {
            if (ConnectionStatus.instance().isConnectionOk(opsModeProCon.getInfo()) && apm != null) {
                operationsModeProgrammerLabel.setText(
                        Bundle.getMessage("OpsModeProgOnline", opsModeProCon.getConnectionName()));
                operationsModeProgrammerLabel.setForeground(new Color(0, 128, 0));
            } else {
                operationsModeProgrammerLabel.setText(
                        Bundle.getMessage("OpsModeProgOffline", opsModeProCon.getConnectionName()));
                operationsModeProgrammerLabel.setForeground(Color.red);
            }
            if (oldOpsMode == null) {
                contextOps.setEnabled(true);
                contextOps.setVisible(true);
                ops.setEnabled(true);
                ops.setVisible(true);
                firePropertyChange("setprogops", "setEnabled", true);
            }
        } else {
            operationsModeProgrammerLabel.setText(Bundle.getMessage("NoOpsProgrammerAvailable"));
            operationsModeProgrammerLabel.setForeground(Color.red);
            if (oldOpsMode != null) {
                contextOps.setEnabled(false);
                contextOps.setVisible(false);
                ops.setEnabled(false);
                ops.setVisible(false);
                firePropertyChange("setprogops", "setEnabled", false);
            }
        }
        String strProgMode;
        if (service.isEnabled()) {
            contextService.setSelected(true);
            service.setSelected(true);
            strProgMode = "setprogservice";
            modePanel.setVisible(true);
        } else if (ops.isEnabled()) {
            contextOps.setSelected(true);
            ops.setSelected(true);
            strProgMode = "setprogops";
            modePanel.setVisible(false);
        } else {
            contextEdit.setSelected(true);
            edit.setSelected(true);
            modePanel.setVisible(false);
            strProgMode = "setprogedit";
        }
        firePropertyChange(strProgMode, "setSelected", true);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        closeWindow(e);
    }
    //Popup listener is used against the roster table to display a

    class rosterPopupListener extends MouseAdapter {

        javax.swing.Timer clickTimer = null;

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
                return;
            }
            if (clickTimer == null) {
                clickTimer = new Timer(clickDelay, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //Single click item is handled else where.
                    }
                });
                clickTimer.setRepeats(false);
            }
            if (e.getClickCount() == 1) {
                clickTimer.start();
            } else if (e.getClickCount() == 2) {
                clickTimer.stop();
                startProgrammer(null, re, programmer1);
            }
        }
    }

    static class ExportRosterItem extends ExportRosterItemAction {

        /**
         *
         */
        private static final long serialVersionUID = 5920288372458701120L;

        ExportRosterItem(String pName, Component pWho, RosterEntry re) {
            super(pName, pWho);
            setExistingEntry(re);
        }

        @Override
        protected boolean selectFrom() {
            return true;
        }
    }

    static class CopyRosterItem extends CopyRosterItemAction {

        /**
         *
         */
        private static final long serialVersionUID = -4822095767152284104L;

        CopyRosterItem(String pName, Component pWho, RosterEntry re) {
            super(pName, pWho);
            setExistingEntry(re);
        }

        @Override
        protected boolean selectFrom() {
            return true;
        }
    }
}
