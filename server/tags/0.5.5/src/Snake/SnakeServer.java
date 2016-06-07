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
import java.util.Timer;
import java.util.TimerTask;

public class SnakeServer implements Runnable {
  private static final int PORT = 10123;
  private static final int MAX_CLIENTS = 2000;
  private static final int MAX_CONNECTIONS_PER_IP = 10;
  private static final String[] FAKE_NAMES = {"FM", "_DeKe_", "00111111", "3xH", "acerbits", "admiralFlameberg",
    "alaskan_thunder", "Alecto", "AMDOCer", "Anjel_Few", "ant0", "Arathorn", "Arienhode", "Axis101a", "Babinnes",
    "BandofBrothers",
    "bd_braindead", "BennoUK", "bigspoon", "bobadidlio", "Bomberman", "Boondock_Saint", "bn_me", "Bucken77", "castun",
    "chrisinthesun20", "cleeve", "cobee", "coffeym", "cruiza2002", "DarkManX_BG", "Darthtoast", "DaSnIpE", "daverave29",
    "dbrunner", "DeJay", "denrock316", "Didzi", "Dj_Lushious", "djorgji", "DKPowers", "Dutchforce", "el_schorpio",
    "E_PaiN", "Eiridan", "epking", "eXceL", "exemjr", "fairyliquidizer", "foggy", "fuzzywuzzy", "ge4ce", "Gearbox",
    "geforce_man", "Ginjarou", "God_Of_Death", "GolemFrost", "GreenPsycho", "Grundy", "grungekid", "h4x0r", "Halo_Evo",
    "harbinger", "harrisburg45", "heller91", "hellraiserASC", "Hezeuschrist", "hitbob", "Hive1984", "Horrorwood",
    "Racy", "I_C", "IndianScout", "Jblaze2", "jdhooghe85", "jfitzw", "JoshHarrington", "JSDonald", "Jura", "KaFFeinE",
    "ketel", "killem2", "kiltdscot", "kingpins23", "IeatNvidiots", "linnyloo", "laughingboy", "M75", "Madgoat",
    "Main0", "Mantis", "Masterjosh", "MaSSicar", "matt_jediknight", "MaverickDBZ", "Max_Powerz", "Maximoose",
    "maxvcore", "MAXXVERTIGO", "mike_hock", "mikeh", "mmoses", "Mr_DoughBoy", "MrWalker", "munkieNS", "Mystic_llama",
    "Mystic_Pickle", "mzman", "nahus", "Nozzer", "OhMyGod", "omega_forts", "P3sTiLeNcE", "Pac0", "PaTmAx69",
    "pc_fanatic", "Pearl_Jam", "perfectmark", "PimpFoxx", "Preddit", "pugheaven", "quantized", "QuIcKsIlv3r", "Racy",
    "Rage710", "RAP1D", "Raptor156", "rattler_nj", "rayhn", "RealyPssd", "RedLotus", "RevMaynard_TAW", "Revo1ver",
    "roger2911", "Romaster", "root_rooot", "rothchilds", "russ18uk", "Ruzhyo", "s0up", "s_j", "saboism", "Sarge_2",
    "Schizophonic", "Sean7", "Septic_Phlegm", "Sink41", "smokeyuk", "sMull", "snowcrash512", "SnW", "SocialDefect",
    "Solidus_Snake", "someonescop", "spammy2505", "Splizxer", "Sr8Fr4gging", "SRCLINTON", "TBUG", "Tidy_Sammy",
    "Tonks", "twonha", "Uh_Oh", "usnavyf14pilot", "Vampirolatino1", "ViolatorX", "VoraciousGorak", "WhiteDevil",
    "williamweaver", "WinterLionatGC", "WiZZyWiGG", "WR2701S", "Wutog", "x", "XeoBllaze", "YoyoManA", "Zaynee"};

  private int num_ai_players;
  private Board board;
  private Collection<HumanSnake> human_snakes;
  private Collection<ComputerSnake> computer_snakes;
  private Collection<HumanSnake> pregame_snakes;
  private Map<String, Integer> connected_ip_counts;
  private boolean limit_connections_per_ip;
  private NotificationSender notification_sender;
  private boolean notifications_enabled;

