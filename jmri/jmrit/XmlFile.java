// XmlFile.java

package jmri.jmrit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.List;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.DocType;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Handle common aspects of XML files.
 *<P>
 * JMRI needs to be able to operate offline, so it needs to store
 * DTDs locally.  At the same time, we want XML files to be 
 * transportable, and to have their DTDs accessible via the web
 * (for browser rendering).  
 * Further, our code assumes that default values for attributes will
 * be provided, and it's necessary to read the DTD for that to work.
 *<p>
 * We implement this using our own EntityResolvor, the 
 * {@link jmri.util.JmriLocalEntityResolver} class.
 *
 * @author	Bob Jacobsen   Copyright (C) 2001, 2002, 2007
 * @version	$Revision: 1.37 $
 */
public abstract class XmlFile {

    /**
     * Define root part of URL for 
     * XSLT style page processing instructions.
     *<p>
     * See the <A HREF="http://jmri.sourceforge.net/help/en/html/doc/Technical/XmlUsage.shtml#xslt">XSLT versioning discussion</a>
     */
    static final public String xsltLocation = "/xml/XSLT/";
    
    /**
     * Read the contents of an XML file from its filename.  
     * The name is expanded by the {@link #findFile}
     * routine.
     * @param name Filename, as needed by {@link #findFile}
     * @throws org.jdom.JDOMException
     * @throws java.io.FileNotFoundException
     * @return null if not found, else root element of located file
     */
    public Element rootFromName(String name) throws org.jdom.JDOMException, java.io.IOException {

        File fp = findFile(name);
        if (fp != null) {
            if (log.isDebugEnabled()) log.debug("readFile: "+name+" from "+fp.getAbsolutePath());
            return rootFromFile(fp);
        }
        else {
            log.warn("Did not find file "+name+" in "+prefsDir()+" or "+xmlDir());
            return null;
        }
    }

    /**
     * Read a File as XML, and return the root object.
     *
     * Multiple methods are tried to locate the DTD needed to do this.
     * Exceptions are only thrown when local recovery is impossible.
     *
     * @throws org.jdom.JDOMException only when all methods have failed
     * @throws java.io.FileNotFoundException
     * @param file File to be parsed.  A FileNotFoundException is thrown if it doesn't exist.
     * @return root element from the file. This should never be null, as an
     *          exception should be thrown if anything goes wrong.
     */
    public Element rootFromFile(File file) throws org.jdom.JDOMException, java.io.IOException {
        Element e;
        try {
            InputStream stream = new BufferedInputStream(new FileInputStream(file));
            return getRootViaURI(verify, stream);
        }
        catch (org.jdom.input.JDOMParseException e4) {
            // file opened, but couldn't be parsed; report that up
            throw e4;
        }
        catch (org.jdom.JDOMException e1) {
            // 1st attempt failed, try second using deprecated method
            if (!openWarn1) reportError1(file.getName(), e1);
            openWarn1 = true;
            try {
                InputStream stream = new BufferedInputStream(new FileInputStream(file));
                e = getRootViaURL(verify, stream);
                log.info("getRootViaURL succeeded as 2nd try");
                return e;
            }
            catch (org.jdom.JDOMException e2) {
                // 2nd attempt failed, try third.
                if (!openWarn2) reportError2(file.getName(), e2);
                openWarn2 = true;
                // All exceptions are allowed to propagate out, 
                // as we have no retry algorithms left
                InputStream stream = new BufferedInputStream(new FileInputStream(file));
                e = getRootViaRelative(verify, stream);
                log.info("GetRootViaRelative succeeded as 3rd try");
                new Exception().printStackTrace();
                return e;
            }
            // other errors are allowed to propagate out
        }
        // other errors are allowed to propagate out
    }

    /**
     * Read a URL as XML, and return the root object.
     *
     * Multiple methods are tried to locate the DTD needed to do this.
     * Exceptions are only thrown when local recovery is impossible.
     *
     * @throws org.jdom.JDOMException only when all methods have failed
     * @throws java.io.FileNotFoundException
     * @param file File to be parsed.  A FileNotFoundException is thrown if it doesn't exist.
     * @return root element from the file. This should never be null, as an
     *          exception should be thrown if anything goes wrong.
     */
    public Element rootFromURL(URL url) throws org.jdom.JDOMException, java.io.IOException {
        Element e;
        try {
            InputStream stream = new BufferedInputStream(url.openConnection().getInputStream());
            return getRootViaURI(verify, stream);
        }
        catch (org.jdom.input.JDOMParseException e4) {
            // file opened, but couldn't be parsed; report that up
            throw e4;
        }
        catch (org.jdom.JDOMException e1) {
            // 1st attempt failed, try second using deprecated method
            if (!openWarn1) reportError1(url.toString(), e1);
            openWarn1 = true;
            try {
                InputStream stream = new BufferedInputStream(url.openConnection().getInputStream());
                e = getRootViaURL(verify, stream);
                log.info("getRootViaURL succeeded as 2nd try");
                return e;
            }
            catch (org.jdom.JDOMException e2) {
                // 2nd attempt failed, try third.
                if (!openWarn2) reportError2(url.toString(), e2);
                openWarn2 = true;
                // All exceptions are allowed to propagate out, 
                // as we have no retry algorithms left
                InputStream stream = new BufferedInputStream(url.openConnection().getInputStream());
                e = getRootViaRelative(verify, stream);
                log.info("GetRootViaRelative succeeded as 3rd try");
                new Exception().printStackTrace();
                return e;
            }
            // other errors are allowed to propagate out
        }
        // other errors are allowed to propagate out
    }

