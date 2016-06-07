/**
 * @author Eugene Marinelli
 */

package Snake;

import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HumanSnake extends Snake {
  private Position last_head_pos;
  private LinkedList<Direction> direction_queue;
  private boolean initial_report_sent;

  private int[][] reported_food_window_timestamps;
  private int[][] reported_obstacle_window_timestamps;
  private boolean one_time_data_sent;

  private SnakeConnection conn;
  private int inactivity_watchdog;
  private boolean ready_for_reset, ready_to_report;
  private String ip;
  private Direction direction;

  private Collection <ColoredPosition> last_enemies, cur_enemies, cur_snakes, added_enemy_set, added_self_set,
    last_self, cur_self;

  private static final int WINDOW_ROW_OFFSET = Board.WINDOW_ROWS / 2;
  private static final int WINDOW_COL_OFFSET = Board.WINDOW_COLS / 2;

  private static final int MAX_DIR_QUEUE_SIZE = 3;
  private static final int INACTIVITY_TIMEOUT = 200;

  public HumanSnake(Board board, Socket sock) {
    super(board);
    this.ip = sock.getInetAddress().getHostName();

    this.clearTimestamps();

    this.resetWatchdog();
    this.last_head_pos = null;
    this.initial_report_sent = false;
    this.direction_queue = new LinkedList<Direction>();
    this.dead = true; // Snake considered dead until initial restart is sent.
    this.one_time_data_sent = false;
    this.ready_for_reset = false;
    this.id = null;
    this.ready_to_report = false;
    this.color = SnakeColor.DARKGRAY;

    this.added_enemy_set = new LinkedHashSet<ColoredPosition>(); // Allocate these once to save time/memory
    this.added_self_set = new LinkedHashSet<ColoredPosition>();
    this.last_enemies = new HashSet<ColoredPosition>();
    this.cur_enemies = new HashSet<ColoredPosition>();
    this.last_self = new HashSet<ColoredPosition>();
    this.cur_self = new HashSet<ColoredPosition>();
    this.cur_snakes = new HashSet<ColoredPosition>();
  }

  public void setConnection(SnakeConnection c) {
    this.conn = c;
  }

  public String getIP() {
    return ip;
  }

  public boolean disconnected() {
    return conn == null || conn.disconnected();
  }

  public void setId(String id) {
    this.id = id;
    this.ready_to_report = true;
  }

  private synchronized void sendInitialReport() {
    if (!this.one_time_data_sent) {
      this.conn.sendBoardSize(board.getMaxHeight(), board.getMaxWidth());
      this.one_time_data_sent = true;
    }

    this.conn.sendAbsolutePosition(head_pos());
    this.initial_report_sent = true;
  }

  private void sendSnakeLocations() {
    Position topleft = board.getOffsetPosition(head_pos(), -WINDOW_ROW_OFFSET, -WINDOW_COL_OFFSET);
    int topleft_r = topleft.getRow();
    int topleft_c = topleft.getCol();

    this.cur_snakes.clear();
    this.cur_enemies.clear();
    this.cur_self.clear();

    // LOTS OF CPU
    this.board.getAbsolutePositionsInWindow(cur_snakes, topleft, Board.CellStatus.SNAKE, Board.WINDOW_ROWS,
      Board.WINDOW_COLS);

    this.added_self_set.clear();
    this.added_enemy_set.clear();

    // Separate the window snake locations into enemy and self.
    for (ColoredPosition cur : this.cur_snakes) {
      if (!this.occupiesPosition(cur)) {
        this.cur_enemies.add(cur);
      } else {
        this.cur_self.add(cur);
      }
    }

    for (ColoredPosition cur : this.cur_enemies) {
      if (!this.last_enemies.contains(cur)) {
        this.added_enemy_set.add(new ColoredPosition(cur.getRow() - topleft_r, cur.getCol() - topleft_c, cur.getColor()));
      } else {
        this.last_enemies.remove(cur);
      }
    }

    for (Position cur : cur_self) {
      if (!last_self.contains(cur)) {
        added_self_set.add(new ColoredPosition(cur.getRow() - topleft_r, cur.getCol() - topleft_c, null));
      } else {
        last_self.remove(cur);
      }
    }

    this.conn.sendEnemySnakeRemoveReport(last_enemies);
    this.conn.sendSelfSnakeRemoveReport(last_self);
    this.conn.sendEnemySnakeAddReport(topleft, added_enemy_set);
    this.conn.sendSelfSnakeAddReport(topleft, added_self_set);

    // Swap the sets (instead of allocating a new one each time).
    Collection<ColoredPosition> tmp = last_enemies;
    this.last_enemies = this.cur_enemies;
    this.cur_enemies = tmp;

    tmp = last_self;
    this.last_self = this.cur_self;
    this.cur_self = tmp;
  }

  public void addKilledSnake(Snake other) {
    if (other != this) {
      this.conn.reportKill(other.id);
    }
    super.addKilledSnake(other);
  }

  public boolean readyToReport() {
    return ready_to_report;
  }

  public void setReadyForReset() {
    this.direction_queue.clear(); // Clear this here to avoid race condition of receiving direction before reset called.

    // At beginning of round, we might receive a ready for reset from client, but snake is ready for reset by default.
    if (this.getLength() == 0)
      this.ready_for_reset = true;
  }

  public void handleRoundEnd() {
    super.handleRoundEnd();
    this.setReadyForReset();
  }

  public boolean readyForReset() {
    assert(!ready_for_reset || this.getLength() == 0) : "Ready for reset is " + ready_for_reset + " and length is " + this.getLength();

    return ready_for_reset;
  }

  public synchronized void setDirection(Direction dir) {
    if (direction_queue.peek() == null) {
      this.direction = dir;
    }
    if (direction_queue.size() < MAX_DIR_QUEUE_SIZE) {
      this.direction_queue.offer(dir);
    }
  }

  /**
  * @return true on success, false on failure.
  */
  public synchronized boolean reset() {
    if (!super.reset()) {
      return false;
    }

    assert(this.getLength() == 1);

    this.initial_report_sent = false;
    this.direction_queue.clear();
    this.dead = false;
    this.ready_for_reset = false;
    this.resetWatchdog();
    this.last_head_pos = this.head_pos();
    this.direction = null;

    this.sendInitialReport();

    return true;
  }

  private void clearTimestamps() {
    // Initialize obstacle frames that have been sent.
    int vwindows = this.board.getNumVerticalWindows();
    int hwindows = this.board.getNumHorizontalWindows();

    this.reported_food_window_timestamps = new int[vwindows][hwindows];
    this.reported_obstacle_window_timestamps = new int[vwindows][hwindows];
    for (int i = 0; i < vwindows; i++) {
      for (int j = 0; j < hwindows; j++) {
        this.reported_food_window_timestamps[i][j] = 0;
        this.reported_obstacle_window_timestamps[i][j] = -1;
      }
    }
  }

  public void setBoard(Board board) {
    this.board = board;
    this.conn.sendBoardSize(this.board.getMaxHeight(), this.board.getMaxWidth());
    this.clearTimestamps();
  }

  public void roundReset() {
    if (disconnected()) {
      return;
    }

    dead = true; // Slight hack... Reset only does something when snake is dead.
    super.roundReset();
  }

  protected Direction direction() {
    return this.direction;
  }

  private synchronized void advanceDirection() {
    Direction opposite = null;
    Direction cur_dir = this.direction();

    if (cur_dir != null) {
      opposite = cur_dir.getOpposite();
    }

    int snake_len = this.getLength();
    Direction d = null;
    do {
      d = direction_queue.poll();
    } while (d != null && ((snake_len > 1 && d == opposite) || d == cur_dir));

    if (d != null) {
      this.direction = d;
    }
  }

  public Position prepareNextHeadPosition() {
    if (this.dead) {
      return null;
    }
    //assert(this.getLength() > 0) : "length of " + this + " is " + this.getLength();

/*  TODO uncomment for watchdog timer to work
    if (!initial_report_sent || dead) {
      if (!decrementAndTestWatchdog()) {
        conn.disconnect();
      }
      return null;
    }

*/
    this.last_head_pos = this.head_pos();

    if (this.direction() != null && this.head_pos() != null) {
      this.next_head_pos = this.board.getOffsetPosition(this.direction(), this.head_pos());
      this.advanceDirection();
    } else {
      this.next_head_pos = this.head_pos();
    }


/*  TODO uncomment for watchdog timer to work
    if (next_head_pos != null && head_pos() != null && !next_head_pos.equals(head_pos())) {
      this.resetWatchdog();
      return null;
    } else {
      // Not moving is also considered inactivity.
      if (!decrementAndTestWatchdog()) {
        conn.disconnect();
        return null;
      }
    }
*/

    return this.next_head_pos;
  }

  private void resetWatchdog() {
    this.inactivity_watchdog = INACTIVITY_TIMEOUT;
  }

  /**
  * @return false if timer has expired, else true.
  */
  private boolean decrementAndTestWatchdog() {
    inactivity_watchdog--;
    if (inactivity_watchdog == 0) {
      return false;
    } else {
      return true;
    }
  }

  public void handleFood() {
    conn.sendFood();
    super.handleFood();
  }

  public void handleKilledBy(Snake killer) {
    if (killer == this) {
      conn.die();
    } else {
      conn.reportKilledBy(killer.getId());
    }

    super.handleKilledBy(killer);
  }

  private void sendFoodAndObstacleReport(Position topleft) {
    if (!disconnected() && board.isValidPosition(topleft)) {
      int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
      int mark_col = topleft.getCol() / Board.WINDOW_COLS;

      int board_time = board.getFoodWindowTimestamp(mark_row, mark_col);
      if (board_time != reported_food_window_timestamps[mark_row][mark_col]) {
        conn.sendFoodReport(topleft, board.getPositionsInWindow(topleft, Board.CellStatus.FOOD));
        reported_food_window_timestamps[mark_row][mark_col] = board_time;
      }

      board_time = board.getWallWindowTimestamp(mark_row, mark_col);
      if (board_time != reported_obstacle_window_timestamps[mark_row][mark_col]) {
        conn.sendObstacleReport(topleft, board.getPositionsInWindow(topleft, Board.CellStatus.WALL));
        reported_obstacle_window_timestamps[mark_row][mark_col] = board_time;
      }
    }
  }

  public void sendNumClients(int numclients) {
    this.conn.sendNumClients(numclients);
  }

  public void send(String msg) {
    this.conn.send(msg);
  }

  public void reportRoundEnd(String s, int rank) {
    this.conn.reportRoundEnd(s, rank);
  }

  public void flush_connection() {
    this.conn.flush();
  }

  public void disconnect() {
    this.conn.disconnect();
  }

  public void sendRoundStatus(SnakeServer.GameMode mode, long time_remaining) {
    this.conn.sendRoundStatus(mode, time_remaining);
  }

  public void sendGameData() {
    if (!initial_report_sent || head_pos() == null) {
      return;
    }

    Position upleft = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos(), -WINDOW_ROW_OFFSET,
      -WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(upleft);

    Position upright = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos(), -WINDOW_ROW_OFFSET,
      WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(upright);

    Position downleft = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos(), WINDOW_ROW_OFFSET,
      -WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(downleft);

    Position downright = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos(), WINDOW_ROW_OFFSET,
      WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(downright);

    //Send enemy locations before setting position. This fixes #67.
    sendSnakeLocations();

    if (!head_pos().equals(last_head_pos)) {
      conn.sendAbsolutePosition(head_pos());
    } else {
      conn.sendTick(); // Send a smaller piece of data which does the same thing if position is unchanged.
    }
  }
}
