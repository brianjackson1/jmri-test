package jmri;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * <hr>
 * This file is part of JMRI.
 * <P>
 * JMRI is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <P>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 * <P>
 *
 * @author			Kevin Dickerson Copyright (C) 2011
 * @version			$Revision$
 */

public class NamedBeanHandleManager extends jmri.managers.AbstractManager implements java.io.Serializable{

    public NamedBeanHandleManager(){
        super();
    }
    @SuppressWarnings("unchecked")
    //Checks are performed to make sure that the beans are the same type before being returned
    public <T> NamedBeanHandle<T> getNamedBeanHandle(String name, T bean){
        if (bean==null || name==null || name.equals(""))
            return null;
        NamedBeanHandle<T> temp = new NamedBeanHandle<T>(name, bean);
        
        for (NamedBeanHandle h : namedBeanHandles ) {
            if (temp.equals(h)){
                temp = null;
                return h;
            }
        }
        namedBeanHandles.add(temp);
        return temp;
    }
    
    /**
    * A Method to update the name on a bean.
    * Note this does not change the name on the bean, it only changes the references
    * 
    */
    public <T> void renameBean(String oldName, String newName, T bean){
        
        /*Gather a list of the beans in the system with the oldName ref.
        Although when we get a new bean we always return the first one that exists
        when a rename is performed it doesn't delete the bean with the old name 
        it simply updates the name to the new one. So hence you can end up with
        multiple named bean entries for one name.
        */
        NamedBeanHandle<T> oldBean = new NamedBeanHandle<T>(oldName, bean);
        for (NamedBeanHandle h : namedBeanHandles ) {
            if (oldBean.equals(h)){
                h.setName(newName);
            }
        }
        oldBean=null;
    }
    
    /**
    *  A method to effectivily move a name from one bean to another.
    *  This method only updates the references to point to the new bean
    *  It does not move the name provided from one bean to another.
    */
    @SuppressWarnings("unchecked")
    //Checks are performed to make sure that the beans are the same type before being moved
    public <T> void moveBean(T oldBean, T newBean, String name){
        /*Gather a list of the beans in the system with the oldBean ref.
        Although when a new bean is requested, we always return the first one that exists
        when a move is performed it doesn't delete the namedbeanhandle with the oldBean
        it simply updates the bean to the new one. So hence you can end up with
        multiple bean entries with the same name.
        */
        
        NamedBeanHandle<T> oldNamedBean = new NamedBeanHandle<T>(name, oldBean);
        for (NamedBeanHandle h : namedBeanHandles ) {
            if (oldNamedBean.equals(h))
                h.setBean(newBean);
        }
        oldNamedBean=null;
    }
    
    public void updateBeanFromUserToSystem(NamedBean bean){
        String systemName = bean.getSystemName();
        for (NamedBeanHandle h : namedBeanHandles ) {
            if (h.getBean().equals(bean))
                h.setName(systemName);
        }
    }
    
    public void updateBeanFromSystemToUser(NamedBean bean){
        String userName = bean.getUserName();
        if((userName==null) || (userName.equals(""))){
            log.error("UserName is empty, can not update items to use UserName");
            return;
        }
        
        for (NamedBeanHandle h : namedBeanHandles ) {
            if (h.getBean().equals(bean))
                h.setName(userName);
        }
    }
    
    public <T> boolean inUse(String name, T bean){
        NamedBeanHandle<T> temp = new NamedBeanHandle<T>(name, bean);
        for (NamedBeanHandle h : namedBeanHandles ) {
            if (temp.equals(h)){
                temp = null;
                return true;
            }
        }
        return false;
    }

    public <T> NamedBeanHandle<T> newNamedBeanHandle(String name, T bean, Class<T> type){
        return getNamedBeanHandle(name, bean);
    }

    // abstract methods to be extended by subclasses
    // to free resources when no longer used
    @Override
    public void dispose() {
        super.dispose();
    }
    
    ArrayList<NamedBeanHandle> namedBeanHandles = new ArrayList<NamedBeanHandle>();
    
    /**
     * Don't want to store this information
     */
    @Override
    protected void registerSelf() {}

    @Override
    public char systemLetter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSystemPrefix() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public char typeLetter() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String makeSystemName(String s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getSystemNameArray() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getSystemNameList() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    java.beans.PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);

    @Override
    public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    protected void firePropertyChange(String p, Object old, Object n) { pcs.firePropertyChange(p,old,n);}

    @Override
    public void register(NamedBean n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deregister(NamedBean n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    protected int getXMLOrder(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(NamedBeanHandleManager.class.getName());
}
