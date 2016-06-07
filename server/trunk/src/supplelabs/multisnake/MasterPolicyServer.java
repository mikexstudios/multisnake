/**
 * Eugene Marinelli
 */

package supplelabs.multisnake;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MasterPolicyServer implements Runnable {
  private static int PORT = 843;

  public void run() {
    try {
      ServerSocket listener = new ServerSocket(PORT);

      while (true) {
        Socket sock = listener.accept();
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
