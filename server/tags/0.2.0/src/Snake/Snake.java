/**
 * Eugene Marinelli
 */

package Snake;

import java.util.LinkedList;
import java.util.Queue;

public class Snake {
  private Board board;

  private SnakeConnection conn;
  private Queue<Position> pos_queue;
  private Position head_pos;
  private int remaining_growth;

  public static int WINDOW_ROWS = 24;
  public static int WINDOW_COLS = 32;
  public static int WINDOW_ROW_OFFSET = 12;
  public static int WINDOW_COL_OFFSET = 16;
  private char[][] window;

  private static final int GROWTH_PER_FOOD = 1;

  // Note - colors will be generated on the client side.

  public Snake(Board board, SnakeConnection conn) {
    this.board = board;
    this.conn = conn;
    pos_queue = new LinkedList<Position>();

    head_pos = board.occupyRandomOpenPosition();
    pos_queue.offer(head_pos);

    window = new char[WINDOW_ROWS][WINDOW_COLS];

    remaining_growth = 0;
  }

  public synchronized void step(Direction step_dir) {
    Position new_head_pos = board.getOffsetPosition(step_dir, head_pos);

    // Try to get new position in game board.
    char pos_val = board.grabPosition(new_head_pos);
    switch (pos_val) {
    case Board.FREE:
      // Successfully occupied position.
      pos_queue.offer(new_head_pos);
      break;
    case Board.FOOD:
      // Occupied position, grow.
      remaining_growth += GROWTH_PER_FOOD;
      pos_queue.offer(new_head_pos);

      conn.sendGrow(GROWTH_PER_FOOD);
      break;
    case Board.OCCUPIED:
    case Board.WALL:
      // Die
      Position p;
      while ((p = pos_queue.poll()) != null) {
        board.releasePosition(p);
      }
      remaining_growth = 0;

      conn.die(); // Does not return
      return;
    }

    if (remaining_growth > 0) {
      remaining_growth--;
    } else {
      Position old_pos = pos_queue.poll();
      board.releasePosition(old_pos);
    }

    head_pos = new_head_pos;

    // Construct and send a window diff report.
    char[][] new_window = board.getWindow(new_head_pos, WINDOW_ROWS, WINDOW_COLS, WINDOW_ROW_OFFSET,
					  WINDOW_COL_OFFSET);
    char[][] buf = new char[WINDOW_ROWS][WINDOW_COLS];

    boolean changed = false;
    for (int i = 0; i < WINDOW_ROWS; i++) {
      for (int j = 0; j < WINDOW_COLS; j++) {
        if (new_window[i][j] == window[i][j]) {
          buf[i][j] = SnakeConnection.NO_DIFF;
        } else {
          buf[i][j] = new_window[i][j];
          changed = true;
        }
      }
    }

    if (changed) {
      window = new_window;
      conn.sendWindowDiff(buf);
    }
  }
}
