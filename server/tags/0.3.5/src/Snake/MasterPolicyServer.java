/**
 * Eugene Marinelli
 */

package Snake;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MasterPolicyServer implements Runnable {
  private static int PORT = 843;
  private static int MAX_CONNECTIONS = 0;

  public void run() {
    int i = 0;

    try {
      ServerSocket listener = new ServerSocket(PORT);
      Socket sock;

      while ((i++ < MAX_CONNECTIONS) || (MAX_CONNECTIONS == 0)) {
        sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.

        MasterPolicyConnection conn = new MasterPolicyConnection(sock);
        Thread t = new Thread(conn);
        t.start();
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }
}