    static boolean openWarn1 = false;
    static boolean openWarn2 = false;
    static boolean openWarn3 = false;
    
    /**
     * Specify a standard prefix for DTDs in new XML documents
     */
    static public final String dtdLocation = "";
    
    // made members for overriding in tests
    protected void reportError1(String name, Exception e) {
        log.warn("Failed to open "+name+" on 1st attempt, error was: "+e);
    }
    protected void reportError2(String name, Exception e) {
        log.warn("Failed to open on 2nd attempt, error was: "+e);
    }
    
    /**
     * Find the DTD via a relative path and get the root element.
     * @deprecated 1.8
     */
    private Element getRootViaRelative(boolean verify, InputStream stream) throws org.jdom.JDOMException, java.io.IOException {

        // Invoke a utility service routine to provide the URL for DTDs
        String dtdUrl = "file:xml"+File.separator+"DTD"+File.separator;

        if (log.isDebugEnabled()) log.debug("getRootViaRelative, DTD via:"+dtdUrl);

        // Open and parse file
        SAXBuilder builder = new SAXBuilder(verify);  // argument controls validation
        
        builder.setEntityResolver(new jmri.util.JmriLocalEntityResolver());
        
        Document doc = builder.build(new BufferedInputStream(stream),dtdUrl);

        // find root
        return doc.getRootElement();
    }
    
    /**
     * Find the DTD via a URL and get the root element.
     * @deprecated 1.8
     * 
     */
    private Element getRootViaURL(boolean verify, InputStream stream) throws org.jdom.JDOMException, java.io.IOException {
        
        // Invoke a utility service routine to provide the URL for DTDs
        String dtdpath = "xml"+File.separator+"DTD"+File.separator;
        File dtdFile = new File(dtdpath);
        String dtdUrl = jmri.util.FileUtil.getUrl(dtdFile);

        if (log.isDebugEnabled()) log.debug("getRootViaURL, DTD URL:"+dtdUrl);

        // Open and parse file
        SAXBuilder builder = new SAXBuilder(verify);  // argument controls validation
        
        builder.setEntityResolver(new jmri.util.JmriLocalEntityResolver());
        
        Document doc = builder.build(new BufferedInputStream(stream),dtdUrl);

        // find root
        return doc.getRootElement();
    }
    
    /**
     * Find the DTD via a URI and get the root element.
     * 
     */
    protected Element getRootViaURI(boolean verify, InputStream stream) throws org.jdom.JDOMException, java.io.IOException {
        // Invoke a utility service routine to provide the URL for DTDs
        String dtdpath = "xml"+File.separator+"DTD"+File.separator;
        File dtdFile = new File(dtdpath);
        String dtdUrl = jmri.util.FileUtil.getUrlViaUri(dtdFile);

        if (log.isDebugEnabled()) log.debug("getRootViaURI, DTD URI:"+dtdUrl);

        // Open and parse file
        SAXBuilder builder = new SAXBuilder(verify);  // argument controls validation
        
        builder.setEntityResolver(new jmri.util.JmriLocalEntityResolver());

        Document doc = builder.build(new BufferedInputStream(stream), dtdUrl);

        // find root
        return doc.getRootElement();
    }
    
    /**
     * Write a File as XML.
     * @throws org.jdom.JDOMException
     * @throws java.io.FileNotFoundException
     * @param file File to be created.
     * @param doc Document to be written out. This should never be null.
     */
    public void writeXML(File file, Document doc) throws org.jdom.JDOMException, java.io.IOException, java.io.FileNotFoundException {
        // write the result to selected file
        java.io.FileOutputStream o = new java.io.FileOutputStream(file);
        XMLOutputter fmt = new XMLOutputter();
        
        fmt.setFormat(org.jdom.output.Format.getPrettyFormat());
        
        fmt.output(doc, o);
        o.close();
    }

