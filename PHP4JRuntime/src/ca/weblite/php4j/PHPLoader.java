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
 *
 * @author shannah
 */
public class PHPLoader {
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
    
    private String getZipPath() {
        
        
        return "/ca/weblite/php4j/native/" + getZipName();
    }
    
    private String getZipUrl() {
        return "https://github.com/shannah/php4j/blob/master/bin/native/"+getZipName()+"?raw=true";
    }
    
    public void uninstall() throws IOException {
        if (php4JDir.exists()) {
            System.out.println("Deleting "+php4JDir);
            NativeUtils.delTree(php4JDir);
        }
    }
    
    public File getPHPDir() {
        return new File(php4JDir, "php");
    }
    
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
    
    
}
