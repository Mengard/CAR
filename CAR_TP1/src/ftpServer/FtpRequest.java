package ftpServer;

import com.sun.xml.internal.bind.v2.TODO;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class FtpRequest extends Thread {
    private ServerSocket ss;
    private Socket s;
    private Socket data;
    private String username;
    private boolean logged;
    private static String OS;
    private File path;
    private boolean isPassive;
    int portActive;

    /**
     * Instantiates a new ftp request.
     *
     * @param s    the socket
     * @param file
     */
    public FtpRequest(Socket s, File file) {
        OS = System.getProperty("os.name").toLowerCase();
        path = file;
        System.out.println("start communication with a " + OS);
        this.s = s;
    }

    @Override
    public void run() {
        sendMessage("220 Welcome to the Ghigny and Leemans FTP Server");
        process();
    }

    /**
     * manage the different kind of messages. ajout du code
     */
    void process() {
        while (true) {
            System.out.println("process");
            String commande = receiveMessage();
            String[] args = commande.split(" ");
            if (args.length > 2) {
                sendMessage("500 Syntax error, command unrecognized. This may include errors such as command line too long.");
            } else {
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
                    case "EPSV":
                        processEPSV();
                        break;
                    case "EPRT":
                        processEPRT(args[1]);
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
     * Login message.
     *
     * @param username the username
     */
    void processUSER(String username) {
        System.out.println("processUSER " + username);
        // if (this.username != null) {
        // sendMessage("Error username already defined");
        // } else if (logged) {
        // sendMessage("Error already logged in");
        // } else
        if (Server.users.containsKey(username)) {
            this.username = username;
            sendMessage("331 User name okay, need password.");
        } else {
            sendMessage("332 Need account for login.");
        }
    }

    /**
     * Pass message.
     *
     * @param password the password
     */
    void processPASS(String password) {
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


    void processSYST() {
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

    void processPWD() {
        sendMessage("257 " + path.getPath() + " is current directory");
    }

    void processType() {
        sendMessage("200 action successfull");
    }

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

    void processRETR() {
        //TODO
    }

    void processSTOR() {
        //TODO
    }

    void processEPRT(String args) {
        String[] connectionString = args.split("\\|");
        portActive = Integer.parseInt(connectionString[3]);
        this.isPassive = false;
        sendMessage("229 Entering Exten" +
                "ded Passive Mode (|||" + portActive + "|)");

    }


    private void processEPSV() {
        this.isPassive = true;
        sendMessage("229 Entering Exten" +
                "ded Passive Mode (|||1780|)");

    }

    void processLIST() {
        if (isPassive) {
            try {
                sendMessage("150 File status okay; about to open data connection.");
                this.ss = new ServerSocket(1780);
                this.data = ss.accept();
                String daPath = convertStreamToString(Runtime.getRuntime().exec(new String[]{"cmd", "/c", "dir", path.getPath()}).getInputStream());
                sendMessage(daPath, data);
                sendMessage("226 Closing data connection. Requested file action successful (for example, file transfer or file abort).");
                data.close();
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String daPath = convertStreamToString(Runtime.getRuntime().exec(new String[]{"cmd", "/c", "dir", path.getPath()}).getInputStream());
                this.data = new Socket("127.0.0.1", portActive);
                sendMessage("150 File status okay; about to open data connection.");
                //ToDo ajouter gestion adresse client
                sendMessage(daPath, data);
                sendMessage("226 Closing data connection. Requested file action successful (for example, file transfer or file abort).");
                data.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Quit message.
     */
    void processQUIT() {
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to send a message
     *
     * @param str the message
     */
    void sendMessage(String str) {
        try {
            OutputStream os = s.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            pw.println(str);
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Used to send a message
     *
     * @param str the message
     * @param s   the socket
     */
    void sendMessage(String str, Socket s) {
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
     * Used to wait for a message
     *
     * @return the message
     */
    String receiveMessage() {
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

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
