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
import java.util.LinkedList;
import java.util.ListIterator;

public class SnakeServer {
  
  private static final int PORT = 10123;
  private static final int MAX_CONNECTIONS = 0;

  private Board board;
  private LinkedList<Snake> snakes;

  public SnakeServer() {
    board = new Board();
    snakes = new LinkedList<Snake>();
  }

  public void run() {
    int i = 0;

    SnakeUpdater sup = new SnakeUpdater();
    Thread snake_updater = new Thread(sup);
    snake_updater.start();

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
        conn.setThread(t);
        t.start();

        snakes.addFirst(snake);
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  private class SnakeUpdater implements Runnable {
    private static final int UPDATE_PERIOD = 150; // milliseconds
    
    public void run() {
      while (true) {
        ListIterator<Snake> itr = snakes.listIterator(0);
        while (itr.hasNext()) {
          Snake cur = itr.next();
          if (cur.isDead()) {
            itr.remove();
          } else {
            cur.step();
          }
        }
        
        try {
        	Thread.sleep(UPDATE_PERIOD);
        } catch (InterruptedException e) { 
          e.printStackTrace();
        }
      }
    }
  }

  private class FoodPlacer implements Runnable {
    private static final int FOOD_PLACEMENT_PERIOD = 750; // milliseconds
    private static final int DESIRED_FOOD_AMOUNT = Board.COLS * Board.ROWS / 30;

    public void run() {
      int last_amount_added = 0;

      while (true) {
        int error = DESIRED_FOOD_AMOUNT - board.getFoodAmount();
        int amount_to_add = error / 4;
        last_amount_added = amount_to_add;
        
        for (int i = 0; i < amount_to_add; i++) {
          board.placeRandomFood();
        }
        try {
          Thread.sleep(FOOD_PLACEMENT_PERIOD);
        } catch (InterruptedException ie) {
        }
      }
    }
  }
}
