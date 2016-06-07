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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class SnakeServer implements Runnable {
  private static final int PORT = 10123;
  private static final int MAX_CLIENTS = 2000;
  private int num_ai_players;

  private Board board;
  private Collection<Snake> snakes;
  private Collection<Snake> pregame_snakes;

  public SnakeServer(int num_ai_players) {
    board = new Board();
    init_common(num_ai_players);
  }

  public SnakeServer(int num_ai_players, String map_filename) {
    board = new Board(map_filename);
    init_common(num_ai_players);
  }

  public SnakeServer(int max_rows, int max_cols, int num_ai_players) {
    board = new Board(max_rows, max_cols);
    init_common(num_ai_players);
  }

  public SnakeServer(int max_rows, int max_cols, int num_ai_players, String map_filename) {
    board = new Board(map_filename, max_rows, max_cols);
    init_common(num_ai_players);
  }

  private void init_common(int num_ai_players) {
    snakes = Collections.synchronizedList(new LinkedList<Snake>());
    pregame_snakes = Collections.synchronizedList(new LinkedList<Snake>());
    this.num_ai_players = num_ai_players;
  }

  public int getNumClients() {
    return snakes.size();
  }

  public void run() {
    SnakeUpdater sup = new SnakeUpdater();
    Timer snake_update_timer = new Timer();
    snake_update_timer.schedule(sup, 0, SnakeUpdater.UPDATE_PERIOD);

    FoodPlacer fp = new FoodPlacer(board);
    Thread food_placer = new Thread(fp);
    food_placer.setPriority(Thread.MIN_PRIORITY);
    food_placer.start();

    for (int i = 0; i < num_ai_players; i++) {
      snakes.add(new ComputerSnake(board));
    }

    // Handling connections low priority compared to making rest of the game responsive.
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    try {
      ServerSocket listener = new ServerSocket(PORT);

      while (true) {
        Socket sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.

        if (snakes.size() != MAX_CLIENTS) {
          HumanSnake snake = new HumanSnake(board, sock, snakes.size() + 1);
          SnakeConnection conn = snake.getConnection();

          // Spawn client listener thread.
          Thread t = new Thread(conn);
          conn.setThread(t);
          t.setPriority(Thread.NORM_PRIORITY);
          t.start();

          pregame_snakes.add(snake);
        } else {
          sock.close();
        }
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  private void addAndReportSnake(Snake snake) {
    synchronized (snakes) {

      /*
      String common_msg = SnakeConnection.joinOrLeaveReportSubstring(snake.getId(), snakes.size());


      Iterator<Snake> itr = snakes.iterator();
      while (itr.hasNext()) {
        Snake cur = itr.next();
        SnakeConnection conn = cur.getConnection();
        if (conn != null) {
          conn.announcePlayerJoined(common_msg);
        }
      }*/

      snakes.add(snake);
    }
  }

  private void reportSnakeDisconnected(String id) {
    synchronized (snakes) {
      String common_msg = SnakeConnection.joinOrLeaveReportSubstring(id, snakes.size());

      /*
      Iterator<Snake> itr = snakes.iterator();
      while (itr.hasNext()) {
        Snake cur = itr.next();
        SnakeConnection conn = cur.getConnection();
        if (conn != null) {
          conn.announcePlayerDisconnected(common_msg);
        }
      }
      */
    }
  }

  private void reportSnakeDied(String id) {
    synchronized (snakes) {
      Iterator<Snake> itr = snakes.iterator();
      while (itr.hasNext()) {
        Snake cur = itr.next();
        SnakeConnection conn = cur.getConnection();
        if (conn != null) {
          conn.announcePlayerDied(id);
        }
      }
    }
  }

  private class SnakeUpdater extends TimerTask {
    private static final int UPDATE_PERIOD = 150; // milliseconds
    private static final double IDEAL_AREA_PER_CLIENT = 100.0;

    private Collection<Snake> dead_snakes; // This is a class variable since we only want to allocate it once.
    private Collection<Snake> disconnected_snakes;

    private static final int SEND_NUM_CLIENTS_PERIOD = 200;
    private int send_num_clients_counter;

    public SnakeUpdater() {
      dead_snakes = new ArrayList();
      disconnected_snakes = new ArrayList();

      send_num_clients_counter = 0;
    }

    public void run() {
      synchronized (pregame_snakes) {
        Iterator <Snake> itr = pregame_snakes.iterator();
        while (itr.hasNext()) {
          Snake cur = itr.next();
          if (cur.readyToReport()) {
            itr.remove();
            addAndReportSnake(cur);
          }
        }
      }

      synchronized (snakes) {
        if (send_num_clients_counter % SEND_NUM_CLIENTS_PERIOD == 0) {
          Iterator <Snake> itr = snakes.iterator();
          while (itr.hasNext()) {
            Snake cur = itr.next();
            SnakeConnection conn = cur.getConnection();
            if (conn != null) {
              conn.sendNumClients(snakes.size());
            }
          }
        }

        // Reset snakes that are ready for a reset.
        Iterator<Snake> itr = snakes.iterator();
        while (itr.hasNext()) {
          Snake cur = itr.next();
          if (cur.readyForReset()) {
            if (!cur.reset()) {
              cur.clearBody();
              cur.getConnection().disconnect();
            }
          }
        }

        // Create next-head map to detect head-on collisions.
        Set<Position> next_head_positions = new HashSet<Position>();
        Set<Position> collision_set = new HashSet<Position>();

        itr = snakes.iterator();
        while (itr.hasNext()) {
          Snake cur = itr.next();
          Position next_head = cur.prepareNextHeadPosition();
          if (next_head_positions.contains(next_head)) {
            collision_set.add(next_head);
          } else {
            next_head_positions.add(next_head);
          }
        }

        // Update snake positions.
        itr = snakes.iterator();
        while (itr.hasNext()) {
          // Either step or reap the object if connection died.
          Snake cur = itr.next();
          //System.out.println("Updating " + cur.getConnection().getClientId());
          if (cur.disconnected()) {
            disconnected_snakes.add(cur);
            itr.remove();
          } else {
            boolean snake_still_alive = cur.step(collision_set);
            if (!snake_still_alive) {
              dead_snakes.add(cur);
            }
          }
        }

        // Wait until all snakes have moved to clear the bodies (handles head to head collisions where both die).
        itr = dead_snakes.iterator();
        while (itr.hasNext()) {
          Snake cur = itr.next();
          cur.clearBody();
          //reportSnakeDied(cur.getId());
        }
        dead_snakes.clear();

        itr = disconnected_snakes.iterator();
        while (itr.hasNext()) {
          Snake cur = itr.next();
          cur.clearBody();
          //reportSnakeDisconnected(cur.getId());
        }
        disconnected_snakes.clear();

        // Send display info to each snake now that everything has been updated.
        itr = snakes.iterator();
        while (itr.hasNext()) {
          Snake cur = itr.next();
          cur.sendGameData();
        }

        // (Synchronously) resize the board.
        int num_snakes = snakes.size();
        if (num_snakes != 0) {
          double w = (double)board.getWidth(), h = (double)board.getHeight();
          double error = IDEAL_AREA_PER_CLIENT - (w * h) / (double)num_snakes;
          if (error > 0.0 && !board.full_size()) {
            board.resize((int)(w * 1.25), (int)(h * 1.25));
          }
        }
      }
    }
  }

  private class FoodPlacer implements Runnable {
    private static final int FOOD_PLACEMENT_PERIOD = 5000; // milliseconds
    private Board board;

    public FoodPlacer(Board board) {
      this.board = board;
    }

    public void run() {
      while (true) {
        int desired_food_amt = board.getWidth() * board.getHeight() / 60;
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
