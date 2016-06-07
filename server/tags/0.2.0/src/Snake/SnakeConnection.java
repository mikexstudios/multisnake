/**
 * Snake Task
 *
 * Eugene Marinelli
 * 12/15/07
 *
 * Based on http://www.kieser.net/linux/java_server.html
 */

package Snake;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class SnakeConnection implements Runnable {
  private static final String EOF = Character.toString((char)0);
  private static final String DIE_MSG = "d";
  private static final String GROW_MSG = "g";
  private static final String CONTINUE_MSG = "c";
  private static final String WINDOW_DIFF_MSG = "w";

  public static final char NO_DIFF = 's';

  private Socket sock;
  private PrintWriter out;
  private Snake snake;

  public SnakeConnection(Socket s) {
    sock = s;
  }

  public void setSnake(Snake s) {
    snake = s;
  }

  public void sendGrow(int amount) {
    send(GROW_MSG + Integer.toString(amount));
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
      Thread.currentThread().join();
    } catch (InterruptedException ie) {
    }
  }

  // TODO optimize packet size -- bit packing etc.
  public void sendWindowDiff(char[][] diffgrid) {
    String msg = WINDOW_DIFF_MSG;

    for (int i = 0; i < Snake.WINDOW_ROWS; i++) {
      for (int j = 0; j < Snake.WINDOW_COLS; j++) {
        if (diffgrid[i][j] != NO_DIFF) {
          msg += Integer.toString(j) + "," + Integer.toString(i) + "," + Character.toString(diffgrid[i][j]) + ";";
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

  public void run() {
    try {
      DataInputStream in = new DataInputStream(sock.getInputStream());
      out = new PrintWriter(sock.getOutputStream());

      String line;
      while ((line = in.readLine()) != null && !line.equals(EOF)) {
        System.out.println("Received: " + line);

        if (line.equals("<policy-file-request/>")) {
          send("<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"10123\" /></cross-domain-policy>");
        } else if (line == null || line.length() > 2) {
          System.out.println("ERROR received null or invalid sized string from client (length=" + line.length()
                             + ", value is " + line + ").");
          System.out.println("Ascii: ");
          for (int i = 0; i < line.length(); i++) {
            System.out.println((int)line.charAt(i));
          }
        } else {
          // Client reports direction, then waits for server to take next step.
          Direction step_dir = null;
          char dir_char = line.charAt(0);
          if ((int)dir_char == 0) {
            dir_char = line.charAt(1);
          }

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
            snake.step(step_dir);
            send(CONTINUE_MSG);
          }
        }
      }

      die();
    } catch (IOException ioe) {
      System.out.println("IOException on socket listen: " + ioe);
      ioe.printStackTrace();
      die();
    }
  }
}
