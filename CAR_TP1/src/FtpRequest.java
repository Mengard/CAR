import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

public class FtpRequest extends Thread {
	private Socket s;
	private String username;
	private boolean logged;
	private static String OS;
	private File path;

	/**
	 * Instantiates a new ftp request.
	 *
	 * @param s
	 *            the socket
	 */
	public FtpRequest(Socket s) {
		OS = System.getProperty("os.name").toLowerCase();
		System.out.println("start communication with a " + OS);
		this.s = s;
	}

	@Override
	public void run() {
		sendMessage("220 Service ready for new user.");
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
				case "TYPE":
					processType();
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
	 * @param username
	 *            the username
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
	 * @param password
	 *            the password
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
			path = new File(Server.default_pwd);
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
		System.out.println("THIS IS PATH :" + path.getPath());
		sendMessage("257 " + path.getPath());
	}

	void processType() {
		sendMessage("200 action successfull");
	}

	void processRETR() {

	}

	void processSTOR() {

	}

	void processLIST() {

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
	 * @param str
	 *            the message
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
}
