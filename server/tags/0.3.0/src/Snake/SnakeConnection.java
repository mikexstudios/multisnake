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

public class SnakeConnection implements Runnable {
  private static final String EOF = Character.toString((char)0);
  private static final String DIE_MSG = "d";
  private static final String GROW_MSG = "g";
  private static final String CONTINUE_MSG = "c";
  private static final String SNAKE_MSG = "s";
  private static final String ABSOLUTE_POSITION_MSG = "p";
  private static final String OBSTACLE_REPORT = "o";
  private static final String FOOD_REPORT = "f";
  private static final String BOARD_SIZE_MSG = "b";

  private Socket sock;
  private PrintWriter out;
  private Snake snake;
  private Thread conn_thread;

  public SnakeConnection(Socket s) {
    sock = s;
  }

  public void setSnake(Snake s) {
    snake = s;
  }
  
  public void setThread(Thread t) {
    conn_thread = t;
  }

  public void sendGrow(int amount) {
    send(GROW_MSG + Integer.toString(amount));
  }

  public void sendAbsolutePosition(Position pos) {
    String msg = ABSOLUTE_POSITION_MSG + Integer.toString(pos.getCol()) + "," + Integer.toString(pos.getRow());
    send(msg);
  }
  
  public void sendBoardSize(int rows, int cols) {
    String size_msg = BOARD_SIZE_MSG + Integer.toString(cols) + "," + Integer.toString(rows);
    send(size_msg);
  }

  public void sendObstacleReport(Position topleft, boolean[][] obstgrid) {
    String marked = markedLocationsString(obstgrid);

    if (marked != "") {
      String msg = OBSTACLE_REPORT + Integer.toString(topleft.getCol()) + "," + Integer.toString(topleft.getRow())
        + ";" + marked;
      send(msg);
    }
  }

  public void sendFoodReport(Position topleft, boolean[][] foodgrid) {
    String marked = markedLocationsString(foodgrid);

    if (marked != "") {
      String msg = FOOD_REPORT + Integer.toString(topleft.getCol()) + "," + Integer.toString(topleft.getRow()) + ";"
        + marked;
      send(msg);
    }
  }

  private String markedLocationsString (boolean[][] m) {
    String msg = "";

    for (int i = 0; i < snake.WINDOW_ROWS; i++) {
      for (int j = 0; j < snake.WINDOW_COLS; j++) {
        if (m[i][j]) {
          msg += Integer.toString(j) + "," + Integer.toString(i) + ";";
        }
      }
    }

    return msg;
  }

  /**
   * Send "die" message, close socket, and kill thread.
   * @return Does not return.
   */
  public void die() {
    send(DIE_MSG);

    try {
      sock.close();
    } catch (IOException ioe) {
    }
    
    try {
      conn_thread.join();
    } catch (InterruptedException e) {
    }
  }

  // TODO optimize packet size -- bit packing etc.
  public void sendSnakeReport(Position topleft, boolean[][] enemygrid) {
    String msg = SNAKE_MSG + Integer.toString(topleft.getCol()) + "," + Integer.toString(topleft.getRow()) + ";";

    for (int i = 0; i < Snake.WINDOW_ROWS; i++) {
      for (int j = 0; j < Snake.WINDOW_COLS; j++) {
        if (enemygrid[i][j]) {
          msg += Integer.toString(j) + "," + Integer.toString(i) + ";";
        }
      }
    }

    send(msg);
  }

  public synchronized void send(String s) {
    System.out.println("Sending: " + s);
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
          buffer.appendCodePoint(codePoint);
        }
      } while (!zeroByteRead && buffer.length() < 100);
    } catch (IOException e) {}

    return buffer.toString();
  }
  
  public void run() {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      out = new PrintWriter(sock.getOutputStream());

      snake.sendInitialReport();

      String line;
      while (true) {
        line = read(in);
        if (line.length() == 0 || snake.isDead()) {
          break;
        }
        System.out.println("Received: " + line /* + "length="+Integer.toString(line.length()) + "charat0="+line.charAt(0) */);

        if (line.length() == 2) {
          Direction step_dir = null;
          char dir_char = line.charAt(0);

          switch(dir_char) {
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
