// NixieClockFrame.java

package jmri.jmrit.nixieclock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Date;

import jmri.*;
import jmri.jmrit.catalog.*;
import jmri.util.JmriJFrame;

/**
 * Frame providing a simple clock showing Nixie tubes.
 * <P>
 * A Run/Stop button is built into this, but because I
 * don't like the way it looks, it's not currently
 * displayed in the GUI.
 *
 * Modified by Dennis Miller for resizing Nov, 2004
 *
 * @author			Bob Jacobsen   Copyright (C) 2001
 * @version			$Revision: 1.17 $
 */
public class NixieClockFrame extends JmriJFrame implements java.beans.PropertyChangeListener {

    // GUI member declarations
    JLabel h1;  // msb of hours
    JLabel h2;
    JLabel m1;  // msb of minutes
    JLabel m2;
    JLabel colon;

    double aspect;
    double iconAspect;

    Timebase clock;

    static NamedIcon tubes[] = new NamedIcon[10];
    static NamedIcon baseTubes[] = new NamedIcon[10];
    static NamedIcon colonIcon;
    static NamedIcon baseColon;
    //"base" variables used to hold original gifs, other variables used with scaled images

    public NixieClockFrame() {
        super(java.util.ResourceBundle.getBundle("jmri.jmrit.JmritToolsBundle").getString("MenuItemNixieClock"));
        
        clock = InstanceManager.timebaseInstance();

        //Load the images (these are now the larger version of the original gifs
        for (int i = 0; i < 10; i++) {
          baseTubes[i] = new NamedIcon("resources/icons/misc/Nixie/M" + i + "B.gif", "resources/icons/misc/Nixie/M" + i + "B.gif");
          tubes[i] = new NamedIcon("resources/icons/misc/Nixie/M" + i + "B.gif", "resources/icons/misc/Nixie/M" + i + "B.gif");
        }
        colonIcon = new NamedIcon("resources/icons/misc/Nixie/colonB.gif", "resources/icons/misc/Nixie/colonB.gif");
        baseColon = new NamedIcon("resources/icons/misc/Nixie/colonB.gif", "resources/icons/misc/Nixie/colonB.gif");
        // set initial size the same as the original gifs
        for (int i = 0; i < 10; i++) {
          Image scaledImage = baseTubes[i].getImage().getScaledInstance(23,32,Image.SCALE_SMOOTH);
          tubes[i].setImage(scaledImage);
        }
        Image scaledImage = baseColon.getImage().getScaledInstance(12,32,Image.SCALE_SMOOTH);
        colonIcon.setImage(scaledImage);

        // determine aspect ratio of a single digit graphic
        iconAspect = 24./32.;

        // determine the aspect ratio of the 4 digit base graphic plus a half digit for the colon
        // this DOES NOT allow space for the Run/Stop button, if it is
        // enabled.  When the Run/Stop button is enabled, the layout will have to be changed
        aspect =  (4.5*24.)/32.;

        // listen for changes to the timebase parameters
        clock.addPropertyChangeListener(this);

        // init GUI
        m1 = new JLabel(tubes[0]);
        m2 = new JLabel(tubes[0]);
        h1 = new JLabel(tubes[0]);
        h2 = new JLabel(tubes[0]);
        colon = new JLabel(colonIcon);

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        getContentPane().add(h1);
        getContentPane().add(h2);
        getContentPane().add(colon);
        getContentPane().add(m1);
        getContentPane().add(m2);

        getContentPane().add(b = new JButton("Stop"));
        b.addActionListener( new ButtonListener());
        // since Run/Stop button looks crummy, don't display for now
        b.setVisible(false);

        update();
        pack();

        // request callback to update time
        clock.addMinuteChangeListener( new java.beans.PropertyChangeListener() {
                    public void propertyChange(java.beans.PropertyChangeEvent e) {
                        update();
                    }
                });


        // Add component listener to handle frame resizing event
        this.addComponentListener(
                        new ComponentAdapter()
                {
                    public void componentResized(ComponentEvent e)
                    {
                        scaleImage();
                    }
                });

    }


    // Added method to scale the clock digit images to fit the
    // size of the display window

    public void scaleImage() {
      int iconHeight;
      int iconWidth;
      int frameHeight = this.getContentPane().getSize().height;
      int frameWidth = this.getContentPane().getSize().width;
      if ((double)frameWidth / (double)frameHeight > aspect) {
        iconHeight = frameHeight;
        iconWidth = (int) (iconAspect * iconHeight);
      }
      else {
        //this DOES NOT allow space for the Run/Stop button, if it is
        //enabled.  When the Run/Stop button is enabled, the layout will have to be changed
        iconWidth = (int) (frameWidth / 4.5);
        iconHeight = (int) (iconWidth / iconAspect);
      }
      for (int i = 0; i < 10; i++) {
        Image scaledImage = baseTubes[i].getImage().getScaledInstance(iconWidth,iconHeight,Image.SCALE_SMOOTH);
        tubes[i].setImage(scaledImage);
      }
      Image scaledImage = baseColon.getImage().getScaledInstance(iconWidth/2,iconHeight,Image.SCALE_SMOOTH);
      colonIcon.setImage(scaledImage);

//      Ugly hack to force frame to redo the layout.
//      Without this the image is scaled but the label size and position doesn't change.
//      doLayout() doesn't work either
      this.setVisible(false);
      this.remove(b);
      this.getContentPane().add(b);
      this.setVisible(true);
      return ;
    }

    @SuppressWarnings("deprecation")
    void update() {
        Date now = clock.getTime();
        int hours = now.getHours();
        int minutes = now.getMinutes();

        h1.setIcon(tubes[hours/10]);
        h2.setIcon(tubes[hours-(hours/10)*10]);
        m1.setIcon(tubes[minutes/10]);
        m2.setIcon(tubes[minutes-(minutes/10)*10]);
    }

    public void dispose() {
        super.dispose();
    }

    /**
     * Handle a change to clock properties
     */
    public void propertyChange(java.beans.PropertyChangeEvent e) {
		boolean now = clock.getRun();
		if (now) b.setText("Stop");
		else b.setText("Run");
    }

    JButton b;

	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			boolean next = !clock.getRun();
			clock.setRun(next);
			if (next) b.setText("Stop");
			else b.setText("Run ");
		}
	}
}
