package ftpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class used to manage the different command received on the command port
 */
public class FtpRequest extends Thread {
    
    /**
     *  Contains the OS on wich the server is running
     */
    private static String OS;
    
    /**
     * Port used for the active mode  
     */
    private int portActive;

    /**
     * Hostname used for the active mode
     */
    private String clientActive;

    /**
     * Server socket used for the passive mode
     */
    private ServerSocket ss;

    /**
     * Socket used for the command port
     */
    private Socket s;

    /**
     * Socket used for the data port
     */
    private Socket data;

    /**
     * User name for the current user
     */
    private String username;

    /**
     * true if the user is logged in 
     */
    private boolean logged;

    /**
     * Contains the current file for navigation purpose
     */
    private File path;
    private boolean isPassive;

    /**
     * Instantiates a new ftp request.
     *
     * @param s    the socket
     * @param file the current file
     */
    public FtpRequest(Socket s, File file) {
        OS = System.getProperty("os.name").toLowerCase();
        path = file;
        System.out.println("start communication with a " + OS);
        this.s = s;
    }

    /**
     * Convert an InputStream to a String
     *
     * @param is the InputStream to convert
     * @return the String converted
     */
    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Send the Welcome message and start listening for commands
     */
    @Override
    public void run() {
        sendMessage("220 Welcome to the Ghigny and Leemans FTP Server");
        process();
    }

    /**
     * Process the differents commands received on the port command
     * and redirect them to methods 
     */
    private void process() {
        while (true) {
            System.out.println("process");
            String commande = receiveMessage();
            if (commande == null) {
                processQUIT();
            } else {
                String[] args = commande.split(" ", 2);
                switch (args[0]) {
                    case "USER":
                        processUSER(args[1]);
                        break;
                    case "PASS":
                        processPASS(args[1]);
                        break;
                    case "SYST":
                        processSYST();
                        break;
                    case "PWD":
                        processPWD();
                        break;
                    case "LIST":
                        processLIST();
                        break;
                    case "TYPE":
                        processType();
                        break;
                    case "CWD":
                        processCWD(args[1]);
                        break;
                    case "CDUP":
                        processCWD("..");
                    case "EPSV":
                        processEPSV();
                        break;
                    case "EPRT":
                        processEPRT(args[1]);
                        break;
                    case "PASV":
                        processPASV();
                        break;
                    case "PORT":
                        processPORT(args[1]);
                        break;
                    case "RETR":
                        processRETR(args[1]);
                        break;
                    case "STOR":
                        processSTOR(args[1]);
                        break;
                    case "QUIT":
                        processQUIT();
                        break;
                    default:
                        sendMessage("502 Command not implemented");
                }
            }
        }
    }

    /**
     * Process the login
     *
     * @param username the username
     */
    private void processUSER(String username) {
        System.out.println("processUSER " + username);
        if (Server.users.containsKey(username)) {
            this.username = username;
            sendMessage("331 User name okay, need password.");
        } else {
            sendMessage("332 Need account for login.");
        }
    }

    /**
     * Process the password verification
     *
     * @param password the password
     */
    private void processPASS(String password) {
        System.out.println("processPASS " + password);
        if (this.username == null) {
            sendMessage("332 Need account for login.");
        } else if (logged) {
            sendMessage("Error already logged in");
        } else if (Server.users.get(this.username).equals(password)) {
            System.out.println("Logged in");
            logged = true;
            sendMessage("230 User logged in, proceed.");
        } else {
            sendMessage("Bad passwod");
        }
    }

    /**
     * Process the OS return code
     */
    private void processSYST() {
        if (OS.contains("win")) {
            sendMessage("215 Windows_NT");
        } else if (OS.contains("mac")) {
            sendMessage("215 MACOS");
        } else if (OS.contains("nix") || OS.contains("nux")
                || OS.contains("aix")) {
            sendMessage("215 UNIX Type: L8");
        } else {
            sendMessage("215 Unknown");
        }
    }

    /**
     * Send the current path
     */
    private void processPWD() {
        sendMessage("257 " + path.getPath() + " is current directory");
    }

    /**
     * Process the Type command 
     */
    private void processType() {
        sendMessage("200 action successfull");
    }

    /**
     * Change the working directory
     *
     * @param args the path to move to
     */
    private void processCWD(String args) {
        String oldPath = path.getAbsolutePath();
        if (args.equals("..")) {
            path = path.getParentFile();
        } else {
            path = new File(args);
        }
        if (path.exists() && path.isDirectory()) {
            sendMessage("250 CWD command successfull");
        } else {
            path = new File(oldPath + "\\" + args);
            System.out.println(path.getAbsolutePath());
            if (path.exists() && path.isDirectory()) {
                sendMessage("250 CWD command successfull");
            } else {
                path = new File(oldPath);
                sendMessage("550" + path + " No such file or directory.");
            }
        }
    }

