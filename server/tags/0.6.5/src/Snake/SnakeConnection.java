/**
 * Snake Task
 *
 * Eugene Marinelli
 * 12/15/07
 *
 * Interface between server and client.  Receives info from, packages and sends data to client.
 */

package Snake;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.SortedSet;

public class SnakeConnection implements Runnable {
  /* Packet header characters. */
  private static final String EOF = Character.toString((char)0);
  private static final String DIE_MSG = "d";
  private static final String FOOD_MSG = "g";
  private static final String ABSOLUTE_POSITION_MSG = "p";
  private static final String BOARD_SIZE_MSG = "b";

  private static final String PLAYER_JOINED_MSG = "j";
  private static final String PLAYER_DIED_MSG = "a";
  private static final String PLAYER_DISCONNECTED_MSG = "i";
  private static final String PLAYER_KILLED_MSG = "q";

  private static final String NUM_CLIENTS_MSG = "c";
  private static final String TICK = "t";
  private static final char ENEMY_SNAKE_ADD_REPORT = 'e';
  private static final char SELF_SNAKE_ADD_REPORT = 's';
  private static final String ENEMY_SNAKE_REMOVE_REPORT = "m";
  private static final String SELF_SNAKE_REMOVE_REPORT = "r";
  private static final char OBSTACLE_REPORT = 'o';
  private static final char FOOD_REPORT = 'f';
  private static final String KILL_MSG = "k";
  private static final String KILLED_BY_MSG = "l";

  private static final String ROUND_END_MSG = "h";
  private static final String ROUND_BEGIN_MSG = "n";
  private static final String ROUND_STATUS_MSG = "u";
  private static final String GAME_MODE_STR = "g";
  private static final String INTERMISSION_MODE_STR = "i";

  private static final int NUM_RANKED_SNAKES_TO_SEND = 4;

  private static final int PACKET_VALUE_OFFSET = 14; // Don't want bytes to appear to be newlines or EOF.
  private static final int ASCII_RADIX = 36;
  private static final int MAX_USERNAME_LENGTH = 14;
  private static final String USERNAME_PATTERN = "\\w*";

  //private static final int SENDS_BETWEEN_BYTE_COUNT_OUTPUT = 10000;

  private Socket sock;
  private PrintWriter out;
  private HumanSnake snake;
  private Thread conn_thread;
  private boolean disconnected;
  private BufferedReader in;
  private boolean userinfo_received;

  // Testing
  private static int obstacle_bytes, position_bytes, tick_bytes, self_bytes, enemy_bytes, grow_bytes, food_bytes,
    num_clients_bytes, enemy_remove_bytes, self_remove_bytes;
  private static int send_count;

