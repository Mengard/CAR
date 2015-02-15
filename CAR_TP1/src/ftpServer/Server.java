package ftpServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server {
    static ServerSocket serverCommand;
    private static File file;

    // username -> password
    static HashMap<String, String> users = new HashMap<>();

    public static void main(String[] args) {

        users.put("anonymous", "anonymous");
        users.put("Mengard", "Dragnem");
        users.put("Wyll", "I4M");

        if (args.length != 1) {
            System.out.println("BAD USAGE");
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
