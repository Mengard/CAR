import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server {
	static String default_pwd = "C:\\";
	static ServerSocket serverCommand;
	static ServerSocket serverData;

	// username -> password
	static HashMap<String, String> users = new HashMap<String, String>();

	public static void main(String[] zero) {

		users.put("anonymous", "anonymous");
		users.put("Mengard", "Dragnem");
		users.put("Wyll", "I4M");

		try {
			serverCommand = new ServerSocket(1779);
			serverData = new ServerSocket(1780);
			new AcceptingThread().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class AcceptingThread extends Thread {

		@Override
		public void run() {
			while (true) {
				try {
					System.out.println("waiting");
					// new FtpRequest(serverData.accept()).start();
					new FtpRequest(serverCommand.accept()).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
