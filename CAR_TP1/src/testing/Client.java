package testing;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by William on 10/02/2015.
 */

public class Client {

    private Socket sCommand;
    private Socket sData;

    public Client(int commandPort, int dataPort) {
        try {
            sCommand = new Socket(InetAddress.getLocalHost(), 1779);
            sData =  new Socket(InetAddress.getLocalHost(), 1780);

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
            OutputStream os = sCommand.getOutputStream();
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
            InputStream is = sCommand.getInputStream();
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
