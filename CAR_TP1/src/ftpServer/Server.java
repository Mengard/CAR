package ftpServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;


/**
 * Main class of the FTP server
 */
public class Server {
    /**
     * ServerSocket used for the Command Port
     */
    static ServerSocket serverCommand;
    /**
     * Map containing the different username and password used for testing purpose
     */
    static HashMap<String, String> users = new HashMap<>();
    /**
     * Current File used to navigate within the server
     */
    private static File file;

    /**
     * Starts the server
     *
     * @param args the arguments of the program
     */
    public static void main(String[] args) {

        users.put("anonymous", "anonymous");
        users.put("Mengard", "Dragnem");
        users.put("Wyll", "I4M");

        if (args.length != 1) {
            System.out.println("Correct usage is : java Server [Current path]");
            System.exit(0);
        }

        file = new File(args[0]);
        if (file.exists() && file.isDirectory()) {
            try {
                serverCommand = new ServerSocket(1779);
                new AcceptingThread().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("The argument is not the path of a directory");
        }
    }

    /**
     * Thread starting a FtpRequest to start listening on the server*
     */
    private static class AcceptingThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println("waiting");
                    new FtpRequest(serverCommand.accept(), file).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
