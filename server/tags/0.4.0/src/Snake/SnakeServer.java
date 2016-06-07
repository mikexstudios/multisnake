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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

public class SnakeServer implements Runnable {
  private static final int PORT = 10123;
  private static final int MAX_CLIENTS = 1000; // Probably should be lower.

  private Board board;
  private List<Snake> snakes;

  public SnakeServer() {
    board = new Board();
    snakes = Collections.synchronizedList(new LinkedList<Snake>());
  }

  public int getNumClients() {
    return snakes.size();
  }

  public void run() {
    // Handling connections low priority compared to making rest of the game responsive.
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    SnakeUpdater sup = new SnakeUpdater();
    Timer snake_update_timer = new Timer();
    snake_update_timer.schedule(sup, 0, SnakeUpdater.UPDATE_PERIOD);

    FoodPlacer fp = new FoodPlacer(board);
    Thread food_placer = new Thread(fp);
    food_placer.setPriority(Thread.MIN_PRIORITY);
    food_placer.start();

    try {
      ServerSocket listener = new ServerSocket(PORT);

      while (true) {
        Socket sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.

        if (snakes.size() != MAX_CLIENTS) {
          Snake snake = new Snake(board, sock);
          SnakeConnection conn = snake.getConnection();
          reportSnakeConnected(conn.getClientId()); // Announce that new snake has joined.

          // Spawn client listener thread.
          Thread t = new Thread(conn);
          conn.setThread(t);
          t.setPriority(Thread.NORM_PRIORITY);
          t.start();

          snakes.add(snake);
        } else {
          sock.close();
        }
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  private void reportSnakeConnected(String id) {
    synchronized (snakes) {
      ListIterator<Snake> itr = snakes.listIterator(0);
      while (itr.hasNext()) {
        Snake cur = itr.next();
        cur.getConnection().announcePlayerJoined(id);
      }
    }
  }

  private void reportSnakeDisconnected(String id) {
    synchronized (snakes) {
      ListIterator<Snake> itr = snakes.listIterator(0);
      while (itr.hasNext()) {
        Snake cur = itr.next();
        cur.getConnection().announcePlayerDisconnected(id);
      }
    }
  }

  private class SnakeUpdater extends TimerTask {
    private static final int UPDATE_PERIOD = 220; // milliseconds

    private List<Snake> dead_snakes; // This is a class variable since we only want to allocate it once.
    private List<Snake> disconnected_snakes;

    public SnakeUpdater() {
      dead_snakes = new ArrayList();
      disconnected_snakes = new ArrayList();
    }

    public void run() {
      synchronized (snakes) {
        // Do this first to maximize responsiveness.
        ListIterator<Snake> itr = snakes.listIterator(0);
        while (itr.hasNext()) {
          // Either step or reap the object if connection died.
          Snake cur = itr.next();
          if (cur.getConnection().disconnected()) {
            disconnected_snakes.add(cur);
            itr.remove();
          } else {
            if (!cur.isDead()) { // If snake is dead, wait until game has been reset before stepping again.
              boolean snake_still_alive = cur.step();
              if (!snake_still_alive) {
                dead_snakes.add(cur);
              }
            }
          }
        }

        // Wait until all snakes have moved to clear the bodies (handles head to head collisions where both die).
        ListIterator<Snake> dead_itr = dead_snakes.listIterator(0);
        while (dead_itr.hasNext()) {
          Snake cur = dead_itr.next();
          cur.clearBody();
        }
        dead_snakes.clear();

        ListIterator<Snake> disconnected_itr = disconnected_snakes.listIterator(0);
        while (disconnected_itr.hasNext()) {
          Snake cur = disconnected_itr.next();
          cur.clearBody();
          reportSnakeDisconnected(cur.getConnection().getClientId());
        }
        disconnected_snakes.clear();
      }
    }
  }

  private class FoodPlacer implements Runnable {
    private static final int FOOD_PLACEMENT_PERIOD = 1000; // milliseconds
    private int desired_food_amt;

    public FoodPlacer(Board board) {
      desired_food_amt = board.getWidth() * board.getHeight() / 40;
    }

    public void run() {
      while (true) {
        int error = desired_food_amt - board.getFoodAmount();
        int amount_to_add = error / 4;

        for (int i = 0; i < amount_to_add; i++) {
          board.placeRandomFood();
        }

        try {
          Thread.sleep(FOOD_PLACEMENT_PERIOD);
        } catch (InterruptedException ie) {}
      }
    }
  }
}