    /**
     * Check if a file of the given name exists. This uses the 
     * same search order as {@link #findFile} 
     *
     * @param name file name, either absolute or relative
     * @return true if the file exists in a searched place
     */
    protected boolean checkFile(String name) {
        File fp = new File(name);
        if (fp.exists()) return true;
        fp = new File(prefsDir()+name);
        if (fp.exists()) {
            return true;
        }
        else {
            File fx = new File(xmlDir()+name);
            if (fx.exists()) {
                return true;
            }
            else {
                return false;
            }
        }
    }


    /**
     * Return a File object for a name. This is here to implement the
     * search rule:
     * <OL>
     * <LI>Check for absolute name.
     * <LI>If not found look in user preferences directory, located by {@link #prefsDir}
     * <LI>If still not found, look in distribution directory, located by {@link #xmlDir}
     * </OL>
     * @param name Filename perhaps containing
     *               subdirectory information (e.g. "decoders/Mine.xml")
     * @return null if file found, otherwise the located File
     */
    protected File findFile(String name) {
        File fp = new File(name);
        if (fp.exists()) return fp;
        fp = new File(prefsDir()+name);
        if (fp.exists()) {
            return fp;
        }
        else {
            File fx = new File(xmlDir()+name);
            if (fx.exists()) {
                return fx;
            }
            else {
                return null;
            }
        }
    }


    /**
     * Diagnostic printout of as much as we can find
     * @param name Element to print, should not be null
     */
    static public void dumpElement(Element name) {
        List l = name.getChildren();
        for (int i = 0; i<l.size(); i++) {
            System.out.println(" Element: "+((Element)l.get(i)).getName()+" ns: "+((Element)l.get(i)).getNamespace());
        }
    }

    /**
     * Move original file to a backup. Use this before writing out a new version of the file.
     * @param name Last part of file pathname i.e. subdir/name, without the
     *               pathname for either the xml or preferences directory.
     */
    public void makeBackupFile(String name) {
		File file = findFile(name);
		if (file == null) {
			log.info("No " + name + " file to backup");
		} else {
			String backupName = backupFileName(file.getAbsolutePath());
			File backupFile = findFile(backupName);
			if (backupFile != null) {
				if (backupFile.delete())
					log.debug("deleted backup file " + backupName);
			}
			if (file.renameTo(new File(backupName)))
				log.debug("created new backup file " + backupName);
			else
				log.error("could not create backup file " + backupName);
		}
	}

    /**
     * Revert to original file from backup. Use this for testing backup files.
     * @param name Last part of file pathname i.e. subdir/name, without the
     *               pathname for either the xml or preferences directory.
     */
    public void revertBackupFile(String name) {
		File file = findFile(name);
		if (file == null) {
			log.info("No " + name + " file to revert");
		} else {
			String backupName = backupFileName(file.getAbsolutePath());
			File backupFile = findFile(backupName);
			if (backupFile != null) {
        			log.info("No " + backupName + " backup file to revert");
				if (file.delete())
					log.debug("deleted original file " + name);
			}
			if (backupFile.renameTo(new File(name)))
				log.debug("created original file " + name);
			else
				log.error("could not create original file " + name);
		}
	}

    /**
	 * Return the name of a new, unique backup file. This is here so it can be
	 * overridden during tests. File to be backed-up must be within the
	 * preferences directory tree.
	 * 
	 * @param name
	 *            Filename without preference path information, e.g.
	 *            "decoders/Mine.xml".
	 * @return Complete filename, including path information into preferences
	 *         directory
	 */
    public String backupFileName(String name) {
        String f = name+".bak";
        if (log.isDebugEnabled()) log.debug("backup file name is "+f);
        return f;
    }

    /**
     * Ensure that a subdirectory is present; if not, create it.
     * @param name Complete pathname of directory to be checked/created.
     */
    static public void ensurePrefsPresent(String name) {
        File f = new File(name);
        if (! f.exists()) {
            log.warn("Creating a missing preferences directory: "+name);
            f.mkdirs();
        }
    }

    /**
     * Create the Document object to store a particular root Element.
     *
     * @param root Root element of the final document
     * @param dtd name of an external DTD
     * @return new Document, with root installed
     */
    static public Document newDocument(Element root, String dtd) {
        Document doc = new Document(root);
        doc.setDocType(new DocType(root.getName(),dtd));
        addDefaultInfo(root);
        return doc;
    }

    /**
     * Add default information to the XML before writing it out.
     * <P>
     * Currently, this is identification information as an XML comment. This includes:
     * <UL>
     * <LI>The JMRI version used
     * <LI>Date of writing
     * <LI>A CVS id string, in case the file gets checked in or out
     * </UL>
     * <P>
     * It may be necessary to extend this to check whether the info is
     * already present, e.g. if re-writing a file.
     * @param root The root element of the document that will be written.
     */
    static public void addDefaultInfo(Element root) {
        String content = "Written by JMRI version "+jmri.Version.name()
                        +" on "+(new java.util.Date()).toString()
                        +" $Id: XmlFile.java,v 1.37 2008-11-17 02:58:26 jacobsen Exp $";
        Comment comment = new Comment(content);
        root.addContent(comment);
    }

