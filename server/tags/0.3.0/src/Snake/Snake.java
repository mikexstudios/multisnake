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
  private Direction direction;
  private int remaining_growth;
  private boolean initial_report_sent;
  private boolean dead;

  public static int WINDOW_ROWS = 24;
  public static int WINDOW_COLS = 32;
  public static int WINDOW_ROW_OFFSET = 12;
  public static int WINDOW_COL_OFFSET = 16;

  private static final int GROWTH_PER_FOOD = 1;

  private static boolean[][] reported_obstacle_windows;

  public Snake(Board board, SnakeConnection conn) {
    this.board = board;
    this.conn = conn;
    pos_queue = new LinkedList<Position>();

    int vert_windows = (int)(((float)board.ROWS + 0.5) / (float)WINDOW_ROWS) + 1;
    int horiz_windows = (int)(((float)board.COLS + 0.5) / (float)WINDOW_COLS) + 1;

    reported_obstacle_windows = new boolean[vert_windows][horiz_windows];
    for (int i = 0; i < vert_windows; i++) {
      for (int j = 0; j < horiz_windows; j++) {
        reported_obstacle_windows[i][j] = false;
      }
    }

    head_pos = board.occupyRandomOpenPosition();
    pos_queue.offer(head_pos);

    remaining_growth = 0;
    initial_report_sent = false;
    direction = null;
    dead = false;
  }

  private void sendObstacleReport(Position topleft) {
    int mark_row = topleft.getRow() / WINDOW_ROWS;
    int mark_col = topleft.getCol() / WINDOW_COLS;

    if (!reported_obstacle_windows[mark_row][mark_col]) {
      conn.sendObstacleReport(topleft, getObstacleGrid(topleft));
      reported_obstacle_windows[mark_row][mark_col] = true;
    }
  }

  public synchronized boolean getInitialReportSent() {
    return initial_report_sent;
  }

  public synchronized boolean isDead() {
    return dead;
  }

  public synchronized void sendInitialReport() {
    // Send initial info.
    conn.sendBoardSize(board.ROWS, board.COLS);
    conn.sendAbsolutePosition(head_pos);

    Position tmp;
    Position topleft = getContainingWindowTopLeft(head_pos);
    conn.sendFoodReport(topleft, getFoodGrid(topleft));
    sendObstacleReport(topleft);

    tmp = board.getOffsetPosition(topleft, WINDOW_ROWS, -WINDOW_COLS);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, 0, -WINDOW_COLS);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, -WINDOW_ROWS, -WINDOW_COLS);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, -WINDOW_ROWS, 0);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, WINDOW_ROWS, 0);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, WINDOW_ROWS, WINDOW_COLS);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, 0, WINDOW_COLS);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, -WINDOW_ROWS, WINDOW_COLS);
    conn.sendFoodReport(tmp, getFoodGrid(tmp));
    sendObstacleReport(tmp);

    sendEnemyLocations();

    initial_report_sent = true;
  }

  private Position getContainingWindowTopLeft(Position pos) {
    return new Position((pos.getRow() / WINDOW_ROWS) * WINDOW_ROWS, (pos.getCol() / WINDOW_COLS) * WINDOW_COLS);
  }

  private boolean[][] getFoodGrid(Position topleft) {
    char[][] window = board.getTopLeftWindow(topleft, WINDOW_ROWS, WINDOW_COLS);
    boolean[][] grid = new boolean[WINDOW_ROWS][WINDOW_COLS];
    for (int i = 0; i < WINDOW_ROWS; i++) {
      for (int j = 0; j < WINDOW_COLS; j++) {
        if (window[i][j] == Board.FOOD) {
          grid[i][j] = true;
        }
      }
    }

    return grid;
  }

  private boolean[][] getObstacleGrid(Position topleft) {
    char[][] window = board.getTopLeftWindow(topleft, WINDOW_ROWS, WINDOW_COLS);

    boolean[][] grid = new boolean[WINDOW_ROWS][WINDOW_COLS];
    for (int i = 0; i < WINDOW_ROWS; i++) {
      for (int j = 0; j < WINDOW_COLS; j++) {
        if (window[i][j] == Board.WALL) {
          grid[i][j] = true;
        }
      }
    }

    return grid;
  }

  public boolean inSameFrame(Position a, Position b) {
    return getContainingWindowTopLeft(a).equals(getContainingWindowTopLeft(b));
  }

  private void sendEnemyLocations() {
    char[][] window = board.getCenterWindow(head_pos, WINDOW_ROWS, WINDOW_COLS, WINDOW_ROW_OFFSET,
      WINDOW_COL_OFFSET);
    boolean[][] buf = new boolean[WINDOW_ROWS][WINDOW_COLS];

    boolean enemies = false;
    for (int i = 0; i < WINDOW_ROWS; i++) {
      for (int j = 0; j < WINDOW_COLS; j++) {
        if (window[i][j] == Board.OCCUPIED) {
          buf[i][j] = true;
          enemies = true;
        } else {
          buf[i][j] = false;
        }
      }
    }
    if (enemies) {
      conn.sendSnakeReport(board.getOffsetPosition(head_pos, -WINDOW_ROW_OFFSET, -WINDOW_COL_OFFSET), buf);
    }
  }
  
  public synchronized void setDirection(Direction dir) {
    direction = dir;
  }

  private synchronized Direction getDirection() {
    return direction;
  }

  public synchronized void step() {
    Direction dir = getDirection();

    if (dir == null || !getInitialReportSent() || isDead()) {
      System.out.println("NOT STEPPING; direction=" + dir + " dead=" + dead);
      return;
    }

    Position new_head_pos = board.getOffsetPosition(dir, head_pos);

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
      // DIE
      Position p;
      while ((p = pos_queue.poll()) != null) {
        board.releasePosition(p);
      }
      remaining_growth = 0;

      dead = true;
      conn.die(); // Does not return
      return;
    }

    if (remaining_growth > 0) {
      remaining_growth--;
    } else {
      Position old_pos = pos_queue.poll();
      board.releasePosition(old_pos);
    }

    // If crossing a window boundary, send a report updating the client with food an obstacle positions.
    Position topleft = getContainingWindowTopLeft(new_head_pos);
    Position a, b, c;

    // Send update if new head position is in a new frame.
    if (!inSameFrame(head_pos, new_head_pos)) {
      System.out.println("HEAD IN NEW FRAME (" + head_pos + "," + new_head_pos + ")");
      if (dir == Direction.NORTH) {
        a = board.getOffsetPosition(topleft, -WINDOW_ROWS, -WINDOW_COLS);
        b = board.getOffsetPosition(topleft, -WINDOW_ROWS, 0);
        c = board.getOffsetPosition(topleft, -WINDOW_ROWS, WINDOW_COLS);
      } else if (dir == Direction.SOUTH) {
        a = board.getOffsetPosition(topleft, WINDOW_ROWS, -WINDOW_COLS);
        b = board.getOffsetPosition(topleft, WINDOW_ROWS, 0);
        c = board.getOffsetPosition(topleft, WINDOW_ROWS, WINDOW_COLS);
      } else if (dir == Direction.EAST) {
        a = board.getOffsetPosition(topleft, -WINDOW_ROWS, WINDOW_COLS);
        b = board.getOffsetPosition(topleft, 0, WINDOW_COLS);
        c = board.getOffsetPosition(topleft, WINDOW_ROWS, WINDOW_COLS);
      } else {
        a = board.getOffsetPosition(topleft, -WINDOW_ROWS, -WINDOW_COLS);
        b = board.getOffsetPosition(topleft, 0, -WINDOW_COLS);
        c = board.getOffsetPosition(topleft, WINDOW_ROWS, -WINDOW_COLS);
      }

      conn.sendFoodReport(a, getFoodGrid(a));
      conn.sendFoodReport(b, getFoodGrid(b));
      conn.sendFoodReport(c, getFoodGrid(c));
      sendObstacleReport(a);
      sendObstacleReport(b);
      sendObstacleReport(c);
    }

    head_pos = new_head_pos;

    conn.sendAbsolutePosition(head_pos);
    sendEnemyLocations();
  }
}
