/**
 * Main entry point for snake server.
 *
 * Eugene Marinelli
 * 12/15/07
 */

import Snake.MasterPolicyServer;
import Snake.SnakeServer;
import Snake.InfoServer;

public class Main {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java -jar dist/SnakeServer.jar <hostname>");
      System.exit(1);
    }

    MasterPolicyServer ps = new MasterPolicyServer();
    Thread policy_server = new Thread(ps);
    policy_server.setPriority(Thread.MIN_PRIORITY);
    policy_server.start();

    SnakeServer ss = new SnakeServer();

    InfoServer is = new InfoServer(ss, args[0]);
    Thread info_server = new Thread(is);
    info_server.setPriority(Thread.MIN_PRIORITY);
    info_server.start();

    ss.run(); // Snake server runs in the current thread.
  }
}
