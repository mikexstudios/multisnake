/**
 * Master Policy Connection - serves the master policy file.
 *
 * Eugene Marinelli
 * 12/15/07
 */

package Snake;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MasterPolicyConnection implements Runnable {
  private static final String EOF = Character.toString((char)0);

  private Socket sock;
  private PrintWriter out;

  public MasterPolicyConnection(Socket s) {
    sock = s;
  }

  public synchronized void send(String s) {
    System.out.println("Sending: " + s);

    out.println(s + EOF);
    out.flush();
  }

  public void run() {
    try {
      DataInputStream in = new DataInputStream(sock.getInputStream());
      out = new PrintWriter(sock.getOutputStream());

      String line = in.readLine();
      System.out.println("Received: " + line);
      send("<cross-domain-policy><site-control permitted-cross-domain-policies=\"all\"/><allow-access-from domain"
           + "=\"*\" to-ports=\"10123\"/></cross-domain-policy>");

      sock.close();
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }
}
