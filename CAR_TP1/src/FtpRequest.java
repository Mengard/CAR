import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class FtpRequest extends Thread {
	private Socket s;
	private String username;
	private boolean logged;

	public FtpRequest(Socket s) {
		System.out.println("start communication");
		this.s = s;
	}

	@Override
	public void run() {
		sendMessage("220 Service ready for new user.");
		process();
	}

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
					sendMessage("215 Windows_NT");
					break;
				default:
					sendMessage("501 Syntax error in parameters or arguments.");
				}
			}
		}
	}

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

	void processPASS(String password) {
		System.out.println("processPASS " + password);
		if (this.username == null) {
			sendMessage("332 Need account for login.");
		} else if (logged) {
			sendMessage("Error already logged in");
		} else if (Server.users.get(this.username).equals(password)) {
			System.out.println("Logged in");
			sendMessage("230 User logged in, proceed.");
		} else {
			sendMessage("Bad passwod");
		}
	}

	void processRETR() {

	}

	void processSTOR() {

	}

	void processLIST() {

	}

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
