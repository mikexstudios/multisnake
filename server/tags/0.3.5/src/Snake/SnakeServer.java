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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

public class SnakeServer {

  private static final int PORT = 10123;
  private static final int MAX_CONNECTIONS = 0;

  private Board board;
  private List<Snake> snakes;

  public SnakeServer() {
    board = new Board();
    snakes = Collections.synchronizedList(new LinkedList<Snake>());
  }

  public void run() {
    int i = 0;

    SnakeUpdater sup = new SnakeUpdater();
    Timer snake_update_timer = new Timer();
    snake_update_timer.schedule(sup, 0, SnakeUpdater.UPDATE_PERIOD);

    FoodPlacer fp = new FoodPlacer();
    Thread food_placer = new Thread(fp);
    food_placer.start();

    try {
      ServerSocket listener = new ServerSocket(PORT);

      while ((i++ < MAX_CONNECTIONS) || (MAX_CONNECTIONS == 0)) {
        Socket sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.

        Snake snake = new Snake(board, sock);
        SnakeConnection conn = snake.getConnection();

        // Announce that new snake has joined.
        String id = conn.getClientId();
        ListIterator<Snake> itr = snakes.listIterator(0);
        while (itr.hasNext()) {
          Snake cur = itr.next();
          cur.getConnection().announcePlayerJoined(id);
        }

        // Spawn client listener thread.
        Thread t = new Thread(conn);
        conn.setThread(t);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        snakes.add(snake);
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  private class SnakeUpdater extends TimerTask {
    private static final int UPDATE_PERIOD = 220; // milliseconds

    public void run() {
      ListIterator<Snake> itr = snakes.listIterator(0);
      while (itr.hasNext()) {
        // Either step or reap the object if connection died.
        Snake cur = itr.next();
        if (cur.getConnection().disconnected()) {
          itr.remove();
          reportSnakeDisconnected(cur.getConnection().getClientId());
        } else {
          if (!cur.isDead()) {
            cur.step();
          }
        }
      }
    }

    public void reportSnakeDisconnected(String id) {
      ListIterator<Snake> itr = snakes.listIterator(0);
      while (itr.hasNext()) {
        Snake cur = itr.next();
        cur.getConnection().announcePlayerDisconnected(id);
      }
    }
  }

  private class FoodPlacer implements Runnable {
    private static final int FOOD_PLACEMENT_PERIOD = 1000; // milliseconds
    private static final int DESIRED_FOOD_AMOUNT = Board.COLS * Board.ROWS / 60;

    public void run() {
      while (true) {
        int error = DESIRED_FOOD_AMOUNT - board.getFoodAmount();
        int amount_to_add = error / 4;

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
