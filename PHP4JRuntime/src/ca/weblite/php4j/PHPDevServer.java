/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.php4j;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shannah
 */
public class PHPDevServer implements AutoCloseable, Runnable {
    private String phpPath="php";
    private int port=0;
    private File documentRoot = new File(".").getAbsoluteFile();
    
    private Process proc;
    private Thread thread;
    private boolean running;
    private boolean ended;
    private final Object lock = new Object();
    private boolean useBundledPHP = true;
    
    public PHPDevServer() {
        
    }
    
    
    public void start() {
        thread = new Thread(this);
        thread.start();
        while (!isRunning() && !isEnded()) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(PHPDevServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        System.out.println("Finished starting...");
    }
    
    
    private static void inheritIO(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine()) {
                    dest.println(sc.nextLine());
                }
            }
        }).start();
    }

    
    public void run() {
        try {
            if (port == 0) {
                ServerSocket sock = new ServerSocket(0);
                port = sock.getLocalPort();
                sock.close();
            }
            String phpPath = getPhpPath();
            
            if (useBundledPHP) {
                PHPLoader phpLoader = new PHPLoader();
                File bundledPhpDir = phpLoader.load(false);
                File phpExe = new File(new File(bundledPhpDir, "bin"), "php");
                if (!phpExe.exists()) {
                    phpExe = new File(phpExe.getParentFile(), "php.exe");
                }
                if (!phpExe.exists()) {
                    phpExe = new File(bundledPhpDir, "php.exe");
                }
                if (!phpExe.exists()) {
                    throw new IOException("Bundled PHP executable not found");
                }
                phpExe.setExecutable(true);
                phpPath = phpExe.getAbsolutePath();
                    
            }
            String hostname = "0";
            if (PHPLoader.isWindows()) {
                hostname = InetAddress.getByName(null).getHostAddress();
            }
            //System.out.println("Starting server at "+hostname+":"+getPort());
            
            
            ProcessBuilder pb = new ProcessBuilder(phpPath, "-S", hostname+":"+getPort());
            if (useBundledPHP && PHPLoader.isWindows()) {
                File phpDir = new File(phpPath).getParentFile();
                File phpIni = new File(phpDir, "php.ini");
                if (phpIni.exists()) {
                    pb.command().add("-c");
                    pb.command().add(phpDir.getAbsolutePath());
                } else {
                    throw new IOException("Could not find php.ini file at "+phpIni.getAbsolutePath());
                }
                //pb.environment().put("PATH", new File(new File(phpPath).getParentFile(), "ext").getAbsolutePath()+File.pathSeparator+System.getenv("PATH"));
                //System.out.println(pb.environment());
                
                //pb.command().add("-d");
                //pb.command().add("extension_dir="+new File(new File(phpPath).getParentFile(), "ext").getAbsolutePath());
            }
            //System.out.println(pb.command());
            pb.directory(getDocumentRoot());
            
            pb.inheritIO();
            proc = pb.start();
            long startTime = System.currentTimeMillis();
            long timeout = 5000;
            String testFileName = "tmp-"+startTime+".txt";
            File testFile = new File(getDocumentRoot(), testFileName);
            if (!testFile.createNewFile()) {
                throw new IOException("Failed to create test file");
            }
            
            URL testUrl = new URL("http://localhost:"+getPort()+"/"+testFile.getName());
            boolean success = false;
            while (System.currentTimeMillis() - timeout < startTime) {
                try {
                    //System.out.println("Connecting to "+testUrl);
                    HttpURLConnection conn = (HttpURLConnection)testUrl.openConnection();
                    
                    conn.setUseCaches(false);
                    int responseCode = conn.getResponseCode();
                    //System.out.println("Response code "+responseCode);
                    if (responseCode == 200) {
                        success = true;
                        break;
                    }
                } catch (Throwable t) {
                    System.out.println(t.getMessage());
                    //t.printStackTrace();
                }
            }
            testFile.delete();
            if (!success) {
               
                throw new IOException("Failed to start PHP Server");
            }
            synchronized(lock) {
                running = true;
                lock.notifyAll();
            }
            //System.out.println("Now we are running");
            int res = proc.waitFor();
            //System.out.println("Result code "+res);
        } catch (IOException ex) {
            Logger.getLogger(PHPDevServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(PHPDevServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (proc != null && proc.isAlive()) {
                try {
                    proc.destroyForcibly();
                } catch (Throwable t){}
            }
            running = false;
            ended = true;
            synchronized(lock) {
                
                lock.notifyAll();
            }
        }
        
        
    }
    
    @Override
    public void close() throws Exception {
        System.out.println("Closing test runner");
        proc.destroyForcibly();
        running = false;
    }

    /**
     * @return the phpPath
     */
    public String getPhpPath() {
        return phpPath;
    }

    /**
     * @param phpPath the phpPath to set
     */
    public void setPhpPath(String phpPath) {
        this.phpPath = phpPath;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the documentRoot
     */
    public File getDocumentRoot() {
        return documentRoot;
    }

    /**
     * @param documentRoot the documentRoot to set
     */
    public void setDocumentRoot(File documentRoot) {
        this.documentRoot = documentRoot;
    }

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return the ended
     */
    public boolean isEnded() {
        return ended;
    }
    
    /**
     * Executes given PHP code.
     * @param phpCode
     * @return
     * @throws IOException 
     */
    public String executeUTF8(String phpCode) throws IOException {
        try (InputStream is = execute(phpCode)) {
            StringBuilder sb = new StringBuilder();
            int len;
            byte[] buf = new byte[8096 * 4];
            while ((len = is.read(buf)) >= 0) {
                sb.append(new String(buf, 0, len, "UTF-8"));
            }
            return sb.toString();
        } 
    }
    
    public InputStream execute(String phpCode) throws IOException {
        if (!isRunning()) {
            throw new RuntimeException("PHP development server is currently not running");
        }
        InputStream out;
        File tmp = File.createTempFile("execute", ".php", getDocumentRoot());
        tmp.deleteOnExit();
        try {
            try (PrintWriter pw = new PrintWriter(tmp)) {
                pw.print(phpCode);

            } 
            
            
            URL url = new URL("http://localhost:"+getPort()+"/"+tmp.getName());
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            return conn.getInputStream();
           
        } finally {
            tmp.delete();
        }
    }
    
}
