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
  private static final String[] SERVER_POST_SCRIPT_URLS = {"http://mtest.appspot.com/manager/postserver"};
  private static final String SERVER_KEY = "Q7t1582VHWoeVE2ITMgrS";
  private static final int SERVER_UPDATE_PERIOD = 15000;

  private SnakeServer snake_server;
  private String hostname;

  public InfoServer(SnakeServer snake_server, String hostname) {
    this.snake_server = snake_server;
    this.hostname = hostname;
  }

  public void run() {
    while (true) {
      for (String base : SERVER_POST_SCRIPT_URLS) {
        String url = base + "?key=" + SERVER_KEY + "&host=" + hostname + "&num_clients="
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
      }

      try {
        Thread.sleep(SERVER_UPDATE_PERIOD);
      } catch (InterruptedException ie) {}
    }
  }
}
