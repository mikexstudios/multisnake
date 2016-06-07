/**
 * Snake Server
 *
 * Eugene Marinelli
 * 12/15/07
 */

package Snake;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SnakeServer {
  
  private static int PORT = 10123;
  private static int MAX_CONNECTIONS = 0;

  private static int FOOD_PLACEMENT_PERIOD = 1000; // milliseconds

  private Board board;

  public SnakeServer() {
    board = new Board();
  }

  public void run() {
    int i = 0;

    FoodPlacer fp = new FoodPlacer();
    Thread food_placer = new Thread(fp);
    food_placer.start();

    try {
      ServerSocket listener = new ServerSocket(PORT);
      Socket sock;

      while ((i++ < MAX_CONNECTIONS) || (MAX_CONNECTIONS == 0)) {
        sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.

        SnakeConnection conn = new SnakeConnection(sock);
        Snake snake = new Snake(board, conn);
        conn.setSnake(snake);

        // Spawn client listener thread.
        Thread t = new Thread(conn);
        t.start();
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  private class FoodPlacer implements Runnable {
    public void run() {
      while (true) {
        board.placeRandomFood();
        try {
          Thread.sleep(FOOD_PLACEMENT_PERIOD);
        } catch (InterruptedException ie) {
        }
      }
    }
  }
}