    /**
     * Define the location of XML files within the distribution
     * directory. <P>
     * Because the programs runtime working directory is also the
     * distribution directory, we just use a relative file name.
     */
    static public String xmlDir() {return "xml"+File.separator;}

    /**
     * Define the location of the preferences directory.  This is system-specific
     * ( "{user.home}" is used to represent the directory pointed to by the
     *  user.home system property):
     * <DL>
     * <DT>If the system property jmri.prefsdir is present, it's used as a path name
     * <DT>Linux<DD>{user.home}/.jmri/
     * <DT>Windows<DD>{user.home}\JMRI
     * <DT>MacOS "Classic"<DD>{user.home}:JMRI
     * <DT>MacOS X<DD>{user.home}/Library/Preferences/JMRI
     * <DT>Other<DD> In the JMRI folder/directory in the folder/directory
     *                  referenced by {user.home}
     * </DL>
     * @return Pathname in local form, with a terminating separator
     */
    static public String prefsDir() {
        // check for jmri.prefsdir
        String jmriPrefsDir = System.getProperty("jmri.prefsdir","");
        if (jmriPrefsDir.length()>0) return jmriPrefsDir+File.separator;
        
        // not present, work through other choices
        String osName       = System.getProperty("os.name","<unknown>");
        String mrjVersion   = System.getProperty("mrj.version","<unknown>");
        String userHome     = System.getProperty("user.home","");

        // add a File.separator to userHome here, so can ignore whether its empty later on
        if (!userHome.equals("")) userHome = userHome+File.separator;

        String result;          // no value; that allows compiler to check completeness of algorithm

        if ( !mrjVersion.equals("<unknown>")) {
            // Macintosh, test for OS X
            if (osName.toLowerCase().equals("mac os x")) {
                // Mac OS X
                result = userHome+"Library"+File.separator+"Preferences"
                    +File.separator+"JMRI"+File.separator;
            } else {
                // Mac Classic, by elimination. Check consistency of mrjVersion
                // with that assumption
                if (!(mrjVersion.charAt(0)=='2'))
                    log.error("Decided Mac Classic, but mrj.version is \""
                              +mrjVersion+"\" os.name is \""
                              +osName+"\"");
                // userHome is the overall preferences directory
                result = userHome+"JMRI"+File.separator;
            }
        } else if (osName.equals("Linux")) {
            // Linux, so use an invisible file
            result = userHome+".jmri"+File.separator;
        } else {
            // Could be Windows, other
            result = userHome+"JMRI"+File.separator;
        }

        if (log.isDebugEnabled()) log.debug("prefsDir defined as \""+result+
                                            "\" based on os.name=\""
                                            +osName
                                            +"\" mrj.version=\""
                                            +mrjVersion
                                            +"\" user.home=\""
                                            +userHome
                                            +"\"");
        return result;
    }

    static boolean verify = false;

    /**
     * Provide default initial location for JFileChoosers
     * to user files
     */ 
    public static String userFileLocationDefault() {
        return jmri.jmrit.XmlFile.prefsDir();
    }
    
    /**
     * Provide a JFileChooser initialized to the default
     * user location, and with a default filter.
     * @param filter Title for the filter, may not be null
     * @param suffix1 An allowed suffix, or null
     * @param suffix2 A second allowed suffix, or null. If both arguments are
     * null, no specific filtering is done.
     */
    public static javax.swing.JFileChooser userFileChooser(
            String filter, String suffix1, String suffix2) {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser(userFileLocationDefault());
        jmri.util.NoArchiveFileFilter filt = new jmri.util.NoArchiveFileFilter(filter);
        if (suffix1 != null) filt.addExtension(suffix1);
        if (suffix2 != null) filt.addExtension(suffix2);
        fc.setFileFilter(filt);
        return fc;
    }

    public static javax.swing.JFileChooser userFileChooser() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser(userFileLocationDefault());
        jmri.util.NoArchiveFileFilter filt = new jmri.util.NoArchiveFileFilter();
        fc.setFileFilter(filt);
        return fc;
    }
        
    public static javax.swing.JFileChooser userFileChooser(String filter) {
        return userFileChooser(filter, null, null);
    }
        
    public static javax.swing.JFileChooser userFileChooser(
            String filter, String suffix1) {
        return userFileChooser(filter, suffix1, null);
    }
        
    // initialize SAXbuilder
    static private SAXBuilder builder = new SAXBuilder(verify);  // argument controls validation, on for now

    // initialize logging
    static private org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(XmlFile.class.getName());

}
