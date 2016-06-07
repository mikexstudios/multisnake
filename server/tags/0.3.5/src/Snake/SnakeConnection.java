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

  private static final int PACKET_VALUE_OFFSET = 11; // Don't want bytes to appear to be newlines or EOF.

  private Socket sock;
  private PrintWriter out;
  private Snake snake;
  private Thread conn_thread;
  private boolean disconnected;

  public SnakeConnection(Socket s, Snake sn) {
    sock = s;
    snake = sn;
    disconnected = false;
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

  public void sendFoodReport(Position topleft, boolean[][] foodgrid) {
    sendLocationReport(topleft, foodgrid, FOOD_REPORT);
  }

  public void sendObstacleReport(Position topleft, boolean[][] obstgrid) {
    sendLocationReport(topleft, obstgrid, OBSTACLE_REPORT);
  }

  public void sendSnakeReport(Position topleft, boolean[][] enemygrid) {
    sendLocationReport(topleft, enemygrid, SNAKE_REPORT);
  }

  public void announcePlayerJoined(String id) {
    String msg = PLAYER_JOINED_MSG + id;
    send(msg);
  }

  public void announcePlayerDisconnected(String id) {
    String msg = PLAYER_DISCONNECTED_MSG + id;
    send(msg);
  }

  private void sendLocationReport(Position topleft, boolean[][] present_grid, char type_hdr) {
    ArrayList<Byte> marked = markedLocationsArray(present_grid);
    int size = marked.size();

    if (size != 0) {
      String hdr = Character.toString(type_hdr) + Integer.toString(topleft.getCol(), 16) + ","
        + Integer.toString(topleft.getRow(), 16) + ";";
      byte[] msg = new byte[size];
      for (int i = 0; i < size; i++) {
        msg[i] = (byte)(marked.get(i) + PACKET_VALUE_OFFSET);
      }

      send(hdr + (new String(msg)));
    }
  }

  private ArrayList<Byte> markedLocationsArray(boolean[][] m) {
    ArrayList<Byte> msg = new ArrayList();

    for (int i = 0; i < snake.WINDOW_ROWS; i++) {
      for (int j = 0; j < snake.WINDOW_COLS; j++) {
        if (m[i][j]) {
          msg.add((byte)j);
          msg.add((byte)i);
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

  public synchronized void send(String s) {
    //System.out.println("Sending: " + s);
    out.println(s + EOF);
    out.flush();
  }

  /* Copied from master policy server example. */
  private String read(BufferedReader in) {
    StringBuffer buffer = new StringBuffer();
    int codePoint;
    boolean zeroByteRead = false;

    try {
      do {
        codePoint = in.read();
        if (codePoint == 0) {
          zeroByteRead=true;
        } else {
          try {
            buffer.appendCodePoint(codePoint);
          } catch (IllegalArgumentException e) {
            // This generally occurs when the client suddenly disconnects.  Return empty string to signify broken
            // connection.
            return "";
          }
        }
      } while (!zeroByteRead && buffer.length() < 100);
    } catch (IOException e) {}

    return buffer.toString();
  }

  public String getClientId() {
    return sock.getInetAddress().getHostAddress();
  }

  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      out = new PrintWriter(sock.getOutputStream());

      snake.sendInitialReport();

      String line;
      while (true) {
        line = read(in);
        if (line.length() == 0) {
          break;
        }
        System.out.println("Received: " + line /* + "length="+Integer.toString(line.length()) + "charat0="+line.charAt(0) */);

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
      snake.deleteSnake();

      try {
        sock.close();
      } catch (IOException ioe) {
        System.out.println("exception on sock close");
      }
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
    }
  }
}
