package ftpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * *
 */
public class FtpRequest extends Thread {
    private static String OS;
    int portActive;
    String clientActive;
    private ServerSocket ss;
    private Socket s;
    private Socket data;
    private String username;
    private boolean logged;
    private File path;
    private boolean isPassive;

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

    /**
     * *
     *
     * @param is
     * @return
     */
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
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
     * Login message.
     *
     * @param username the username
     */
    void processUSER(String username) {
        System.out.println("processUSER " + username);
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

    void processCWD(String args) {
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

    void processRETR(String arg) {

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

    void processSTOR(String arg) {

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

    void processPORT(String arg) {
        String[] split = arg.split(",");
        clientActive = split[0] + "." + split[1] + "." + split[2] + "." + split[3];
        portActive = Integer.parseInt(split[4]) * 256 + Integer.parseInt(split[5]);
        sendMessage("200 PORT command successfull");
    }

    void processEPRT(String args) {
        String[] connectionString = args.split("\\|");
        clientActive = connectionString[2];
        portActive = Integer.parseInt(connectionString[3]);
        this.isPassive = false;
        sendMessage("229 Entering Exten" +
                "ded Actif Mode (||" + clientActive + "|" + portActive + "|)");

    }
    
    void processPASV() {
        this.isPassive = true;
        sendMessage("227 Entering Passive Mode (" + s.getLocalAddress().toString().replaceAll("\\.", ",").substring(1) + "," + 1780 / 256 + "," + 1780 % 256 +")");
    }

    void processEPSV() {
        this.isPassive = true;
        sendMessage("229 Entering Exten" +
                "ded Passive Mode (|||1780|)");
    }

    void processLIST() {

        try {
            //ouverture socket data
            sendMessage("150 File status okay; about to open data connection.");
            if (isPassive) {
                this.ss = new ServerSocket(1780);
                this.data = ss.accept();
            } else {
                this.data = new Socket(clientActive, portActive);
            }

            //envoie liste
            String daPath = convertStreamToString(Runtime.getRuntime().exec(new String[]{"cmd", "/c", "dir", path.getPath()}).getInputStream());
            sendMessage(daPath, data);

            //fermeture socket data
            sendMessage("226 Closing data connection. Requested file action successful (for example, file transfer or file abort).");
            data.close();
            if (isPassive) ss.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        sendMessage(str, s);
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
     * *
     *
     * @param fIS
     * @param s
     */
    void sendFile(FileInputStream fIS, Socket s) {
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

    /**
     * *
     *
     * @param fOS
     * @param data
     */
    void receiveFile(FileOutputStream fOS, Socket data) {
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
