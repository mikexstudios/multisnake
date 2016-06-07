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
import java.util.List;
import java.util.ListIterator;

public class SnakeConnection implements Runnable {
  private static final String EOF = Character.toString((char)0);
  private static final String DIE_MSG = "d";
  private static final String GROW_MSG = "g";
  private static final String ABSOLUTE_POSITION_MSG = "p";
  private static final String BOARD_SIZE_MSG = "b";
  private static final String PLAYER_JOINED_MSG = "j";
  private static final String PLAYER_DISCONNECTED_MSG = "i";
  private static final char SNAKE_REPORT = 's';
  private static final char OBSTACLE_REPORT = 'o';
  private static final char FOOD_REPORT = 'f';

  private static final int PACKET_VALUE_OFFSET = 14; // Don't want bytes to appear to be newlines or EOF.

  private Socket sock;
  private PrintWriter out;
  private Snake snake;
  private Thread conn_thread;
  private boolean disconnected;
  private BufferedReader in;

  public SnakeConnection(Socket s, Snake sn) {
    sock = s;
    snake = sn;
    disconnected = false;

    try {
      out = new PrintWriter(sock.getOutputStream());
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }

  public void setSnake(Snake s) {
    snake = s;
  }

  public void setThread(Thread t) {
    conn_thread = t;
  }

  public void sendGrow(int amount) {
    send(GROW_MSG + Integer.toString(amount, 16));
  }

  public void sendAbsolutePosition(Position pos) {
    String msg = ABSOLUTE_POSITION_MSG + Integer.toString(pos.getCol(), 16) + "," + Integer.toString(pos.getRow(), 16);
    send(msg);
  }

  public void sendBoardSize(int rows, int cols) {
    String size_msg = BOARD_SIZE_MSG + Integer.toString(cols, 16) + "," + Integer.toString(rows, 16);
    send(size_msg);
  }

  public void sendFoodReport(Position topleft, List<Position> food_positions) {
    sendLocationReport(topleft, food_positions, FOOD_REPORT);
  }

  public void sendObstacleReport(Position topleft, List<Position> obst_positions) {
    sendLocationReport(topleft, obst_positions, OBSTACLE_REPORT);
  }

  public void sendSnakeReport(Position topleft, List<Position> enemy_positions) {
    sendLocationReport(topleft, enemy_positions, SNAKE_REPORT);
  }

  public void announcePlayerJoined(String id) {
    String msg = PLAYER_JOINED_MSG + id;
    send(msg);
  }

  public void announcePlayerDisconnected(String id) {
    String msg = PLAYER_DISCONNECTED_MSG + id;
    send(msg);
  }

  private void sendLocationReport(Position topleft, List<Position> positions, char type_hdr) {
    List<Byte> marked = positionsToBytes(positions);
    int size = marked.size();

    if (size != 0) {
      String hdr = Character.toString(type_hdr) + Integer.toString(topleft.getCol(), 16) + ","
        + Integer.toString(topleft.getRow(), 16) + ";";
      byte[] msg = new byte[size];
      for (int i = 0; i < size; i++) {
        msg[i] = (byte)(marked.get(i) + PACKET_VALUE_OFFSET);
      }

/*
      System.out.print("location report: "+ hdr);
      for (int i = 0; i < msg.length; i++) {
        System.out.print((msg[i]-PACKET_VALUE_OFFSET) + ",");
      }
      System.out.println();
*/

      send(hdr + new String(msg));
    }
  }

  private List<Byte> positionsToBytes(List<Position> positions) {
    List<Byte> msg = new ArrayList(positions.size() * 2);
    
    ListIterator<Position> itr = positions.listIterator(0);
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

  public String getClientId() {
    return sock.getInetAddress().getHostAddress();
  }

  public void run() {
    while (true) {
      String line = ConnectionUtils.read(in);
      if (line.length() == 0) {
        break;
      }
      //System.out.println("recv " + line /* + "length="+Integer.toString(line.length()) + "charat0="+line.charAt(0) */);

      if (line.length() == 2) {
        Direction step_dir = null;
        char dir_char = line.charAt(0);

        switch(dir_char) {
        case 'r':
          snake.reset();
          break;
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
          System.out.println("ERROR invalid direction from client: ascii " + (int)line.charAt(0));
          break;
        }

        if (step_dir != null) {
          snake.setDirection(step_dir);
        }
      }
    }

    disconnected = true;
    snake.clearBody();

    try {
      sock.close();
    } catch (IOException ioe) {
      System.out.println("exception on sock close");
    }
  }
}
