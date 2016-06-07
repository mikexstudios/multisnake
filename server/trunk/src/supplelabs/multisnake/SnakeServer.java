/**
 * Snake Server
 *
 * Eugene Marinelli
 * 12/15/07
 */

package supplelabs.multisnake;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public class SnakeServer implements Runnable {
  private static final int PORT = 10123;
  private static final int MAX_CLIENTS = 2000;
  private static final int MAX_CONNECTIONS_PER_IP = 10;
  public static final int DEFAULT_ROUND_DURATION = 5*60; // seconds
  public static final int DEFAULT_INTERMISSION_DURATION = 15; // seconds

  public enum GameMode {GAME_MODE, INTERMISSION_MODE}
  public static boolean DEBUG_MODE;

  private int num_ai_players;
  private Board board;
  private Collection<Snake> snakes;
  private Collection<HumanSnake> pregame_snakes;
  private Map<String, Integer> connected_ip_counts;
  private boolean limit_connections_per_ip;
  private NotificationSender notification_sender;
  private boolean notifications_enabled;
  private int round_duration, intermission_duration;
  private String[] board_files;
  private int board_index;

  public SnakeServer(SnakeServerOptions options) {
    class PPMFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".ppm"));
        }
    }
    this.board_files = new File("./maps").list(new PPMFilter());
    if (this.board_files.length == 0) {
      System.out.println("ERROR no maps in map directory.");
      System.exit(1);
    }
    for (int i = 0; i < this.board_files.length; i++) {
      this.board_files[i] = "./maps/" + this.board_files[i];
    }

    String map_filename;
    if (options.map_filename == null) {
      map_filename = board_files[0];
    } else {
      map_filename = options.map_filename;
    }

    if (options.max_rows != null && options.max_cols != null) {
      this.board = new Board(map_filename, options.max_rows, options.max_cols);
    } else {
      this.board = new Board(map_filename);
    }
    snakes = Collections.synchronizedList(new LinkedList<Snake>());
    pregame_snakes = Collections.synchronizedList(new LinkedList<HumanSnake>());
    this.num_ai_players = options.num_ai;
    this.notifications_enabled = !(options.disable_notifs);

    this.round_duration = options.round_duration * 1000; // convert to milliseconds
    this.intermission_duration = options.intermission_duration * 1000;

    this.limit_connections_per_ip = options.limit_connections_per_ip;
    if (limit_connections_per_ip) {
      connected_ip_counts = new HashMap<String, Integer>();
    }

    DEBUG_MODE = options.debug_mode;
  }

  public int getNumClients() {
    return snakes.size();
  }

  public void run() {
    SnakeUpdater sup = new SnakeUpdater();
    Timer snake_update_timer = new Timer();

    SnakeConnectionPinger scp = new SnakeConnectionPinger();
    snake_update_timer.scheduleAtFixedRate(scp, 0, 35);

    //snake_update_timer.schedule(sup, 0, SnakeUpdater.UPDATE_PERIOD);
    snake_update_timer.scheduleAtFixedRate(sup, 0, SnakeUpdater.UPDATE_PERIOD);

    notification_sender = new NotificationSender();
    Thread notification_sender_thread = new Thread(notification_sender);
    notification_sender_thread.setPriority(Thread.MIN_PRIORITY);
    notification_sender_thread.start();

    for (int i = 0; i < num_ai_players; i++) {
      snakes.add(new ComputerSnake(board, "Bot" + i, true));
    }

    // Handling connections low priority compared to making rest of the game responsive.
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    try {
      ServerSocket listener = new ServerSocket(PORT);

      while (true) {
        Socket sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.
        //sock.setPerformancePreferences(0,2,1);

        if (getNumClients() != MAX_CLIENTS) {
          HumanSnake snake = new HumanSnake(board, sock);

          if (limit_connections_per_ip) {
            // Limit the number of connections per IP
            String ip = snake.getIP();
            Integer cur_count = connected_ip_counts.get(ip);
            if (cur_count == null) {
              connected_ip_counts.put(ip, 1);
            } else {
              if (cur_count.intValue() >= MAX_CONNECTIONS_PER_IP) {
                // Too many connections from this IP -- client is probably up to no good.
                sock.close();
                continue;
              } else {
                connected_ip_counts.put(ip, cur_count.intValue() + 1);
              }
            }
          }

          // Spawn client listener thread.
          SnakeConnection conn = new SnakeConnection(sock, snake);
          snake.setConnection(conn);
          Thread t = new Thread(conn);
          conn.setThread(t);
          t.setPriority(Thread.MAX_PRIORITY);
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

  private void reportSnakeJoined(Snake snake) {
    String common_msg = SnakeConnection.getJoinNotificationString(snake.getId());
    notification_sender.pushNotification(common_msg);
  }

  private void reportSnakeDisconnected(String id) {
    String common_msg = SnakeConnection.getLeaveNotificationString(id);
    notification_sender.pushNotification(common_msg);
  }

  private void reportKillOrDeath(Snake killer, Snake victim) {
    if (killer == victim) {
      // Suicide
      String common_msg = SnakeConnection.getDeathNotificationString(victim.getId());
      notification_sender.pushNotification(common_msg);
    } else {
      // Kill
      String common_msg = SnakeConnection.getKillNotificationString(killer.getId(), victim.getId());
      notification_sender.pushNotification(common_msg);
    }
  }

  private void reportRoundEnd() {
    System.out.println("End of round.");

    // Compute the ranking.
    SortedSet<Snake> sorted_snakes = new TreeSet<Snake>(snakes);
    String round_end_common_str = SnakeConnection.getRoundEndString(intermission_duration / 1000, sorted_snakes);

    int rank = 1;
    for (Snake s : sorted_snakes) {
      s.reportRoundEnd(round_end_common_str, rank);
      rank++;
    }
  }

  private void reportRoundBegin() {
    String round_begin_common_str = SnakeConnection.getRoundBeginString(round_duration / 1000);
    for (Snake s : snakes) {
      s.send(round_begin_common_str);
    }
  }

  private void handleDisconnect(Snake snake) {
    if (snake.getId() != null) { // Case where user hasn't entered an id yet.
      reportSnakeDisconnected(snake.getId());
    }

    if (limit_connections_per_ip) {
      // Decrement ip count.
      String ip = snake.getIP();
      if (ip != null) {
        Integer count = connected_ip_counts.get(snake.getIP());
        if (count.intValue() == 1) {
          connected_ip_counts.remove(ip);
        } else {
          connected_ip_counts.put(ip, count.intValue() - 1);
        }
      }
    }
  }

  private class SnakeUpdater extends TimerTask {
    //private static final int UPDATE_PERIOD = 140;//180; // milliseconds
    private static final int UPDATE_PERIOD = 10;//180; // milliseconds
    private static final double IDEAL_AREA_PER_CLIENT = 250.0;
    //private static final int SEND_NUM_CLIENTS_PERIOD = 200;

    //private int send_num_clients_counter;
    private GameMode game_mode;
    private long mode_end_time;

    public SnakeUpdater() {
      //send_num_clients_counter = 0;
      game_mode = GameMode.GAME_MODE;
      mode_end_time = System.currentTimeMillis() + round_duration; // First round will end at this time.
    }

    @Override
    public void run() {
      long cur_time = System.currentTimeMillis();

      synchronized (pregame_snakes) {
        Iterator<HumanSnake> itr = pregame_snakes.iterator();
        while (itr.hasNext()) {
          HumanSnake cur = itr.next();

          if (cur.disconnected()) {
            // Client disconnected before entering a name.
            handleDisconnect(cur);
            itr.remove();
          } else if (cur.readyToReport()) {
            // Client is ready to enter the game.
            itr.remove();
            reportSnakeJoined(cur);

            cur.sendRoundStatus(game_mode, (mode_end_time - cur_time) / 1000);
            snakes.add(cur);
          }
        }
      }

      switch (game_mode) {
        case INTERMISSION_MODE:
          if (mode_end_time < cur_time) { // If current time is after the mode end time...
            System.out.println("Round beginning.");

            game_mode = GameMode.GAME_MODE;
            mode_end_time = cur_time + round_duration;

            for (Snake cur : snakes) {
              assert(cur.readyForReset());
              cur.roundReset();
            }

            reportRoundBegin();
          }
          break;

        case GAME_MODE:
          if (mode_end_time < cur_time) { // If current time is after the mode end time, do intermission.
            game_mode = GameMode.INTERMISSION_MODE;
            mode_end_time = cur_time + intermission_duration;
            board.clearFood();
            reportRoundEnd();

            // Clear all snakes.
            for (Snake cur : snakes) {
              cur.handleRoundEnd();
            }

            board_index = (board_index + 1) % board_files.length;
            board = new Board(board_files[board_index]); // TODO should use options max_rows and max_cols here if appliciable
            for (Snake s : snakes) {
              s.setBoard(board);
            }
          } else {
            doGameStep();
          }
          break;
      }

      // Flush all data.
      synchronized (snakes) {
        // Set<Thread> threads = new HashSet<Thread>();
        for (Snake cur : snakes) {
          Thread t = new Thread(new SnakeFlusher(cur));
          //threads.add(t);
          t.setPriority(Thread.MAX_PRIORITY);
          t.start();
        }
				/*
        for (Thread t : threads) {
          try {
            t.join();
          } catch (Exception e) {}
					}*/
      }
    }

    private void resetSnakes() {
      Iterator<Snake> itr = snakes.iterator();
      while (itr.hasNext()) {
        Snake cur = itr.next();
        if (cur.readyForReset()) {
          assert(cur.getLength() == 0) : cur + " is readyForReset but its length is " + cur.getLength();

          if (!cur.reset()) {
            // Reset failed.
            itr.remove();
            cur.clearBody();
            cur.disconnect();
          }
        }
      }
    }

    private void stepSnakes() {
      synchronized (snakes) {
        // Check for disconnects.
        Iterator<Snake> itr = snakes.iterator();
        while (itr.hasNext()) {
          Snake s = itr.next();
          if (s.disconnected()) {
            itr.remove();
            s.clearBody();
            handleDisconnect(s);
          }
        }

        // Create map from position to list of snakes trying to move
        // into this position.  Snake of maximum length survives.  If
        // there is a tie for longest, every snake in the list dies.
        // A directed graph of killings is constructed.  Each snake
        // has a set of snakes which it has killed during this step.
        // At the end, the bodies of these snakes are cleared, and the
        // killers get credit for the kill.  Snakes that are found to
        // be able to move forward are advanced at the end of the
        // step.

        Map<Position, List<Snake>> snakes_moving_into_pos = new HashMap<Position, List<Snake>>();
        for (Snake s : snakes) {
          if (!s.isDead() && s.moving()) {
            assert(s.getLength() > 0);

            Position next_head = s.prepareNextHeadPosition();
            if (next_head != null) {
              Board.Cell pos_value = board.getBoardValue(next_head);
              if (pos_value == null) {
                s.addKilledSnake(s);
              } else {
                switch (pos_value.getStatus()) {
                case FOOD:
                case FREE:
                  if (snakes_moving_into_pos.get(next_head) == null) {
                    snakes_moving_into_pos.put(next_head, new ArrayList<Snake>(2));
                  }
                  snakes_moving_into_pos.get(next_head).add(s);
                  break;
                case SNAKE:
                  Snake other = pos_value.getSnake();
                  assert(other.getLength() >= 1);

                  if (s.getLength() == 1) {
                    other.addKilledSnake(s);
                  } else {
                    if (other == s) {
                      // Ran into ourself.
                      s.addKilledSnake(s);
                    } else {
                      if (other.getLength() > 1) {
                        other.addKilledSnake(s);
                      } else {
                        // We try to eat this single unit snake.
                        if (snakes_moving_into_pos.get(next_head) == null) {
                          snakes_moving_into_pos.put(next_head, new ArrayList<Snake>(2));
                        }
                        snakes_moving_into_pos.get(next_head).add(s);
                        // Even if other's length is 1, don't register the kill here -- multiple snakes might
                        // converge on the same single unit snake simultaneously.
                      }
                    }
                  }
                  break;
                case WALL:
                case NULL:
                  s.addKilledSnake(s);
                  break;
                default:
                  s.addKilledSnake(s);
                  break;
                }
              }
            }
          }
        }

        Map<Snake, Position> final_moves = new HashMap<Snake, Position>();
        for (Map.Entry<Position,List<Snake>> e : snakes_moving_into_pos.entrySet()) {
          List<Snake> snakes_here = e.getValue();
          //Position pos = e.getKey();
          assert(snakes_here != null && snakes_here.size() >= 1);

          if (snakes_here.size() == 1) {
            // No contention, the snake in this set wins.
            final_moves.put(snakes_here.get(0), e.getKey());
          } else {
            int max_len = -1;
            List<Snake> max_snakes = new ArrayList<Snake>();

            for (Snake s : snakes_here) {
              if (s.getLength() > max_len) {
                max_snakes.clear();
                max_len = s.getLength();
                max_snakes.add(s);
              } else if (s.getLength() == max_len) {
                max_snakes.add(s);
              }
            }

            assert(max_snakes.size() > 0);
            Snake winner = null;
            if (max_snakes.size() == 1) {
              // One snake prevails.
              winner = max_snakes.get(0);
              final_moves.put(winner, e.getKey());

              for (Snake s : snakes_here) {
                if (s != winner) {
                  winner.addKilledSnake(s);
                }
              }
            } else {
              // No winner here, so we say that each snake just dies.
              for (Snake s : snakes_here) {
                s.addKilledSnake(s);
              }
            }
          }
        }

        for (Map.Entry<Snake,Position> e : final_moves.entrySet()) {
          Snake s = e.getKey();
          Position next_pos = e.getValue();

          Board.Cell pos_val = board.getBoardValue(next_pos);
          switch (pos_val.getStatus()) {
            case FREE: break; // Successfully occupy position.
            case FOOD: s.handleFood(); break;
            case SNAKE:
              // Now snake eat the baby snake located at the position it's moving into.
              Snake other = pos_val.getSnake();
              assert(other.getLength() == 1);
              s.addKilledSnake(other);
              break;
            case WALL:
            case NULL:
              assert(false);
              break;
          }
        }

        // Register all kills and clear dead bodies.
        for (Snake s : snakes) {
          for (Snake victim : s.snakesKilledThisStep()) {
            victim.handleKilledBy(s);
            reportKillOrDeath(s, victim);
            victim.clearBody();
          }
        }

        // Now step all snakes that didn't die into their next head positions.
        for (Map.Entry<Snake,Position> e : final_moves.entrySet()) {
          Snake s = e.getKey();
          if (!s.isDead()) { // Snake may have been eaten.
            e.getKey().commitMove();
          }
        }

        for (Snake s : snakes) {
          s.clearKills();
        }
      }
    }

    private void doGameStep() {
      synchronized (snakes) {
        placeFood();

        // Reset snakes that are ready for a reset.
        resetSnakes();
        stepSnakes();

        // Send display info to each snake now that everything has been updated.
        Set<Thread> threads = new HashSet<Thread>();
        for (Snake cur : snakes) {
          /*
          Overhead actually outweighs parallelism here.
          Thread t = new Thread(new SnakeGameDataSender(cur));
          threads.add(t);
          t.setPriority(Thread.MAX_PRIORITY);
          t.start();
          */
          cur.sendGameData();
        }
        for (Thread t : threads) {
          try {
            t.join();
          } catch (Exception e) {}
        }

        int num_snakes = getNumClients();
        if (num_snakes != 0) {
          double w = board.getWidth(), h = board.getHeight();
          double error = IDEAL_AREA_PER_CLIENT - (w * h) / num_snakes;
          if (error > 0.0 && !board.full_size()) {
            // Synchronously resize the board.
            board.resize((int)(w * 1.25), (int)(h * 1.25));
          }
        }
      }
    }
  }

  private void placeFood() {
    int desired_food_amt = board.getWidth() * board.getHeight() / 60;
    int error = desired_food_amt - board.getFoodAmount();
    int amount_to_add = error / 4;

    for (int i = 0; i < amount_to_add; i++) {
      board.placeRandomFood();
    }
  }

  private class NotificationSender implements Runnable {
    private static final int NOTIFICTION_SEND_PERIOD = 2500; // milliseconds
    private static final int MAX_QUEUED_NOTIFICATIONS = 3;

    private Queue<Notification> notification_queue;

    private class Notification {
      public String message;

      public Notification(String message) {
        this.message = message;
      }
    }

    public NotificationSender() {
      notification_queue = new LinkedList<Notification>();
    }

    public void pushNotification(String n) {
      synchronized (notification_queue) {
        if (notification_queue.size() == MAX_QUEUED_NOTIFICATIONS) {
          notification_queue.poll();
        }
        notification_queue.offer(new Notification(n));
      }
    }

    public void run() {
      while (true) {
        synchronized (snakes) {
          synchronized (notification_queue) {
            for (Snake cur : snakes) {
              cur.sendNumClients(getNumClients());
              if (notifications_enabled) {
                for (Notification n : notification_queue) {
                  cur.send(n.message);
                }
              }
            }
          }
        }

        notification_queue.clear();

        try {
          Thread.sleep(NOTIFICTION_SEND_PERIOD);
        } catch (InterruptedException ie) {}
      }
    }
  }

  private class SnakeFlusher implements Runnable {
    Snake snake;

    public SnakeFlusher(Snake s) {
      this.snake = s;
    }

    public void run() {
      snake.flush_connection();
    }
  }

  private class SnakeConnectionPinger extends TimerTask {
      @Override
      public void run() {
	  synchronized (snakes) {
	      for (Snake s : snakes) {
		  if (s.conn != null) {
		      s.conn.send("0");
		      s.flush_connection();
		  }
	      }
	  }
      }
  }

  /*
  private class SnakeGameDataSender implements Runnable {
    Snake snake;

    public SnakeGameDataSender(Snake s) {
      this.snake = s;
    }

    public void run() {
      snake.sendGameData();
    }
  }*/
}
