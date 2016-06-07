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
  private boolean[][] reported_obstacle_windows;
  private int[][] reported_food_window_timestamps;
  private boolean board_size_sent;

  public static final int WINDOW_ROW_OFFSET = Board.WINDOW_ROWS / 2;
  public static final int WINDOW_COL_OFFSET = Board.WINDOW_COLS / 2;

  private static final int GROWTH_PER_FOOD = 1;
  private static final int MAX_DIR_QUEUE_SIZE = 3;

  public Snake(Board board, Socket sock) {
    this.board = board;

    // Initialize obstacle frames that have been sent.
    int vwindows = board.getNumVerticalWindows();
    int hwindows = board.getNumHorizontalWindows();
    reported_obstacle_windows = new boolean[vwindows][hwindows];
    for (int i = 0; i < vwindows; i++) {
      for (int j = 0; j < hwindows; j++) {
        reported_obstacle_windows[i][j] = false;
      }
    }

    reported_food_window_timestamps = new int[vwindows][hwindows];
    for (int i = 0; i < vwindows; i++) {
      for (int j = 0; j < hwindows; j++) {
        reported_food_window_timestamps[i][j] = 0;
      }
    }

    direction = null;
    initial_report_sent = false;
    pos_queue = new LinkedList<Position>();
    direction_queue = new LinkedList<Direction>();
    dead = true; // Snake considered dead until initial restart is sent.
    board_size_sent = false;

    conn = new SnakeConnection(sock, this);
  }

  public synchronized void clearBody() {
    Position p;
    while ((p = pos_queue.poll()) != null) {
      board.releasePosition(p);
    }
  }

  private void resetSnake() {
    clearBody();  // Need to do this to eliminate race condition of snake reset before snake body cleared.

    // Snake starts with just a head.  Occupy a random initial position.
    head_pos = board.occupyRandomOpenPosition();
    pos_queue.offer(head_pos);

    remaining_growth = 0;
    initial_report_sent = false;
    direction = null;
    direction_queue.clear();

    dead = false;
  }

  private void sendObstacleReport(Position topleft) {
    if (!board.isValidPosition(topleft)) {
      // topleft is not in the board.
      return;
    }

    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS;

    if (!reported_obstacle_windows[mark_row][mark_col]) {
      conn.sendObstacleReport(topleft, board.getPositionsInWindow(topleft, Board.WALL));
      reported_obstacle_windows[mark_row][mark_col] = true;
    }
  }

  private void sendFoodReport(Position topleft) {
    if (board.isValidPosition(topleft)) {
      int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
      int mark_col = topleft.getCol() / Board.WINDOW_COLS;

      int board_time = board.getFoodWindowTimestamp(topleft);
      if (board_time != reported_food_window_timestamps[mark_row][mark_col]) {
        conn.sendFoodReport(topleft, board.getPositionsInWindow(topleft, Board.FOOD));
        reported_food_window_timestamps[mark_row][mark_col] = board_time;
      }
    }
  }

  public synchronized boolean getInitialReportSent() {
    return initial_report_sent;
  }

  public synchronized void sendInitialReport() {
    if (!board_size_sent) {
      conn.sendBoardSize(board.getHeight(), board.getWidth());
      board_size_sent = true;
    }
    conn.sendAbsolutePosition(head_pos);

    Position topleft = Board.getContainingWindowTopLeft(head_pos);
    if (board.isValidPosition(topleft)) {
      sendFoodReport(topleft);
      sendObstacleReport(topleft);
    }

    Position tmp;
    tmp = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, -Board.WINDOW_COLS);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, 0, -Board.WINDOW_COLS);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, -Board.WINDOW_COLS);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, 0);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, 0);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, Board.WINDOW_COLS);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, 0, Board.WINDOW_COLS);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    tmp = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, Board.WINDOW_COLS);
    if (board.isValidPosition(tmp)) {
      sendFoodReport(tmp);
      sendObstacleReport(tmp);
    }

    sendEnemyLocations();
    initial_report_sent = true;
  }

  public boolean inSameFrame(Position a, Position b) {
    return Board.getContainingWindowTopLeft(a).equals(Board.getContainingWindowTopLeft(b));
  }

  private void sendEnemyLocations() {
    Position topleft = board.getOffsetPosition(head_pos, -WINDOW_ROW_OFFSET, -WINDOW_COL_OFFSET);
    conn.sendSnakeReport(topleft, board.getPositionsInWindow(topleft, Board.OCCUPIED));
  }

  public synchronized void setDirection(Direction dir) {
    if (direction_queue.size() < MAX_DIR_QUEUE_SIZE) {
      direction_queue.offer(dir);
    }
  }

  public SnakeConnection getConnection() {
    return conn;
  }

  public synchronized boolean isDead() {
    return dead;
  }

  public synchronized void reset() {
    // Only reset if the snake has died.
    if (dead) {
      resetSnake();
      sendInitialReport();
    }
  }

  private synchronized Direction getNextDirection() {
    int snake_len = pos_queue.size();

    Direction opposite = null;
    if (direction != null) {
      opposite = direction.getOpposite();
    }

    Direction d = null;
    do {
      d = direction_queue.poll();
    } while (d != null && ((snake_len > 1 && d == opposite) || d == direction));

    if (d != null) {
      direction = d;
    }

    return direction;
  }

  /*
  * @brief Causes snake to take one step in its current direction.
  * @return true if snake did not just die, else false (if snake just died).
  */
  public synchronized boolean step() {
    Direction dir = getNextDirection();

    if (!getInitialReportSent() || isDead()) {
      // System.out.println("NOT STEPPING; " initialreportsent=" + initial_report_sent);
      return true;
    }

    Position new_head_pos;
    if (dir != null) {
      new_head_pos = board.getOffsetPosition(dir, head_pos);
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
      case Board.NULL:
        // Ran into wall or snake body -> DIE.
        dead = true;
        conn.die();
        return false; // Indicates that snake just died.
      }

      if (remaining_growth > 0) {
        remaining_growth--;
      } else {
        Position old_pos = pos_queue.poll();
        board.releasePosition(old_pos);
      }
      
      // Send update if new head position is in a new frame.
      if (!inSameFrame(head_pos, new_head_pos)) {
        Position topleft = Board.getContainingWindowTopLeft(new_head_pos);
        Position a = null, b = null, c = null;

        if (dir == Direction.NORTH) {
          a = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, -Board.WINDOW_COLS);
          b = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, 0);
          c = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, Board.WINDOW_COLS);
        } else if (dir == Direction.SOUTH) {
          a = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, -Board.WINDOW_COLS);
          b = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, 0);
          c = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, Board.WINDOW_COLS);
        } else if (dir == Direction.EAST) {
          a = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, Board.WINDOW_COLS);
          b = board.getOffsetPosition(topleft, 0, Board.WINDOW_COLS);
          c = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, Board.WINDOW_COLS);
        } else if (dir == Direction.WEST) {
          a = board.getOffsetPosition(topleft, -Board.WINDOW_ROWS, -Board.WINDOW_COLS);
          b = board.getOffsetPosition(topleft, 0, -Board.WINDOW_COLS);
          c = board.getOffsetPosition(topleft, Board.WINDOW_ROWS, -Board.WINDOW_COLS);
        } else {
          System.out.println("ERROR - Invalid direction");
        }

        if (board.isValidPosition(a)) {
          sendFoodReport(a);
          sendObstacleReport(a);
        }

        if (board.isValidPosition(b)) {
          sendFoodReport(b);
          sendObstacleReport(b);
        }

        if (board.isValidPosition(c)) {
          sendFoodReport(c);
          sendObstacleReport(c);
        }
      }

      head_pos = new_head_pos;
    } else {
      new_head_pos = head_pos;
    }

    //Send enemy locations before setting position. This fixes #67.
    sendEnemyLocations();
    conn.sendAbsolutePosition(head_pos);

    return true;
  }
}
