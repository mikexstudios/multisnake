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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
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

  // TODO put this in a file
  private static final String[] FAKE_NAMES = {"FM", "_DeKe_", "00111111", "3xH", "acerbits", "admiralFlam",
    "alaskan_thunder", "Alecto", "AMDOCer", "Anjel_Few", "ant0", "Arathorn", "Arienhode", "Axis101a", "Babinnes",
    "BandofBro",
    "bd_braindead", "BennoUK", "bigspoon", "bobadidlio", "Bomberaman", "Boondock", "bn_me", "Bucken77", "castun",
    "chrisin", "cleeve", "cobee", "coffeym", "cruiza2002", "DarkManX_BG", "Darthtoast", "DaSnIpE", "daverave29",
    "dbrunner", "DeJay", "denrock316", "Didzi", "Dj_Lushious", "djorgji", "DKPowers", "Dutchforce", "el_schorpio",
    "E_PaiN", "Eiridan", "epking", "eXceL", "exemjr", "fairyliquidizer", "foggy", "fuzzywuzzy", "ge4ce", "Gearbox",
    "geforce_man", "Ginjarou", "God_Of_Death", "GolemFrost", "GreenPsycho", "Grundy", "grungekid", "h4x0r", "Halo_Evo",
    "harbinger", "harrisburg45", "heller91", "hellraiserAS", "Hezeuschrist", "hitbob", "Hive1984", "Horrorwood",
    "Racy", "I_C", "IndianScout", "Jblaze2", "jdhooghe85", "jfitzw", "JoshHarrin", "JSDonald", "Jura", "KaFFeinE",
    "ketel", "killem2", "kiltdscot", "kingpins23", "ieatn", "linnyloo", "laughingboy", "M75", "Madgoat",
    "Main0", "Mantis", "Masterjosh", "MaSSicar", "matt_jedi", "MaverickDBZ", "Max_Powerz", "Maximoose",
    "maxvcore", "MAXXVERTIGO", "mike_hock", "mikeh", "mmoses", "Mr_DoughBoy", "MrWalker", "munkieNS", "Mystic_llama",
    "Mystic_Pickle", "mzman", "nahus", "Nozzer", "OhMyGod", "omega_forts", "P3sTiLeNcE", "Pac0", "PaTmAx69",
    "pc_fanatic", "Pearl_Jam", "perfectmark", "PimpFoxx", "Preddit", "pugheaven", "quantized", "QuIcKsIlv3r", "Racy",
    "Rage710", "RAP1D", "Raptor156", "rattler_nj", "rayhn", "RealyPssd", "RedLotus", "RevMaynard", "Revo1ver",
    "roger2911", "Romaster", "root_rooot", "rothchilds", "russ18uk", "Ruzhyo", "s0up", "s_j", "saboism", "Sarge_2",
    "Schphonic", "Sean7", "Septic_Phlegm", "Sink41", "smokeyuk", "sMull", "sncrash512", "SnW", "SocialDefect",
    "Solidusake", "someonescop", "spammy2505", "Splizxer", "Sr8Fr4gging", "SRCLINTON", "TBUG", "Tidy_Sammy",
    "Tonks", "twonha", "Uh_Oh", "usnavyf14pilot", "Vampitino1", "ViolatorX", "VoracioGora", "WhiteDevil",
    "williamweaver", "WinterLion", "WiZZyWiGG", "WR2701S", "Wutog", "x", "XeoBllaze", "YoyoManA", "Zaynee"};

  private int num_ai_players;
  private Board board;
  private Collection<HumanSnake> human_snakes;
  private Collection<ComputerSnake> computer_snakes;
  private Collection<HumanSnake> pregame_snakes;
  private Map<String, Integer> connected_ip_counts;
  private boolean limit_connections_per_ip;
  private NotificationSender notification_sender;
  private boolean notifications_enabled;
  private int round_duration, intermission_duration;

  public SnakeServer(int num_ai_players, boolean limit_connections_per_ip, boolean disable_notifs,
    int round_duration, int intermission_duration) {
    board = new Board();
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs, round_duration, intermission_duration);
  }

  public SnakeServer(int num_ai_players, String map_filename, boolean limit_connections_per_ip,
    boolean disable_notifs, int round_duration, int intermission_duration) {
    board = new Board(map_filename);
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs, round_duration, intermission_duration);
  }

  public SnakeServer(int max_rows, int max_cols, int num_ai_players, boolean limit_connections_per_ip,
    boolean disable_notifs, int round_duration, int intermission_duration) {
    board = new Board(max_rows, max_cols);
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs, round_duration, intermission_duration);
  }

  public SnakeServer(int max_rows, int max_cols, int num_ai_players, String map_filename,
    boolean limit_connections_per_ip, boolean disable_notifs, int round_duration, int intermission_duration) {
    board = new Board(map_filename, max_rows, max_cols);
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs, round_duration, intermission_duration);
  }

  private void init_common(int num_ai_players, boolean limit_connections_per_ip, boolean disable_notifs,
    int round_duration, int intermission_duration) {
    human_snakes = Collections.synchronizedList(new LinkedList<HumanSnake>());
    computer_snakes = Collections.synchronizedList(new LinkedList<ComputerSnake>());
    pregame_snakes = Collections.synchronizedList(new LinkedList<HumanSnake>());
    this.num_ai_players = num_ai_players;
    this.notifications_enabled = !disable_notifs;
    
    this.round_duration = round_duration * 1000; // convert to milliseconds
    this.intermission_duration = intermission_duration * 1000;

    this.limit_connections_per_ip = limit_connections_per_ip;
    if (limit_connections_per_ip) {
      connected_ip_counts = new HashMap<String, Integer>();
    }
  }

  public int getNumClients() {
    return human_snakes.size() + computer_snakes.size();
  }

  public void run() {
    SnakeUpdater sup = new SnakeUpdater();
    Timer snake_update_timer = new Timer();
    //snake_update_timer.schedule(sup, 0, SnakeUpdater.UPDATE_PERIOD);
    snake_update_timer.scheduleAtFixedRate(sup, 0, SnakeUpdater.UPDATE_PERIOD);

    FoodPlacer fp = new FoodPlacer(board);
    Thread food_placer = new Thread(fp);
    food_placer.setPriority(Thread.MIN_PRIORITY);
    food_placer.start();

    notification_sender = new NotificationSender();
    Thread notification_sender_thread = new Thread(notification_sender);
    notification_sender_thread.setPriority(Thread.MIN_PRIORITY);
    notification_sender_thread.start();

    Random randgen = new Random();
    for (int i = 0; i < num_ai_players; i++) {
      computer_snakes.add(new ComputerSnake(board, FAKE_NAMES[Math.abs(randgen.nextInt()) % FAKE_NAMES.length]));
    }

    // Handling connections low priority compared to making rest of the game responsive.
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    try {
      ServerSocket listener = new ServerSocket(PORT);

      while (true) {
        Socket sock = listener.accept();
        sock.setTcpNoDelay(true); // Suggested for flash XMLSocket.
        sock.setPerformancePreferences(0,1,1);

        if (getNumClients() != MAX_CLIENTS) {
          HumanSnake snake = new HumanSnake(board, sock);
          SnakeConnection conn = snake.getConnection();
          assert(conn != null);

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
    Set<Snake> excluded = new HashSet();
    excluded.add(snake);
    notification_sender.pushNotification(common_msg, excluded);
  }

  private void reportSnakeDisconnected(String id) {
    String common_msg = SnakeConnection.getLeaveNotificationString(id);
    notification_sender.pushNotification(common_msg, new HashSet<Snake>());
  }

  private void reportSnakeDied(Snake snake) {
    String common_msg = SnakeConnection.getDeathNotificationString(snake.getId());
    Set<Snake> excluded = new HashSet<Snake>();
    excluded.add(snake);
    notification_sender.pushNotification(common_msg, excluded);
  }

  private void reportKill(Snake killer, Snake victim) {
    String common_msg = SnakeConnection.getKillNotificationString(killer.getId(), victim.getId());
    Set<Snake> excluded = new HashSet();
    excluded.add(killer);
    excluded.add(victim);
    notification_sender.pushNotification(common_msg, excluded);
  }

  private void reportRoundEnd() {
    System.out.println("End of round.");

    if (human_snakes.size() == 0) {
      return;
    }

    // Compute the ranking.
    SortedSet<HumanSnake> human_sorted_set = new TreeSet<HumanSnake>(human_snakes);
    SortedSet<Snake> all_snakes = new TreeSet<Snake>(human_sorted_set);
    all_snakes.addAll(computer_snakes); // Adding to treeset should result in sorted set.

    String round_end_common_str = SnakeConnection.getRoundEndString(intermission_duration / 1000, all_snakes);

    Iterator<Snake> itr = all_snakes.iterator();
    int rank = 1;
    while (itr.hasNext()) {
      Snake cur = itr.next();
      SnakeConnection conn = cur.getConnection();
      if (conn != null) {
        conn.reportRoundEnd(round_end_common_str, rank);
      }
      rank++;
    }
  }

  private void reportRoundBegin() {
    String round_begin_common_str = SnakeConnection.getRoundBeginString(round_duration / 1000);
    Iterator<HumanSnake> human_itr = human_snakes.iterator();
    while (human_itr.hasNext()) {
      HumanSnake cur = human_itr.next();
      SnakeConnection cur_conn = cur.getConnection();
      cur_conn.send(round_begin_common_str);
    }
  }

  private void handleDisconnect(HumanSnake snake) {
    if (notifications_enabled) {
      String id = snake.getId();
      if (id != null) { // Case where user hasn't entered an id yet.
        reportSnakeDisconnected(id);
      }
    }

    if (limit_connections_per_ip) {
      // Decrement ip count.
      String ip = snake.getIP();
      Integer count = connected_ip_counts.get(snake.getIP());
      if (count.intValue() == 1) {
        connected_ip_counts.remove(ip);
      } else {
        connected_ip_counts.put(ip, count.intValue() - 1);
      }
    }
  }

  private class SnakeUpdater extends TimerTask {
    private static final int UPDATE_PERIOD = 140;//180; // milliseconds
    private static final double IDEAL_AREA_PER_CLIENT = 250.0;
    private static final int SEND_NUM_CLIENTS_PERIOD = 200;

    private int send_num_clients_counter;
    private GameMode game_mode;
    private long mode_end_time;

    public SnakeUpdater() {
      send_num_clients_counter = 0;
      game_mode = GameMode.GAME_MODE;

      mode_end_time = System.currentTimeMillis() + round_duration; // First round will end at this time.
    }

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

            if (notifications_enabled) {
              reportSnakeJoined(cur);
            }

            SnakeConnection cur_conn = cur.getConnection();
            if (cur_conn != null) {
              cur_conn.sendRoundStatus(game_mode, (mode_end_time - cur_time) / 1000);
              human_snakes.add(cur);
            } else {
              handleDisconnect(cur);
            }
          }
        }
      }

      switch (game_mode) {
        case INTERMISSION_MODE:
          if (mode_end_time < cur_time) { // If current time is after the mode end time...
            System.out.println("Round beginning.");

            game_mode = GameMode.GAME_MODE;
            mode_end_time = cur_time + round_duration;

            // Reset all snakes.
            for (HumanSnake cur : human_snakes) {
              cur.roundReset();
            }
            for (ComputerSnake cur : computer_snakes) {
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
          } else {
            doGameStep();
          } 
          break;
      }

      // Flush all data.
      Iterator<HumanSnake> itr = human_snakes.iterator();
      while (itr.hasNext()) {
        HumanSnake cur = itr.next();
        SnakeConnection cur_conn = cur.getConnection();
        if (cur_conn == null) {
          handleDisconnect(cur);
          itr.remove();
          cur.clearBody();
        } else {
          cur_conn.flush();
        }
      }
    }

    private void resetSnakes() {
      Iterator<HumanSnake> itr = human_snakes.iterator();
      while (itr.hasNext()) {
        HumanSnake cur = itr.next();
        SnakeConnection cur_conn = cur.getConnection();

        if (cur_conn != null) {
          if (cur.readyForReset()) {
            if (!cur.reset()) {
              // Reset failed.
              itr.remove();
              cur.clearBody();
              cur_conn.disconnect();
            }
          }
        }
      }

      Iterator<ComputerSnake> itr2 = computer_snakes.iterator();
      while (itr2.hasNext()) {
        ComputerSnake cur = itr2.next();
        if (cur.readyForReset()) {
          if (!cur.reset()) {
            // Reset failed.
            itr.remove();
            cur.clearBody();
          }
        }
      }
    }

    private void stepSnakes() {
      Set<Position> next_head_positions = new HashSet<Position>();
      Set<Position> collision_set = new HashSet<Position>();
      Map<Position, Snake> single_next_head_map = new HashMap<Position, Snake>();
      Collection<Snake> dead_snakes = new ArrayList<Snake>();

      Iterator<HumanSnake> itr = human_snakes.iterator();
      while (itr.hasNext()) {
        HumanSnake cur = itr.next();
        if (cur.disconnected()) {
          dead_snakes.add(cur);
          handleDisconnect(cur);
          itr.remove();
        } else {
          Position next_head = cur.prepareNextHeadPosition();

          if (cur.getLength() == 1) {
            single_next_head_map.put(next_head, cur);
          }

          if (next_head_positions.contains(next_head)) {
            collision_set.add(next_head);
          } else {
            next_head_positions.add(next_head);
          }
        }
      }

      for (ComputerSnake cur : computer_snakes) {
        Position next_head = cur.prepareNextHeadPosition();

        if (cur.getLength() == 1) {
          single_next_head_map.put(next_head, cur);
        }

        if (next_head_positions.contains(next_head)) {
          collision_set.add(next_head);
        } else {
          next_head_positions.add(next_head);
        }
      }

      // Update snake positions.
      itr = human_snakes.iterator();
      while (itr.hasNext()) {
        HumanSnake cur = itr.next();
        // Either step or reap the object if connection died.
        if (cur.disconnected()) {
          handleDisconnect(cur);
          itr.remove();
          dead_snakes.add(cur);
        } else {
          Snake killed_by = cur.step(collision_set, single_next_head_map);
          if (killed_by != null) {
            dead_snakes.add(cur);
            if (notifications_enabled) {
              if (killed_by != cur) {
                // Killed by another snake.
                reportKill(killed_by, cur);
                cur.setKillReported();
                killed_by.registerKill(cur);
              } else {
                // No killer / suicide.
                reportSnakeDied(cur);
              }
            }
          }
        }
      }

      for (ComputerSnake cur : computer_snakes) {
        Snake killed_by = cur.step(collision_set, single_next_head_map);
        if (killed_by != null) {
          dead_snakes.add(cur);
          if (notifications_enabled) {
            if (killed_by != cur) {
              reportKill(killed_by, cur);
              cur.setKillReported();
              killed_by.registerKill(cur);
            } else {
              reportSnakeDied(cur);
            }
          }
        }
      }


      // Check for eaten snakes.
      for (HumanSnake cur : human_snakes) {
        Snake killer = cur.getKiller();
        if (killer != null) {
          dead_snakes.add(cur);
          if (!cur.getKillReported() && notifications_enabled) {
            reportKill(killer, cur);
            cur.setKillReported();
          }
        }
      }

      for (ComputerSnake cur : computer_snakes) {
        Snake killer = cur.getKiller();
        if (killer != null) {
          dead_snakes.add(cur);
          if (!cur.getKillReported() && notifications_enabled) {
            reportKill(killer, cur);
            cur.setKillReported();                  
          }
        }
      }

      // Wait until all snakes have moved to clear the bodies (handles head to head collisions where both die).
      for (Snake cur : dead_snakes) {
        cur.clearBody();
      }
    }

    private void doGameStep() {
      synchronized (human_snakes) {
        synchronized (computer_snakes) {
          // Reset snakes that are ready for a reset.
          resetSnakes();

          stepSnakes();

          // Send display info to each snake now that everything has been updated.
          for (HumanSnake cur : human_snakes) {
            if (cur.disconnected()) {
              human_snakes.remove(cur);
              cur.clearBody();
            } else {
              cur.sendGameData();
            }
          }

          // Synchronously resize the board.
          int num_snakes = getNumClients();
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

  private class NotificationSender implements Runnable {
    private static final int NOTIFICTION_SEND_PERIOD = 2500; // milliseconds
    private static final int MAX_QUEUED_NOTIFICATIONS = 3;

    private Queue<Notification> notification_queue;

    private class Notification {
      public String message;
      public Set<Snake> excluded_clients;

      public Notification(String message, Set<Snake> excluded_clients) {
        this.message = message;
        this.excluded_clients = excluded_clients;
      }
    }

    public NotificationSender() {
      notification_queue = new LinkedList<Notification>();
    }

    public void pushNotification(String n, Set<Snake> excluded_clients) {
      synchronized (notification_queue) {
        if (notification_queue.size() == MAX_QUEUED_NOTIFICATIONS) {
          notification_queue.poll();
        }
        notification_queue.offer(new Notification(n, excluded_clients));
      }
    }

    public void run() {
      while (true) {
        synchronized (human_snakes) {
          synchronized (notification_queue) {
            for (HumanSnake cur : human_snakes) {
              SnakeConnection conn = cur.getConnection();
              if (conn != null) {
                conn.sendNumClients(getNumClients());
                if (notifications_enabled) {
                  for (Notification n : notification_queue) {
                    if (!n.excluded_clients.contains(cur)) {
                      conn.send(n.message);
                    }
                  }
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
}
