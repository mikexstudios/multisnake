/**
* Eugene Marinelli
*/

package Snake;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;

public class InfoServer implements Runnable {
  private static final String SERVER_POST_SCRIPT_URL = "http://www.multisnake.com/server_manager/post_server_stats.php";
  private static final String SERVER_KEY = "Q7t1582VHWoeVE2ITMgrS";
  private static final int SERVER_UPDATE_PERIOD = 15000;

  private SnakeServer snake_server;
  private String hostname;

  public InfoServer(SnakeServer snake_server, String hostname) {
    this.snake_server = snake_server;
    this.hostname = hostname;

    /*  This doesn't work very well
    try {
      InetAddress addr = InetAddress.getLocalHost();
      hostname = addr.getHostAddress();
      System.out.println("Local host address: " + hostname);
    } catch (UnknownHostException e) {}
    */
  }

  public void run() {
    while (true) {
      String url = SERVER_POST_SCRIPT_URL + "?key=" + SERVER_KEY + "&host=" + hostname + "&numclients="
        + snake_server.getNumClients();
      URL server_url = null;
      try {
        server_url = new URL(url);
      } catch (MalformedURLException e) {
        System.out.println("Error - malformed url: " + url);
        return;
      }

      try {
        server_url.openConnection().getContent();
      } catch (IOException e) {}

      try {
        Thread.sleep(SERVER_UPDATE_PERIOD);
      } catch (InterruptedException ie) {}
    }
  }
}
