/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package php4jruntime;

import ca.weblite.php4j.PHPDevServer;
import ca.weblite.php4j.PHPLoader;
import java.io.File;

/**
 *
 * @author shannah
 */
public class PHP4JRuntime {

    public static void install() throws Exception {
        try (PHPDevServer server = new PHPDevServer()) {
            System.out.println("Starting server");
            server.start();
            
            // This should trigger an install
            System.out.println("Install complete");
            
        }
    }
    
    public static void uninstall() throws Exception {
        PHPLoader loader = new PHPLoader();
        loader.uninstall();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "-install".equals(args[0])) {
            install();
            return;
        } else if (args.length == 1 && "-uninstall".equals(args[0])) {
            uninstall();
            return;
        }
        // TODO code application logic here
        try (PHPDevServer server = new PHPDevServer()) {
            if (args.length > 0) {
                server.setDocumentRoot(new File(args[0]));
            }
            System.out.println("Starting server");
            server.start();
            Object monitor = new Object();
            synchronized(monitor) {
                monitor.wait();
            }
            
        }
    }
    
}
