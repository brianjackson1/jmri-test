// LocationsTableFrame.java
package jmri.jmrit.operations.locations;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import jmri.jmrit.operations.OperationsFrame;
import jmri.jmrit.operations.setup.Control;
import jmri.jmrit.operations.setup.Setup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frame for adding and editing the location roster for operations.
 *
 * @author	Bob Jacobsen Copyright (C) 2001
 * @author Daniel Boudreau Copyright (C) 2008
 * @version $Revision$
 */
public class LocationsTableFrame extends OperationsFrame {

    /**
     *
     */
    private static final long serialVersionUID = 4984202877113842727L;
    LocationsTableModel locationsModel = new LocationsTableModel();
    javax.swing.JTable locationsTable = new javax.swing.JTable(locationsModel);
    JScrollPane locationsPane;

    // labels
    JLabel textSort = new JLabel(Bundle.getMessage("SortBy"));
    JLabel textSep = new JLabel("          ");

    // radio buttons
    javax.swing.JRadioButton sortByName = new javax.swing.JRadioButton(Bundle.getMessage("Name"));
    javax.swing.JRadioButton sortById = new javax.swing.JRadioButton(Bundle.getMessage("Id"));

    // major buttons
    JButton addButton = new JButton(Bundle.getMessage("Add"));
//	JButton printTableButton = new JButton("Print Table");

    public LocationsTableFrame() {
        super(Bundle.getMessage("TitleLocationsTable"));
        // general GUI config

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // Set up the jtable in a Scroll Pane..
        locationsPane = new JScrollPane(locationsTable);
        locationsPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        locationsPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        locationsModel.initTable(locationsTable);
        getContentPane().add(locationsPane);

        // Set up the control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        controlPanel.add(textSort);
        controlPanel.add(sortByName);
        controlPanel.add(sortById);
        controlPanel.add(textSep);
        controlPanel.add(addButton);
//		controlPanel.add (printTableButton);
        controlPanel.setMaximumSize(new Dimension(Control.panelWidth1025, 50));

        getContentPane().add(controlPanel);

        sortByName.setSelected(true);

        // setup buttons
        addButtonAction(addButton);
//		addButtonAction(printTableButton);

        addRadioButtonAction(sortByName);
        addRadioButtonAction(sortById);

        //	build menu
        JMenuBar menuBar = new JMenuBar();
        JMenu toolMenu = new JMenu(Bundle.getMessage("Tools"));
        toolMenu.add(new LocationCopyAction());
        toolMenu.add(new SchedulesTableAction(Bundle.getMessage("Schedules")));
        toolMenu.add(new ModifyLocationsAction(Bundle.getMessage("TitleModifyLocations")));
        toolMenu.add(new ModifyLocationsCarLoadsAction());
        toolMenu.add(new ShowCarsByLocationAction(false, null, null));
        if (Setup.isVsdPhysicalLocationEnabled()) {
            toolMenu.add(new SetPhysicalLocationAction(Bundle.getMessage("MenuSetPhysicalLocation"), null));
        }
        toolMenu.add(new PrintLocationsAction(Bundle.getMessage("MenuItemPrint"), false));
        toolMenu.add(new PrintLocationsAction(Bundle.getMessage("MenuItemPreview"), true));
        menuBar.add(toolMenu);
        menuBar.add(new jmri.jmrit.operations.OperationsMenu());
        setJMenuBar(menuBar);
        addHelpMenu("package.jmri.jmrit.operations.Operations_Locations", true);	// NOI18N

        initMinimumSize();
        // make panel a bit wider than minimum if the very first time opened
        if (getWidth() == Control.panelWidth500) {
            setSize(850, getHeight());
        }

        // create ShutDownTasks
        createShutDownTask();
    }

    public void radioButtonActionPerformed(java.awt.event.ActionEvent ae) {
        log.debug("radio button activated");
        if (ae.getSource() == sortByName) {
            sortByName.setSelected(true);
            sortById.setSelected(false);
            locationsModel.setSort(locationsModel.SORTBYNAME);
        }
        if (ae.getSource() == sortById) {
            sortByName.setSelected(false);
            sortById.setSelected(true);
            locationsModel.setSort(locationsModel.SORTBYID);
        }
    }

    // add button
    public void buttonActionPerformed(java.awt.event.ActionEvent ae) {
//		log.debug("location button activated");
        if (ae.getSource() == addButton) {
            LocationEditFrame f = new LocationEditFrame(null);
            f.setTitle(Bundle.getMessage("TitleLocationAdd"));
        }
//		if (ae.getSource() == printTableButton){
//			printTable();
//		}
    }

//	public void printTable() {
//		try {
//			locationsTable.print();
//		} catch (PrinterException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
    public void dispose() {
        locationsModel.dispose();
        super.dispose();
    }

    static Logger log = LoggerFactory
            .getLogger(LocationsTableFrame.class.getName());
}
