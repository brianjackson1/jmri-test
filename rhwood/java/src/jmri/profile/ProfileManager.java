package jmri.profile;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import jmri.beans.Bean;
import jmri.util.FileUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage JMRI configuration profiles.
 *
 * @author rhwood
 */
public class ProfileManager extends Bean {

    private final ArrayList<Profile> profiles = new ArrayList<Profile>();
    private final ArrayList<Profile> disabledProfiles = new ArrayList<Profile>();
    private final ArrayList<File> searchPaths = new ArrayList<File>();
    private Profile activeProfile = null;
    private final File catalog;
    private File configFile = null;
    private boolean readingProfiles = false;
    private boolean autoStartActiveProfile = true;
    private static ProfileManager instance = null;
    public static final String ACTIVE_PROFILE = "activeProfile"; // NOI18N
    private static final String AUTO_START = "autoStart"; // NOI18N
    private static final String CATALOG = "profiles.xml"; // NOI18N
    private static final String PROFILE = "profile"; // NOI18N
    public static final String PROFILES = "profiles"; // NOI18N
    public static final String DISABLED_PROFILES = "disabledProfiles"; // NOI18N
    private static final String PROFILECONFIG = "profileConfig"; // NOI18N
    public static final String SEARCH_PATHS = "searchPaths"; // NOI18N
    public static final String SYSTEM_PROPERTY = "org.jmri.profile"; // NOI18N
    private static final Logger log = LoggerFactory.getLogger(ProfileManager.class);

