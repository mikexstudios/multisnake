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

public class SnakeConnection implements Runnable {
  private static final String EOF = Character.toString((char)0);
  private static final String DIE_MSG = "d";
  private static final String GROW_MSG = "g";
  private static final String ABSOLUTE_POSITION_MSG = "p";
  private static final String BOARD_SIZE_MSG = "b";
  private static final String PLAYER_JOINED_MSG = "j";
  private static final String PLAYER_DIED_MSG = "a";
  private static final String PLAYER_DISCONNECTED_MSG = "i";
  private static final String NUM_CLIENTS_MSG = "c";
  private static final char ENEMY_SNAKE_ADD_REPORT = 'e';
  private static final char SELF_SNAKE_ADD_REPORT = 's';
  private static final char SNAKE_REMOVE_REPORT = 'r';
  private static final char OBSTACLE_REPORT = 'o';
  private static final char FOOD_REPORT = 'f';

  private static final int PACKET_VALUE_OFFSET = 14; // Don't want bytes to appear to be newlines or EOF.
  private static final int ASCII_RADIX = 36;

  private Socket sock;
  private PrintWriter out;
  private HumanSnake snake;
  private Thread conn_thread;
  private boolean disconnected;
  private BufferedReader in;
  private boolean username_received;

  public SnakeConnection(Socket socket, HumanSnake snake) {
    this.sock = socket;
    this.snake = snake;
    disconnected = false;
    username_received = false;

    try {
      out = new PrintWriter(sock.getOutputStream());
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  public void setSnake(HumanSnake s) {
    snake = s;
  }

  public void setThread(Thread t) {
    conn_thread = t;
  }

  public void sendGrow(int amount) {
    send(GROW_MSG + Integer.toString(amount, ASCII_RADIX));
  }

  public void sendAbsolutePosition(Position pos) {
    String msg = ABSOLUTE_POSITION_MSG + Integer.toString(pos.getCol(), ASCII_RADIX) + ","
      + Integer.toString(pos.getRow(), ASCII_RADIX);
    send(msg);
  }

  public void sendBoardSize(int rows, int cols) {
    String size_msg = BOARD_SIZE_MSG + Integer.toString(cols, ASCII_RADIX) + "," + Integer.toString(rows, ASCII_RADIX);
    send(size_msg);
  }

  public void sendFoodReport(Position topleft, Collection<Position> food_positions) {
    sendLocationReport(topleft, food_positions, FOOD_REPORT);
  }

  public void sendObstacleReport(Position topleft, Collection<Position> obst_positions) {
    sendLocationReport(topleft, obst_positions, OBSTACLE_REPORT);
  }

  public void sendEnemySnakeAddReport(Position topleft, Collection<Position> added_positions) {
    sendLocationReport(topleft, added_positions, ENEMY_SNAKE_ADD_REPORT);
  }

  public void sendSelfSnakeAddReport(Position topleft, Collection<Position> added_positions) {
    sendLocationReport(topleft, added_positions, SELF_SNAKE_ADD_REPORT);
  }

  public void sendSnakeRemoveReport(Collection<Position> removed_positions) {
    if (removed_positions.size() != 0) {
      String msg = Character.toString(SNAKE_REMOVE_REPORT);
      Iterator<Position> itr = removed_positions.iterator();
      while (itr.hasNext()) {
        Position cur = itr.next();
        msg += Integer.toString(cur.getCol(), ASCII_RADIX) + "," + Integer.toString(cur.getRow(), ASCII_RADIX) + ";";
      }

      send(msg);
    }
  }

  public static String joinOrLeaveReportSubstring(String id, int num_clients) {
    return Integer.toString(num_clients, ASCII_RADIX) + ";" + id;
  }

  public void announcePlayerJoined(String sub_msg) {
    send(PLAYER_JOINED_MSG + sub_msg);
  }

  public void announcePlayerDisconnected(String sub_msg) {
    send(PLAYER_DISCONNECTED_MSG + sub_msg);
  }

  public void sendNumClients(int num_clients) {
    send(NUM_CLIENTS_MSG + Integer.toString(num_clients, ASCII_RADIX));
  }

  private void sendLocationReport(Position topleft, Collection<Position> positions, char type_hdr) {
    List<Byte> marked = positionsToBytes(positions);
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

      send(hdr + new String(msg));
    }
  }

  public void announcePlayerDied(String player_id) {
    send(PLAYER_DIED_MSG + player_id);
  }

  private List<Byte> positionsToBytes(Collection<Position> positions) {
    List<Byte> msg = new ArrayList(positions.size() * 2);
    
    Iterator<Position> itr = positions.iterator();
    while (itr.hasNext()) {
      Position cur = itr.next();
      msg.add((byte)cur.getCol());
      msg.add((byte)cur.getRow());
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

  public synchronized void send(String s) {
    out.println(s + EOF);
    out.flush();
  }

  public void disconnect() {
    try {
      sock.close();
    } catch (IOException ioe) {
      System.out.println("exception on sock close");
    }

    disconnected = true;
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

      if (!username_received) {
        if (first_char == 'u') {
          snake.setId(line.substring(1));
          username_received = true;
        } else {
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
            case 'n':
              step_dir = Direction.NORTH;
              break;
            case 'e':
              step_dir = Direction.EAST;
              break;
            case 's':
              step_dir = Direction.SOUTH;
              break;
            case 'w':
              step_dir = Direction.WEST;
              break;
            default:
              //System.out.println("ERROR invalid direction from client: ascii " + (int)line.charAt(0));
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
