/**
 * Main entry point for snake server.
 *
 * Eugene Marinelli
 * 12/15/07
 */

import Snake.MasterPolicyServer;
import Snake.SnakeServer;

public class Main {
  public static void main(String[] args) {
    MasterPolicyServer ps = new MasterPolicyServer();
    Thread policy_server = new Thread(ps);
    policy_server.start();

    SnakeServer ss = new SnakeServer();
    ss.run();
  }
}