  public SnakeConnection(Socket socket, HumanSnake snake) {
    this.sock = socket;
    this.snake = snake;
    disconnected = false;
    userinfo_received = false;

    try {
      out = new PrintWriter(sock.getOutputStream());
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  public static void printByteCounts() {
    System.out.println("obst:" + obstacle_bytes + " food:" + food_bytes + " pos:" + position_bytes
      + " tick:" + tick_bytes + " self:" + self_bytes + " enemy:" + enemy_bytes + " grow:" + grow_bytes
      + " nclients:" + num_clients_bytes + " enemy_remove:" + enemy_remove_bytes + " self_remove:" + self_remove_bytes);
  }

  public void reportKill(String other_id) {
    send(KILL_MSG + other_id);
  }

  public void reportKilledBy(String other_id) {
    send(KILLED_BY_MSG + other_id);
  }

  public static String getRoundEndString(int intermission_duration, SortedSet<Snake> ranked_snakes) {
    String msg = Integer.toString(intermission_duration, ASCII_RADIX);

    if (ranked_snakes.size() == 0) {
      return msg;
    }

    msg += ";";

    int snakes_listed = 0;

    Iterator<Snake> itr = ranked_snakes.iterator();
    Snake first = itr.next();
    msg += first.getId() + "," + Integer.toString(first.getScore(), ASCII_RADIX);

    while (itr.hasNext() && snakes_listed < NUM_RANKED_SNAKES_TO_SEND-1) {
      Snake cur = itr.next();
      msg += ";" + cur.getId() + "," + Integer.toString(cur.getScore(), ASCII_RADIX);
      snakes_listed++;
    }

    return msg;
  }

  public void reportRoundEnd(String common_round_end_str, int rank) {
    send(ROUND_END_MSG + Integer.toString(rank, ASCII_RADIX) + ";" + common_round_end_str);
  }

  public static String getRoundBeginString(int round_duration) {
    return ROUND_BEGIN_MSG + Integer.toString(round_duration, ASCII_RADIX);
  }

  public void sendRoundStatus(SnakeServer.GameMode mode, long time_remaining) {
    String mode_str = mode == SnakeServer.GameMode.INTERMISSION_MODE ? INTERMISSION_MODE_STR : GAME_MODE_STR;
    send(ROUND_STATUS_MSG + mode_str + Long.toString(time_remaining, ASCII_RADIX));
  }

  public void setSnake(HumanSnake s) {
    snake = s;
  }

  public void setThread(Thread t) {
    conn_thread = t;
  }

  public void sendFood() {
    grow_bytes += send(FOOD_MSG);
  }

  public void sendAbsolutePosition(Position pos) {
    String msg = ABSOLUTE_POSITION_MSG + Integer.toString(pos.getCol(), ASCII_RADIX) + ","
      + Integer.toString(pos.getRow(), ASCII_RADIX);
    position_bytes += send(msg);
  }

  public void sendBoardSize(int rows, int cols) {
    String size_msg = BOARD_SIZE_MSG + Integer.toString(cols, ASCII_RADIX) + "," + Integer.toString(rows, ASCII_RADIX);
    send(size_msg);
  }

  public void sendFoodReport(Position topleft, Collection<Position> food_positions) {
    food_bytes += sendLocationReport(topleft, food_positions, FOOD_REPORT, false);
  }

  public static String getKillNotificationString(String killer, String victim) {
    return PLAYER_KILLED_MSG + killer + ";" + victim;
  }

  public void sendObstacleReport(Position topleft, Collection<Position> obst_positions) {
    obstacle_bytes += sendLocationReport(topleft, obst_positions, OBSTACLE_REPORT, false);
  }

  public void sendEnemySnakeAddReport(Position topleft, Collection<? extends Position> added_positions) {
    enemy_bytes += sendLocationReport(topleft, added_positions, ENEMY_SNAKE_ADD_REPORT, true);
  }

  public void sendSelfSnakeAddReport(Position topleft, Collection<? extends Position> added_positions) {
    self_bytes += sendLocationReport(topleft, added_positions, SELF_SNAKE_ADD_REPORT, false);
  }

  public void sendEnemySnakeRemoveReport(Collection<? extends Position> removed_positions) {
    enemy_remove_bytes += sendSnakeRemoveReport(ENEMY_SNAKE_REMOVE_REPORT, removed_positions);
  }

  public void sendSelfSnakeRemoveReport(Collection<? extends Position> removed_positions) {
    self_remove_bytes += sendSnakeRemoveReport(SELF_SNAKE_REMOVE_REPORT, removed_positions);
  }

  public int sendSnakeRemoveReport(String hdr, Collection<? extends Position> removed_positions) {
    if (removed_positions.size() != 0) {
      String msg = hdr;

      Iterator<? extends Position> itr = removed_positions.iterator();
      // Manually handle the first to avoid extra semicolon at the end.
      Position first = itr.next(); // Already made sure that there is at least one removed position.
      msg += Integer.toString(first.getCol(), ASCII_RADIX) + "," + Integer.toString(first.getRow(), ASCII_RADIX);
      while (itr.hasNext()) {
        Position cur = itr.next();
        msg += ";" + Integer.toString(cur.getCol(), ASCII_RADIX) + "," + Integer.toString(cur.getRow(), ASCII_RADIX);
      }

      return send(msg);
    } else {
      return 0;
    }
  }

  public static String getJoinNotificationString(String id) {
    return PLAYER_JOINED_MSG + id;
  }

  public static String getLeaveNotificationString(String id) {
    return PLAYER_DISCONNECTED_MSG + id;
  }

  public static String getDeathNotificationString(String id) {
    return PLAYER_DIED_MSG + id;
  }

  public void sendNumClients(int num_clients) {
    num_clients_bytes += send(NUM_CLIENTS_MSG + Integer.toString(num_clients, ASCII_RADIX));
  }

  private int sendLocationReport(Position topleft, Collection<? extends Position> positions, char type_hdr,
    boolean colored) {
    List<Byte> marked = positionsToBytes(positions, colored);
    int size = marked.size();

    if (size != 0) {
      String hdr = Character.toString(type_hdr) + Integer.toString(topleft.getCol(), ASCII_RADIX) + ","
        + Integer.toString(topleft.getRow(), ASCII_RADIX) + ";";
      byte[] msg = new byte[size];
      for (int i = 0; i < size; i++) {
        msg[i] = (byte)(marked.get(i) + PACKET_VALUE_OFFSET);
      }

///*
//      System.out.print("location report: "+ hdr);
//      for (int i = 0; i < msg.length; i++) {
//        System.out.print((msg[i]-PACKET_VALUE_OFFSET) + ",");
//        if (msg[i] - PACKET_VALUE_OFFSET < 0) {
//          System.out.println("neg" + (msg[i] - PACKET_VALUE_OFFSET));
//        }
//      }
//      System.out.println();
//*/

      return send(hdr + new String(msg));
    } else {
      return 0;
    }
  }

  /** This can be used instead of "absolute position" messages in order to signify a game tick. */
  public void sendTick() {
    tick_bytes += send(TICK);
  }

  private List<Byte> positionsToBytes(Collection<? extends Position> positions, boolean colored) {
    List<Byte> msg = new ArrayList<Byte>(positions.size() * 3);

    for (Position cur : positions) {
      msg.add((byte)cur.getCol());
      msg.add((byte)cur.getRow());
      if (colored) {
        SnakeColor c = ((ColoredPosition)cur).getColor();
        if (c == null) {
          msg.add((byte)SnakeColor.BLUE.getId());
        } else {
          msg.add((byte)c.getId());
        }
      }
    }

    return msg;
  }

  public boolean disconnected() {
    return disconnected;
  }

  /**
   * Send "die" message, close socket, and kill thread.
   * @return Does not return.
   */
  public void die() {
    send(DIE_MSG);
  }

  public synchronized int send(String s) {
    //assert(!disconnected);
    if (disconnected) {
      //System.out.println("ERROR -- SENDING ON DISCONNECTED SOCKET");
      return 0;
    }

    out.print(s + EOF);
    // Flushing is done separately.

    /*
    send_count++;
    if (send_count % SENDS_BETWEEN_BYTE_COUNT_OUTPUT == 0) {
      printByteCounts();
    }*/

    return s.length() + 1;
  }

  public synchronized void flush() {
    if (disconnected) {
      //System.out.println("ERROR - FLUSHING DISCONNECTED SOCKET.");
    } else {
      out.flush();
    }
  }

  public void disconnect() {
    if (disconnected) {
      return;
    }

    try {
      sock.close();
    } catch (IOException ioe) {
      System.out.println("exception on sock close");
    }

    disconnected = true;

    // Make sure this connection isn't used again.
    sock = null;
    out = null;
    in = null;
    //snake = null;  breaks stuff...
  }

  public void run() {
    while (true) {
      String line = ConnectionUtils.read(in);
      int length = line.length();
      if (length == 0) {
        snake.clearBody();
        disconnect();
        return;
      }

      char first_char = line.charAt(0);

      if (!userinfo_received) {
        if (first_char == 'u') {
          String[] info = line.trim().substring(1, length).split(",");
          if (info.length != 2) {
            snake.clearBody();
            disconnect();
            return;
          }

          int username_len = info[0].length() > MAX_USERNAME_LENGTH ? MAX_USERNAME_LENGTH : info[0].length();
          String username = info[0];
          if (Pattern.matches(USERNAME_PATTERN, username)) {
            snake.setId(username);
            userinfo_received = true;
          } else {
            System.out.println("Invalid username: " + username);
            snake.clearBody();
            disconnect();
            return;
          }

          int color_id;
          try {
            color_id = Integer.parseInt(info[1], 16);
          } catch (Exception e) {
            System.out.println("Parseint failed for " + info[1]);
            snake.clearBody();
            disconnect();
            return;
          }

          SnakeColor c = SnakeColor.fromInt(color_id);
          if (c != null) {
            snake.setColor(c);
          } else {
            System.out.println("Invalid color: " + info[1]);
            snake.clearBody();
            disconnect();
            return;
          }
        } else {
          /* Invalid first message. */
          snake.clearBody();
          disconnect();
          return;
        }
      } else {
        if (line.length() == 2) { // Input line length must be 2.
          if (first_char == 'r') {
            snake.setReadyForReset();
          } else {
            Direction step_dir = null;

            switch(first_char) {
              case 'n': step_dir = Direction.NORTH; break;
              case 'e': step_dir = Direction.EAST; break;
              case 's': step_dir = Direction.SOUTH; break;
              case 'w': step_dir = Direction.WEST; break;
              default:
              // Received an invalid character.  This is obviously not our client program.
                snake.clearBody();
                disconnect();
                return;
            }

            if (step_dir != null) {
              snake.setDirection(step_dir);
            }
          }
        } else {
          snake.clearBody();
          disconnect();
          return;
        }
      }
    }
  }
}
