// PanelProFrame.java
package apps.gui3.paned;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import jmri.jmrix.loconet.LocoNetSystemConnectionMemo;
import jmri.util.swing.JTreeUtil;
import jmri.util.swing.multipane.MultiPaneWindow;
import jmri.util.swing.multipane.PanedInterface;

/**
 * Standalone, but paned, PanelPro window.
 *
 * Ignores WindowInterface, since standalone.
 *
 * @author	Bob Jacobsen Copyright (C) 2010
 * @version	$Revision$
 */
public class PanelProFrame extends MultiPaneWindow {

    /**
     *
     */
    private static final long serialVersionUID = 5937729511010257865L;

    /**
     * Enhanced constructor for placing the pane in various GUIs
     */
    public PanelProFrame(String title) {
        super(title, "xml/config/apps/panelpro/Gui3LeftTree.xml",
                "xml/config/apps/panelpro/Gui3Menus.xml",
                "xml/config/apps/panelpro/Gui3MainToolBar.xml");
    }

    /**
     * Make a PanelPro tree, and add the LocoNet stuff, because we assume those
     * systems active here
     */
    @Override
    protected TreeNode makeNavTreeTopNode(String treeFile, PanedInterface rightTopWI) {
        DefaultMutableTreeNode top = JTreeUtil.loadTree(treeFile, rightTopWI, null);  // no context

        // as a test, we manually create a loconet tree
        System.err.print("Manually attempting to create two LocoNet trees");
        if (jmri.InstanceManager.getList(LocoNetSystemConnectionMemo.class) != null) {
            LocoNetSystemConnectionMemo lm1 = jmri.InstanceManager.getList(LocoNetSystemConnectionMemo.class).get(0);
            if (lm1 != null) {
                DefaultMutableTreeNode l1 = JTreeUtil.loadTree("xml/config/parts/jmri/jmrix/loconet/ToolsTree.xml", rightTopWI, lm1);
                l1.setUserObject("LocoNet");
                top.add(l1);
            }
            LocoNetSystemConnectionMemo lm2 = jmri.InstanceManager.getList(LocoNetSystemConnectionMemo.class).get(1);
            if (lm2 != null) {
                DefaultMutableTreeNode l2 = JTreeUtil.loadTree("xml/config/parts/jmri/jmrix/loconet/ToolsTree.xml", rightTopWI, lm2);
                l2.setUserObject("LocoNet2");
                top.add(l2);
            }
        }

        return top;
    }
}

/* @(#)PanelProFrame.java */
