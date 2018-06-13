/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.php4j;

import ca.weblite.php4j.nativeutils.NativeUtils;
import java.io.File;
import java.io.IOException;

/**
 * Loads the native PHP libs from either the classpath (if using PHP4J-fat.jar),
 * or from Github (if using PHP4J-thin.jar).
 * @author Steve Hannah
 */
public class PHPLoader {
    
    /**
     * The install directory.
     * @see #getInstallDir() 
     * @see #setInstallDir(java.io.File) 
     */
    File php4JDir = new File(
            new File(System.getProperty("user.home")),
            ".php4j");
    
    
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void detectOS() {
        if (isWindows()) {

        } else if (isMac()) {

        } else if (isUnix()) {

        } else {

        }
    }

    static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    static boolean isUnix() {
        return (OS.indexOf("nux") >= 0);
    }
    
    /**
     * Gets the name of the zip file containing the PHP distribution
     * for the current platform.
     * @return 
     */
    private String getZipName() {
        if (isWindows()) {
            return "php4j-win.zip";
        } else if (isMac()) {
            return "php4j-macos.zip";
        } else if (isUnix()){
            return "php4j-linux.zip";
        } else {
            return "php4j-unknown.zip";
        }
    }
    
    /**
     * Gets the path (in the classpath) for the zip file containing
     * the PHP native lib.
     * @return 
     */
    private String getZipPath() {
        
        
        return "/ca/weblite/php4j/native/" + getZipName();
    }
    
    /**
     * Gets the URL to download the native lib zip.
     * @return 
     */
    private String getZipUrl() {
        return "https://github.com/shannah/php4j/blob/master/bin/native/"+getZipName()+"?raw=true";
    }
    
    /**
     * Deletes the install directory.
     * @throws IOException 
     */
    public void uninstall() throws IOException {
        if (php4JDir.exists()) {
            System.out.println("Deleting "+php4JDir);
            NativeUtils.delTree(php4JDir);
        }
    }
    
    /**
     * Gets the directory where PHP is installed (or will be installed after 
     * loading.
     * @return The PHP directory.
     */
    public File getPHPDir() {
        return new File(php4JDir, "php");
    }
    
    /**
     * Loads the native PHP libraries.
     * @param forceReload If true it will delete any existing installation at
     * {@link #getInstallDir() }.  If false, it will just use the existing install
     * if found.
     * 
     * <p>Note: If you are using PHP4J-fat.jar, then the native libs will be 
     * loaded from the classpath.  If you are using PHP4J-thin.jar, it will
     * download them from github.</p>
     * @return The PHP directory.
     * @throws IOException If there is a problem loading it.
     */
    public File load(boolean forceReload) throws IOException {
        if (!forceReload && getPHPDir().exists()) {
            return getPHPDir();
        }
        File bundledZip = null;
        try {
            bundledZip = NativeUtils.loadFileFromJar(getZipPath(), PHPLoader.class);
        } catch (IOException ex) {
            bundledZip = NativeUtils.loadFileFromURL(getZipUrl());
        }
        try {
            if (bundledZip != null) {
                if (getPHPDir().exists()) {
                    NativeUtils.delTree(getPHPDir());
                }
                getPHPDir().getParentFile().mkdirs();
                NativeUtils.extractZipTo(bundledZip, getPHPDir());


            }
            if (!getPHPDir().exists()) {
                throw new IOException("No PHP was found bundled");
            }

            File phpIni = new File(getPHPDir(), "php.ini");
            if (isWindows()) {
                String phpIniContents = NativeUtils.readFileToString(phpIni, "UTF-8");

                phpIniContents = phpIniContents.replace("C:\\xampp\\php", getPHPDir().getAbsolutePath());
                NativeUtils.writeFileToString(phpIni, phpIniContents, "UTF-8");
            } else if (isMac()) {
                String phpIniContents = NativeUtils.readFileToString(phpIni, "UTF-8");

                phpIniContents = phpIniContents.replace("/Applications/XAMPP/xamppfiles", getPHPDir().getAbsolutePath());
                NativeUtils.writeFileToString(phpIni, phpIniContents, "UTF-8");
            } else if (isUnix()) {
                String phpIniContents = NativeUtils.readFileToString(phpIni, "UTF-8");

                phpIniContents = phpIniContents.replace("/opt/lampp", getPHPDir().getAbsolutePath());
                NativeUtils.writeFileToString(phpIni, phpIniContents, "UTF-8");
            }
            return getPHPDir();
        } finally {
            if (bundledZip != null && bundledZip.exists()) {
                bundledZip.delete();
            }
        }
    }
    
    /**
     * Sets the directory that native libs (e.g. PHP) are loaded into.
     * @param installDir The install dir.
     */
    public void setInstallDir(File installDir) {
        php4JDir = installDir;
    }
    
    /**
     * Gets the directory that native libs (e.g. PHP) are loaded into.
     * @return The install directory.
     */
    public File getInstallDir() {
        return php4JDir;
    }
}