  public SnakeServer(int num_ai_players, boolean limit_connections_per_ip, boolean disable_notifs) {
    board = new Board();
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs);
  }

  public SnakeServer(int num_ai_players, String map_filename, boolean limit_connections_per_ip,
    boolean disable_notifs) {
    board = new Board(map_filename);
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs);
  }

  public SnakeServer(int max_rows, int max_cols, int num_ai_players, boolean limit_connections_per_ip,
    boolean disable_notifs) {
    board = new Board(max_rows, max_cols);
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs);
  }

  public SnakeServer(int max_rows, int max_cols, int num_ai_players, String map_filename,
    boolean limit_connections_per_ip, boolean disable_notifs) {
    board = new Board(map_filename, max_rows, max_cols);
    init_common(num_ai_players, limit_connections_per_ip, disable_notifs);
  }

  private void init_common(int num_ai_players, boolean limit_connections_per_ip, boolean disable_notifs) {
    human_snakes = Collections.synchronizedList(new LinkedList<HumanSnake>());
    computer_snakes = Collections.synchronizedList(new LinkedList<ComputerSnake>());
    pregame_snakes = Collections.synchronizedList(new LinkedList<HumanSnake>());
    this.num_ai_players = num_ai_players;
    this.notifications_enabled = !disable_notifs;

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
    snake_update_timer.schedule(sup, 0, SnakeUpdater.UPDATE_PERIOD);

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

        if (getNumClients() != MAX_CLIENTS) {
          HumanSnake snake = new HumanSnake(board, sock);
          SnakeConnection conn = snake.getConnection();

          if (limit_connections_per_ip) {
            // Limit the number of connections per IP
            String ip = snake.getIP();
            Integer cur_count = connected_ip_counts.get(ip);
            if (cur_count == null) {
              connected_ip_counts.put(ip, 1);
            } else {
              if (cur_count.intValue() >= MAX_CONNECTIONS_PER_IP) {
                // Too many connections from this IP, probably a hacker.
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

  private void reportSnakeJoined(String id) {
    String common_msg = SnakeConnection.getJoinNotificationString(id);
    notification_sender.pushNotification(common_msg);
  }

  private void reportSnakeDisconnected(String id) {
    String common_msg = SnakeConnection.getLeaveNotificationString(id);
    notification_sender.pushNotification(common_msg);
  }

  private void reportSnakeDied(String id) {
    String common_msg = SnakeConnection.getDeathNotificationString(id);
    notification_sender.pushNotification(common_msg);
  }

  private class SnakeUpdater extends TimerTask {
    private static final int UPDATE_PERIOD = 150; // milliseconds
    private static final double IDEAL_AREA_PER_CLIENT = 100.0;

    private Collection<Snake> dead_snakes; // This is a class variable since we only want to allocate it once.
    private Collection<HumanSnake> disconnected_snakes;

    Set<Position> next_head_positions;
    Set<Position> collision_set;

    private static final int SEND_NUM_CLIENTS_PERIOD = 200;
    private int send_num_clients_counter;

    public SnakeUpdater() {
      dead_snakes = new ArrayList<Snake>();
      disconnected_snakes = new ArrayList<HumanSnake>();

      next_head_positions = new HashSet<Position>();
      collision_set = new HashSet<Position>();

      send_num_clients_counter = 0;
    }

    public void run() {
      synchronized (pregame_snakes) {
        Iterator <HumanSnake> itr = pregame_snakes.iterator();
        while (itr.hasNext()) {
          HumanSnake cur = itr.next();

          if (cur.disconnected()) {
            disconnected_snakes.add(cur);
            itr.remove();
          } else if (cur.readyToReport()) {
            itr.remove();

            if (notifications_enabled) {
              reportSnakeJoined(cur.getId());
            }

            human_snakes.add(cur);
          }
        }
      }

      synchronized (human_snakes) {
      synchronized (computer_snakes) {
        // Reset snakes that are ready for a reset.
        Iterator<HumanSnake> human_itr = human_snakes.iterator();
        while (human_itr.hasNext()) {
          HumanSnake cur = human_itr.next();
          if (cur.readyForReset()) {
            if (!cur.reset()) {
              // Reset failed.
              cur.clearBody();
              cur.getConnection().disconnect();
            }
          }
        }

        Iterator<ComputerSnake> comp_itr;
        if (num_ai_players > 0) {
          comp_itr = computer_snakes.iterator();
          while (comp_itr.hasNext()) {
            ComputerSnake cur = comp_itr.next();
            if (cur.readyForReset()) {
              if (!cur.reset()) {
                // Reset failed.
                cur.clearBody();
              }
            }
          }
        }

        // Create next-head map to detect head-on collisions.  Use pre-allocated data structures.
        next_head_positions.clear();
        collision_set.clear();

        human_itr = human_snakes.iterator();
        while (human_itr.hasNext()) {
          HumanSnake cur = human_itr.next();
          Position next_head = cur.prepareNextHeadPosition();
          if (next_head_positions.contains(next_head)) {
            collision_set.add(next_head);
          } else {
            next_head_positions.add(next_head);
          }
        }

        if (num_ai_players > 0) {
          comp_itr = computer_snakes.iterator();
          while (comp_itr.hasNext()) {
            ComputerSnake cur = comp_itr.next();
            Position next_head = cur.prepareNextHeadPosition();
            if (next_head_positions.contains(next_head)) {
              collision_set.add(next_head);
            } else {
              next_head_positions.add(next_head);
            }
          }
        }

        // Update snake positions.
        human_itr = human_snakes.iterator();
        while (human_itr.hasNext()) {
          // Either step or reap the object if connection died.
          HumanSnake cur = human_itr.next();
          if (cur.disconnected()) {
            disconnected_snakes.add(cur);
            human_itr.remove();
          } else {
            boolean snake_still_alive = cur.step(collision_set);
            if (!snake_still_alive) {
              dead_snakes.add(cur);
            }
          }
        }

        if (num_ai_players > 0) {
          comp_itr = computer_snakes.iterator();
          while (comp_itr.hasNext()) {
            ComputerSnake cur = comp_itr.next();
            boolean snake_still_alive = cur.step(collision_set);
            if (!snake_still_alive) {
              dead_snakes.add(cur);
            }
          }
        }

        // Wait until all snakes have moved to clear the bodies (handles head to head collisions where both die).
        Iterator<Snake> snake_itr = dead_snakes.iterator();
        while (snake_itr.hasNext()) {
          Snake cur = snake_itr.next();
          cur.clearBody();
          if (notifications_enabled) {
            reportSnakeDied(cur.getId());
          }
        }
        dead_snakes.clear();

        // Handle disconnected snakes.
        human_itr = disconnected_snakes.iterator();
        while (human_itr.hasNext()) {
          HumanSnake cur = human_itr.next();
          cur.clearBody();

          if (notifications_enabled) {
            reportSnakeDisconnected(cur.getId());
          }

          if (limit_connections_per_ip) {
            String ip = cur.getIP();
            Integer count = connected_ip_counts.get(cur.getIP());
            if (count.intValue() == 1) {
              //System.out.println("removing record for ip " + ip);
              connected_ip_counts.remove(ip);
            } else {
              //System.out.println("decrementing record for ip " + ip);
              connected_ip_counts.put(ip, count.intValue() - 1);
            }
          }
        }
        disconnected_snakes.clear();

        // Send display info to each snake now that everything has been updated.
        human_itr = human_snakes.iterator();
        while (human_itr.hasNext()) {
          HumanSnake cur = human_itr.next();
          cur.sendGameData();
        }

        human_itr = human_snakes.iterator();
        while (human_itr.hasNext()) {
          HumanSnake cur = human_itr.next();
          cur.getConnection().flush();
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

    private Queue<String> notification_queue;

    public NotificationSender() {
      notification_queue = new LinkedList<String>();
    }

    public void pushNotification(String n) {
      if (notification_queue.size() == MAX_QUEUED_NOTIFICATIONS) {
        notification_queue.poll();
      }
      notification_queue.offer(n);
    }

    public void run() {
      while (true) {
        synchronized (human_snakes) {
          Iterator<HumanSnake> human_itr = human_snakes.iterator();
          while (human_itr.hasNext()) {
            HumanSnake cur = human_itr.next();
            SnakeConnection conn = cur.getConnection();
            conn.sendNumClients(getNumClients());

            if (notifications_enabled) {
              Iterator<String> notif_itr = notification_queue.iterator();
              while (notif_itr.hasNext()) {
                String s = notif_itr.next();
                conn.send(s);
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