    /**
     * Process the RETR command used to download a file from the server
     * @param arg the path of the file to retreive
     */
    private void processRETR(String arg) {
        try {
            FileInputStream fIS = new FileInputStream(path.getPath() + "\\" + arg);
            if (isPassive) {
                try {
                    //ouverture de la connection
                    sendMessage("150 File status okay; about to open data connection.");
                    this.ss = new ServerSocket(1780);
                    this.data = ss.accept();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                this.data = new Socket(clientActive, portActive);
            }
            //envoie du ficher
            sendFile(fIS, data);

            //fermeture de la connection
            sendMessage("226 Closing data connection. Requested file action successful (for example, file transfer or file abort).");
            data.close();
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process the STOR command used to upload a file to the server
     * @param arg the path of the file to store
     */
    private void processSTOR(String arg) {

        try {
            FileOutputStream fOS = new FileOutputStream(path.getPath() + "\\" + arg);
            if (isPassive) {
                try {
                    //ouverture de la connection
                    sendMessage("150 File status okay; about to open data connection.");
                    this.ss = new ServerSocket(1780);
                    this.data = ss.accept();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                this.data = new Socket(clientActive, portActive);
            }
            //envoie du ficher
            receiveFile(fOS, data);

            //fermeture de la connection
            sendMessage("226 Closing data connection. Requested file action successful (for example, file transfer or file abort).");
            data.close();
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the active mode with IPV4
     * @param arg the hostname and port on wich to connect for data sending
     */
    private void processPORT(String arg) {
        String[] split = arg.split(",");
        clientActive = split[0] + "." + split[1] + "." + split[2] + "." + split[3];
        portActive = Integer.parseInt(split[4]) * 256 + Integer.parseInt(split[5]);
        this.isPassive = false;
        sendMessage("200 PORT command successfull");
    }

    /**
     * Start the active mode with IPV6
     * @param args the hostname and port on wich to connect for data sending
     */
    private void processEPRT(String args) {
        String[] connectionString = args.split("\\|");
        clientActive = connectionString[2];
        portActive = Integer.parseInt(connectionString[3]);
        this.isPassive = false;
        sendMessage("229 Entering Exten" +
                "ded Actif Mode (||" + clientActive + "|" + portActive + "|)");

    }

    /**
     * Start the passive mode with IPV4
     */
    private void processPASV() {
        this.isPassive = true;
        sendMessage("227 Entering Passive Mode (" + s.getLocalAddress().toString().replaceAll("\\.", ",").substring(1) + "," + 1780 / 256 + "," + 1780 % 256 +")");
    }

    /**
     * Start the passive mode with IPV6
     */
    private void processEPSV() {
        this.isPassive = true;
        sendMessage("229 Entering Exten" +
                "ded Passive Mode (|||1780|)");
    }

    /**
     * Send the current directory's list of files
     */
    private void processLIST() {
        try {
            //Data socket opening
            sendMessage("150 File status okay; about to open data connection.");
            if (isPassive) {
                this.ss = new ServerSocket(1780);
                this.data = ss.accept();
            } else {
                this.data = new Socket(clientActive, portActive);
            }

            //list sending
            String daPath = convertStreamToString(Runtime.getRuntime().exec(new String[]{"cmd", "/c", "dir", path.getPath()}).getInputStream());
            sendMessage(daPath, data);

            //Data socket closing
            sendMessage("226 Closing data connection. Requested file action successful (for example, file transfer or file abort).");
            data.close();
            if (isPassive) ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Quit message.
     */
    private void processQUIT() {
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to send a message trought the command port
     *
     * @param str the message to send
     */
    private void sendMessage(String str) {
        sendMessage(str, s);
    }

    /**
     * Used to send a message trought a specific socket
     *
     * @param str the message to send
     * @param s   the socket to use
     */
    private void sendMessage(String str, Socket s) {
        try {
            OutputStream os = s.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            pw.println(str);
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                s.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Used to send a file trought a specific socket
     *
     * @param fIS the FileInputStream to send
     * @param s   the socket to use
     */
    private void sendFile(FileInputStream fIS, Socket s) {
        try {
            OutputStream os = s.getOutputStream();
            byte[] buffer = new byte[1024];
            int nbBytes = 0;
            while ((nbBytes = fIS.read(buffer)) != -1) {
                os.write(buffer, 0, nbBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                s.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Used to wait for a message
     *
     * @return the message received
     */
    private String receiveMessage() {
        try {
            InputStream is = s.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String str = br.readLine();
            System.out.println(str);
            return str;
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * Used to receive a file trought a specific socket
     *
     * @param fOS the FileOutputStream in wich to write
     * @param data the socket to use
     */
    private void receiveFile(FileOutputStream fOS, Socket data) {
        try {
            InputStream is = data.getInputStream();
            byte[] buffer = new byte[1024];
            int nbBytes = 0;
            while ((nbBytes = is.read(buffer)) != -1) {
                fOS.write(buffer, 0, nbBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
