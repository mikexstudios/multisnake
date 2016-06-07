/**
 * Eugene Marinelli
 *
 */

package Snake;

import java.net.Socket;
import java.util.LinkedList;

public class Snake {
  private Board board;

  private SnakeConnection conn;
  private LinkedList<Position> pos_queue;
  private Position head_pos;
  private Direction direction;
  private LinkedList<Direction> direction_queue; // One popped per step.
  private int remaining_growth;
  private boolean initial_report_sent;
  private boolean dead;

  public static final int WINDOW_ROWS = 24; //48;
  public static final int WINDOW_COLS = 32; // 64;
  public static final int WINDOW_ROW_OFFSET = WINDOW_ROWS / 2;
  public static final int WINDOW_COL_OFFSET = WINDOW_COLS / 2;

  private static final int GROWTH_PER_FOOD = 1;
  private static final int MAX_DIR_QUEUE_SIZE = 5;

  private static boolean[][] reported_obstacle_windows;

  public Snake(Board board, Socket sock) {
    this.board = board;
    conn = new SnakeConnection(sock, this);

    // Initialize obstacle frames that have been sent.
    int vert_windows = (int)(((float)board.ROWS + 0.5) / (float)WINDOW_ROWS) + 1;
    int horiz_windows = (int)(((float)board.COLS + 0.5) / (float)WINDOW_COLS) + 1;
    reported_obstacle_windows = new boolean[vert_windows][horiz_windows];
    for (int i = 0; i < vert_windows; i++) {
      for (int j = 0; j < horiz_windows; j++) {
        reported_obstacle_windows[i][j] = false;
      }
    }

    initSnake();
  }

  public void deleteSnake() {
    Position p;
    while ((p = pos_queue.poll()) != null) {
      board.releasePosition(p);
    }
  }

  private void initSnake() {
    pos_queue = new LinkedList<Position>();

    // Snake starts with just a head.  Occupy a random initial position.
    head_pos = board.occupyRandomOpenPosition();
    pos_queue.offer(head_pos);

    remaining_growth = 0;
    initial_report_sent = false;
    direction = null;
    direction_queue = new LinkedList<Direction>();

    dead = false;
  }

  private void sendObstacleReport(Position topleft) {
    int mark_row = topleft.getRow() / WINDOW_ROWS;
    int mark_col = topleft.getCol() / WINDOW_COLS;

    if (!reported_obstacle_windows[mark_row][mark_col]) {
      conn.sendObstacleReport(topleft, getBoolGridOfType(topleft, Board.WALL));
      reported_obstacle_windows[mark_row][mark_col] = true;
    }
  }

  private void sendFoodReport(Position topleft) {
    conn.sendFoodReport(topleft, getBoolGridOfType(topleft, Board.FOOD));
  }

  public synchronized boolean getInitialReportSent() {
    return initial_report_sent;
  }

  public synchronized void sendInitialReport() {
    conn.sendBoardSize(board.ROWS, board.COLS);
    conn.sendAbsolutePosition(head_pos);

    Position topleft = getContainingWindowTopLeft(head_pos);
    sendFoodReport(topleft);
    sendObstacleReport(topleft);

    Position tmp;
    tmp = board.getOffsetPosition(topleft, WINDOW_ROWS, -WINDOW_COLS);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, 0, -WINDOW_COLS);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, -WINDOW_ROWS, -WINDOW_COLS);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, -WINDOW_ROWS, 0);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, WINDOW_ROWS, 0);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, WINDOW_ROWS, WINDOW_COLS);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, 0, WINDOW_COLS);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    tmp = board.getOffsetPosition(topleft, -WINDOW_ROWS, WINDOW_COLS);
    sendFoodReport(tmp);
    sendObstacleReport(tmp);

    sendEnemyLocations();

    initial_report_sent = true;
  }

  private Position getContainingWindowTopLeft(Position pos) {
    return new Position((pos.getRow() / WINDOW_ROWS) * WINDOW_ROWS, (pos.getCol() / WINDOW_COLS) * WINDOW_COLS);
  }

  private boolean[][] getBoolGridOfType(Position topleft, int type) {
    char[][] window = board.getTopLeftWindow(topleft, WINDOW_ROWS, WINDOW_COLS);
    boolean[][] grid = new boolean[WINDOW_ROWS][WINDOW_COLS];
    for (int i = 0; i < WINDOW_ROWS; i++) {
      for (int j = 0; j < WINDOW_COLS; j++) {
        if (window[i][j] == type) {
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
    if (direction_queue.size() < MAX_DIR_QUEUE_SIZE) {
      direction_queue.offer(dir);
    }
  }

  public synchronized boolean isDead() {
    return dead;
  }

  public synchronized void reset() {
    // Only reset if the snake has died.
    if (dead) {
      initSnake();
      sendInitialReport();
    }
  }

  private synchronized Direction getNextDirection() {
    Direction d = null;
    int snake_len = pos_queue.size();

    Direction opposite = null;
    if (direction != null) {
      opposite = direction.getOpposite();
    }

    do {
      d = direction_queue.poll();
    } while (d != null && ((snake_len > 1 && d == opposite) || d == direction));

    if (d != null) {
      direction = d;
    }

    return direction;
  }

  public synchronized void step() {
    Direction dir = getNextDirection();

    if (dir == null || !getInitialReportSent() || isDead()) {
      System.out.println("NOT STEPPING; direction=" + dir + " dead=" + dead + " initialreportsent="
        + initial_report_sent);
      return;
    }

    Position new_head_pos = board.getOffsetPosition(dir, head_pos);

    // Try to get new position in game board.  React to the previous value of that position.
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
      // Ran into wall or snake body -> DIE.
      deleteSnake();

      dead = true;
      conn.die();
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
    Position a = null, b = null, c = null;

    // Send update if new head position is in a new frame.
    if (!inSameFrame(head_pos, new_head_pos)) {
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
      } else if (dir == Direction.WEST) {
        a = board.getOffsetPosition(topleft, -WINDOW_ROWS, -WINDOW_COLS);
        b = board.getOffsetPosition(topleft, 0, -WINDOW_COLS);
        c = board.getOffsetPosition(topleft, WINDOW_ROWS, -WINDOW_COLS);
      } else {
        System.out.println("ERROR - Invalid direction");
      }

      sendFoodReport(a);
      sendFoodReport(b);
      sendFoodReport(c);
      sendObstacleReport(a);
      sendObstacleReport(b);
      sendObstacleReport(c);
    }

    head_pos = new_head_pos;

    conn.sendAbsolutePosition(head_pos);
    sendEnemyLocations();
  }

  public SnakeConnection getConnection() {
    return conn;
  }
}
