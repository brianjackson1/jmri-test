// InternalReporterManager.java
package jmri.managers;

import jmri.Reporter;
import jmri.implementation.AbstractReporter;

/**
 * Implementation of the InternalReporterManager interface.
 *
 * @author	Bob Jacobsen Copyright (C) 2010
 * @version	$Revision$
 * @since 2.9.4
 */
public class InternalReporterManager extends AbstractReporterManager {

    /**
     * Create an internal (dummy) reporter object
     *
     * @return new null
     */
    protected Reporter createNewReporter(String systemName, String userName) {
        return new AbstractReporter(systemName, userName) {
            /**
             *
             */
            private static final long serialVersionUID = -2544443870907571291L;

            public int getState() {
                return state;
            }

            public void setState(int s) {
                state = s;
            }
            int state = 0;
        };
    }

    @Override
    public boolean allowMultipleAdditions(String systemName) {
        return true;
    }

    public String getSystemPrefix() {
        return "I";
    }
}

/* @(#)InternalReporterManager.java */
