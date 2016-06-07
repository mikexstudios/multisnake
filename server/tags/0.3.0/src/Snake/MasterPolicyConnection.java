/**
 * Master Policy Connection - serves the master policy file.
 *
 * Eugene Marinelli
 * 12/15/07
 */

package Snake;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

  /* Copied from master policy server example. */
  private String read(BufferedReader in) {
    StringBuffer buffer = new StringBuffer();
    int codePoint;
    boolean zeroByteRead = false;

    try {
      do {
        codePoint = in.read();
        if (codePoint == 0) {
          zeroByteRead=true;
        } else {
          buffer.appendCodePoint( codePoint );
        }
      } while (!zeroByteRead && buffer.length() < 100);
    } catch (IOException e) {}

    return buffer.toString();
  }

  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      out = new PrintWriter(sock.getOutputStream());

      String line = read(in);
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