    public ProfileManager() {
        this.catalog = new File(FileUtil.getPreferencesPath() + CATALOG);
        try {
            this.readProfiles();
            this.findProfiles();
        } catch (JDOMException ex) {
            log.error(ex.getLocalizedMessage(), ex);
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    public static ProfileManager defaultManager() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    public Profile getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String id) {
        if (id == null) {
            Profile old = activeProfile;
            activeProfile = null;
            FileUtil.setProfilePath(null);
            this.firePropertyChange(ProfileManager.ACTIVE_PROFILE, old, null);
            return;
        }
        for (Profile p : profiles) {
            if (p.getId().equals(id)) {
                this.setActiveProfile(p);
                return;
            }
        }
        log.warn("Unable to set active profile. No profile with id {} could be found.", id);
    }

    public void setActiveProfile(Profile profile) {
        Profile old = activeProfile;
        if (profile == null) {
            activeProfile = null;
            FileUtil.setProfilePath(null);
            this.firePropertyChange(ProfileManager.ACTIVE_PROFILE, old, null);
            return;
        }
        activeProfile = profile;
        FileUtil.setProfilePath(profile.getPath().toString());
        this.firePropertyChange(ProfileManager.ACTIVE_PROFILE, old, profile);
    }

    public void saveActiveProfile() throws IOException {
        this.saveActiveProfile(this.getActiveProfile(), this.autoStartActiveProfile);
    }

    protected void saveActiveProfile(Profile profile, boolean autoStart) throws IOException {
        Properties p = new Properties();
        FileOutputStream os = null;

        if (profile != null) {
            p.setProperty(ACTIVE_PROFILE, profile.getId());
            p.setProperty(AUTO_START, Boolean.toString(autoStart));
        }
        if (!this.configFile.exists() && !this.configFile.createNewFile()) {
            throw new IOException("Unable to create file at " + this.getConfigFile().getAbsolutePath()); // NOI18N
        }
        try {
            os = new FileOutputStream(this.getConfigFile());
            p.storeToXML(os, "Active profile configuration (saved at " + (new Date()).toString() + ")"); // NOI18N
            os.close();
        } catch (IOException ex) {
            if (os != null) {
                os.close();
            }
            throw ex;
        }

    }

    public void readActiveProfile() throws IOException {
        Properties p = new Properties();
        FileInputStream is = null;
        if (this.configFile.exists()) {
            try {
                is = new FileInputStream(this.getConfigFile());
                p.loadFromXML(is);
                is.close();
            } catch (IOException ex) {
                if (is != null) {
                    is.close();
                }
                throw ex;
            }
            this.setActiveProfile(p.getProperty(ACTIVE_PROFILE));
            if (p.containsKey(AUTO_START)) {
                this.setAutoStartActiveProfile(Boolean.parseBoolean(p.getProperty(AUTO_START)));
            }
        }
    }

    public Profile[] getProfiles() {
        return profiles.toArray(new Profile[profiles.size()]);
    }

    public Profile getProfiles(int index) {
        if (index >= 0 && index < profiles.size()) {
            return profiles.get(index);
        }
        return null;
    }

    public void setProfiles(Profile profile, int index) {
        Profile oldProfile = profiles.get(index);
        if (!this.readingProfiles) {
            profiles.set(index, profile);
            this.fireIndexedPropertyChange(PROFILES, index, oldProfile, profile);
        }
    }

    public Profile[] getDisabledProfiles() {
        return disabledProfiles.toArray(new Profile[disabledProfiles.size()]);
    }

    public Profile getDisabledProfiles(int index) {
        if (index >= 0 && index < disabledProfiles.size()) {
            return disabledProfiles.get(index);
        }
        return null;
    }

    public void setDisabledProfiles(Profile profile, int index) {
        Profile oldProfile = profiles.get(index);
        if (!this.readingProfiles) {
            disabledProfiles.set(index, profile);
            this.fireIndexedPropertyChange(DISABLED_PROFILES, index, oldProfile, profile);
        }
    }

    protected void addProfile(Profile profile) {
        if (profile.isDisabled()) {
            if (!disabledProfiles.contains(profile)) {
                disabledProfiles.add(profile);
                if (!this.readingProfiles) {
                    int index = disabledProfiles.indexOf(profile);
                    this.fireIndexedPropertyChange(DISABLED_PROFILES, index, null, profile);
                }
            }
            return;
        }
        if (!profiles.contains(profile)) {
            profiles.add(profile);
            if (!this.readingProfiles) {
                int index = profiles.indexOf(profile);
                this.fireIndexedPropertyChange(PROFILES, index, null, profile);
                try {
                    this.writeProfiles();
                } catch (IOException ex) {
                    log.warn("Unable to write profiles while adding profile {}.", profile.getId(), ex);
                }
            }
        }
    }

    protected void removeProfile(Profile profile) {
        try {
            int index = profiles.indexOf(profile);
            if (index >= 0) {
                if (profiles.remove(profile)) {
                    this.fireIndexedPropertyChange(PROFILES, index, profile, null);
                    this.writeProfiles();
                }
            } else {
                index = disabledProfiles.indexOf(profile);
                if (disabledProfiles.remove(profile)) {
                    this.fireIndexedPropertyChange(DISABLED_PROFILES, index, profile, null);
                    this.writeProfiles();
                }
            }
        } catch (IOException ex) {
            log.warn("Unable to write profiles while removing profile {}.", profile.getId(), ex);
        }
    }

    public File[] getSearchPaths() {
        return searchPaths.toArray(new File[searchPaths.size()]);
    }

    public File getSearchPaths(int index) {
        if (index >= 0 && index < searchPaths.size()) {
            return searchPaths.get(index);
        }
        return null;
    }

    protected void addSearchPath(File path) {
        if (!searchPaths.contains(path)) {
            searchPaths.add(path);
            if (!this.readingProfiles) {
                int index = searchPaths.indexOf(path);
                this.fireIndexedPropertyChange(SEARCH_PATHS, index, null, path);
            }
            this.findProfiles();
        }
    }

    protected void removeSearchPath(File path) {
        if (searchPaths.contains(path)) {
            int index = searchPaths.indexOf(path);
            searchPaths.remove(path);
            this.fireIndexedPropertyChange(SEARCH_PATHS, index, path, null);
        }
    }

    private void readProfiles() throws JDOMException, IOException {
        try {
            boolean reWrite = false;
            if (!catalog.exists()) {
                this.writeProfiles();
            }
            if (!catalog.canRead()) {
                return;
            }
            this.readingProfiles = true;
            Document doc = (new SAXBuilder()).build(catalog);
            profiles.clear();
            for (Element e : (List<Element>) doc.getRootElement().getChild(PROFILES).getChildren()) {
                File pp = FileUtil.getFile(FileUtil.getExternalFilename(e.getAttributeValue(Profile.PATH)));
                try {
                    this.addProfile(new Profile(pp));
                } catch (FileNotFoundException ex) {
                    log.info("Cataloged profile \"{}\" not in expected location\nSearching for it in {}", e.getAttributeValue(Profile.ID), pp.getParentFile());
                    this.findProfiles(pp.getParentFile());
                    reWrite = true;
                }
            }
            searchPaths.clear();
            for (Element e : (List<Element>) doc.getRootElement().getChild(SEARCH_PATHS).getChildren()) {
                File path = FileUtil.getFile(FileUtil.getExternalFilename(e.getAttributeValue(Profile.PATH)));
                if (!searchPaths.contains(path)) {
                    this.addSearchPath(path);
                }
            }
            if (searchPaths.isEmpty()) {
                this.addSearchPath(FileUtil.getFile(FileUtil.getPreferencesPath()));
            }
            this.readingProfiles = false;
            if (reWrite) {
                this.writeProfiles();
            }
        } catch (JDOMException ex) {
            this.readingProfiles = false;
            throw ex;
        } catch (IOException ex) {
            this.readingProfiles = false;
            throw ex;
        }
    }

    private void writeProfiles() throws IOException {
        FileWriter fw = null;
        Document doc = new Document();
        doc.setRootElement(new Element(PROFILECONFIG));
        Element profilesElement = new Element(PROFILES);
        Element pathsElement = new Element(SEARCH_PATHS);
        for (Profile p : this.profiles) {
            Element e = new Element(PROFILE);
            e.setAttribute(Profile.ID, p.getId());
            e.setAttribute(Profile.PATH, FileUtil.getPortableFilename(p.getPath(), true));
            profilesElement.addContent(e);
        }
        for (File f : this.searchPaths) {
            Element e = new Element(Profile.PATH);
            e.setAttribute(Profile.PATH, FileUtil.getPortableFilename(f.getPath(), true));
            pathsElement.addContent(e);
        }
        doc.getRootElement().addContent(profilesElement);
        doc.getRootElement().addContent(pathsElement);
        try {
            fw = new FileWriter(catalog);
            (new XMLOutputter(Format.getPrettyFormat())).output(doc, fw);
            fw.close();
        } catch (IOException ex) {
            // close fw if possible
            if (fw != null) {
                fw.close();
            }
            // rethrow the error
            throw ex;
        }
    }

    private void findProfiles() {
        for (File searchPath : this.searchPaths) {
            this.findProfiles(searchPath);
        }
    }

    private void findProfiles(File searchPath) {
        File[] profilePaths = searchPath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.isDirectory() && Arrays.asList(pathname.list()).contains(Profile.PROPERTIES));
            }
        });
        for (File pp : profilePaths) {
            try {
                this.addProfile(new Profile(pp));
            } catch (IOException ex) {
                log.error("Error attempting to read Profile at {}", pp, ex);
            }
        }
    }

    /**
     * @return the configFile
     */
    public File getConfigFile() {
        return configFile;
    }

    /**
     * @param configFile the configFile to set
     */
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    /**
     * @return the autoStartActiveProfile
     */
    public boolean isAutoStartActiveProfile() {
        return (this.getActiveProfile() != null && autoStartActiveProfile);
    }

    /**
     * @param autoStartActiveProfile the autoStartActiveProfile to set
     */
    public void setAutoStartActiveProfile(boolean autoStartActiveProfile) {
        this.autoStartActiveProfile = autoStartActiveProfile;
    }

    protected void disableProfile(Profile p) throws IOException {
        this.removeProfile(p);
        if (!p.isDisabled()) {
            p.setDisabled(true);
        }
        this.addProfile(p);
    }

    protected void enableProfile(Profile p) throws IOException {
        this.removeProfile(p);
        if (p.isDisabled()) {
            p.setDisabled(false);
        }
        this.addProfile(p);
    }

    /**
     * Create a default profile if no profiles exist.
     *
     * @return A new profile or null if profiles already exist.
     * @throws IOException
     */
    public static Profile createDefaultProfile() throws IllegalArgumentException, IOException {
        if (ProfileManager.defaultManager().profiles.isEmpty()) {
            String pn = "My JMRI Profile";
            String pid = FileUtil.sanitizeFilename(pn);
            File pp = new File(FileUtil.getPreferencesPath() + pid);
            Profile profile = new Profile(pn, pid, pp);
            ProfileManager.defaultManager().addProfile(profile);
            log.info("Created default profile \"{}\"", pn);
            return profile;
        } else {
            return null;
        }
    }

    /**
     * Copy a JMRI configuration not in a profile and its user preferences to a
     * profile.
     *
     * @param config
     * @param name
     * @return The profile with the migrated configuration.
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static Profile migrateConfigToProfile(File config, String name) throws IllegalArgumentException, IOException {
        String pid = FileUtil.sanitizeFilename(name);
        File pp = new File(FileUtil.getPreferencesPath(), pid);
        Profile profile = new Profile(name, pid, pp);
        FileUtil.copy(config, new File(profile.getPath(), Profile.CONFIG_FILENAME));
        FileUtil.copy(new File(config.getParentFile(), "UserPrefs" + config.getName()), new File(profile.getPath(), "UserPrefs" + Profile.CONFIG_FILENAME)); // NOI18N
        ProfileManager.defaultManager().addProfile(profile);
        log.info("Migrated \"{}\" config to profile \"{}\"", name, name);
        return profile;
    }
}
