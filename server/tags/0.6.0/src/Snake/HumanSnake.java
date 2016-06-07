/**
 * @author Eugene Marinelli
 */

package Snake;

import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HumanSnake extends Snake {
  private Set<Position> pos_set;
  private Position last_head_pos;
  private Direction direction;
  private LinkedList<Direction> direction_queue;
  private int remaining_growth;
  private boolean initial_report_sent;

  private int[][] reported_food_window_timestamps;
  private int[][] reported_obstacle_window_timestamps;
  private boolean one_time_data_sent;

  private int inactivity_watchdog;
  private boolean ready_for_reset;
  private String ip;
  private boolean ready_to_report;

  private Collection last_enemies, cur_enemies, cur_snakes, added_enemy_set, added_self_set,
    last_self, cur_self;

  private static final int WINDOW_ROW_OFFSET = Board.WINDOW_ROWS / 2;
  private static final int WINDOW_COL_OFFSET = Board.WINDOW_COLS / 2;

  private static final int MAX_DIR_QUEUE_SIZE = 3;
  private static final int INACTIVITY_TIMEOUT = 200;

  public HumanSnake(Board board, Socket sock) {
    this.board = board;

    // Initialize obstacle frames that have been sent.
    int vwindows = board.getNumVerticalWindows();
    int hwindows = board.getNumHorizontalWindows();

    reported_food_window_timestamps = new int[vwindows][hwindows];
    reported_obstacle_window_timestamps = new int[vwindows][hwindows];
    for (int i = 0; i < vwindows; i++) {
      for (int j = 0; j < hwindows; j++) {
        reported_food_window_timestamps[i][j] = 0;
        reported_obstacle_window_timestamps[i][j] = -1;
      }
    }

    resetWatchdog();
    next_head_pos = null;
    head_pos = null;
    last_head_pos = null;
    direction = null;
    initial_report_sent = false;
    pos_queue = new LinkedList<Position>();
    pos_set = new HashSet<Position>();
    direction_queue = new LinkedList<Direction>();
    dead = true; // Snake considered dead until initial restart is sent.
    one_time_data_sent = false;
    ready_for_reset = false;
    id = null;
    ready_to_report = false;
    eater = null;
    color = SnakeColor.BLUE;

    added_enemy_set = new LinkedHashSet<ColoredPosition>(); // Allocate these once to save time/memory
    added_self_set = new LinkedHashSet<ColoredPosition>();
    last_enemies = new HashSet<ColoredPosition>();
    cur_enemies = new HashSet<ColoredPosition>();
    last_self = new HashSet<ColoredPosition>();
    cur_self = new HashSet<ColoredPosition>();
    cur_snakes = new HashSet<ColoredPosition>();

    ip = sock.getInetAddress().getHostName();
    conn = new SnakeConnection(sock, this);
  }

  public synchronized void clearBody() {
    pos_set.clear();

    Position p;
    while ((p = pos_queue.poll()) != null) {
      //board.transmuteToFood(p);
      board.releasePosition(p);
    }
  }

  public String getIP() {
    return ip;
  }

  public boolean disconnected() {
    return conn == null || conn.disconnected();
  }

  public void setId(String id) {
    this.id = id;
    ready_to_report = true;
  }

  public String getId() {
    return id;
  }

  /**
  * @return false on failure (so spaces available), true on success.
  */
  private boolean resetSnake() {
    clearBody();  // Need to do this to eliminate race condition of snake reset before snake body cleared.

    // Snake starts with just a head.  Occupy a random initial position.
    head_pos = board.occupyRandomOpenPosition();
    if (head_pos == null) { // No open positions.
      conn.disconnect();
      return false;
    }

    pos_queue.offer(head_pos);
    pos_set.add(head_pos);

    remaining_growth = 0;
    initial_report_sent = false;
    direction = null;
    direction_queue.clear();
    dead = false;
    next_head_pos = null;
    ready_for_reset = false;
    eater = null;
    kill_reported = false;
    resetWatchdog();

    return true;
  }

  private synchronized void sendInitialReport() {
    if (!one_time_data_sent) {
      conn.sendBoardSize(board.getMaxHeight(), board.getMaxWidth());
      one_time_data_sent = true;
    }

    conn.sendAbsolutePosition(head_pos);

    initial_report_sent = true;
  }

  private void sendSnakeLocations() {
    Position topleft = board.getOffsetPosition(head_pos, -WINDOW_ROW_OFFSET, -WINDOW_COL_OFFSET);
    int topleft_r = topleft.getRow();
    int topleft_c = topleft.getCol();

    cur_snakes.clear();
    cur_enemies.clear();
    cur_self.clear();

    // LOTS OF CPU
    board.getAbsolutePositionsInWindow(cur_snakes, topleft, Board.OCCUPIED, Board.WINDOW_ROWS, Board.WINDOW_COLS);

    added_self_set.clear();
    added_enemy_set.clear();

    // Separate the window snake locations into enemy and self.
    Iterator<ColoredPosition> itr = cur_snakes.iterator();
    while (itr.hasNext()) {
      ColoredPosition cur = itr.next();
      if (!pos_set.contains(cur)) {
        cur_enemies.add(cur);
      } else {
        cur_self.add(cur);
      }
    }

    itr = cur_enemies.iterator();
    while (itr.hasNext()) {
      ColoredPosition cur = itr.next();
      if (!last_enemies.contains(cur)) {
        added_enemy_set.add(new ColoredPosition(cur.getRow() - topleft_r, cur.getCol() - topleft_c, cur.getColor()));
      } else {
        last_enemies.remove(cur);
      }
    }

    itr = cur_self.iterator();
    while (itr.hasNext()) {
      Position cur = itr.next();
      if (!last_self.contains(cur)) {
        added_self_set.add(new ColoredPosition(cur.getRow() - topleft_r, cur.getCol() - topleft_c, null));
      } else {
        last_self.remove(cur);
      }
    }

    conn.sendEnemySnakeRemoveReport(last_enemies);
    conn.sendSelfSnakeRemoveReport(last_self);
    conn.sendEnemySnakeAddReport(topleft, added_enemy_set);
    conn.sendSelfSnakeAddReport(topleft, added_self_set);

    // Swap the sets (instead of allocating a new one each time).
    Collection<ColoredPosition> tmp = last_enemies;
    last_enemies = cur_enemies;
    cur_enemies = tmp;

    tmp = last_self;
    last_self = cur_self;
    cur_self = tmp;
  }

  public void gotEatenBy(Snake eater) {
    this.eater = eater;
    handleKilledBy(eater);
  }

  public boolean readyToReport() {
    return ready_to_report;
  }

  public void setReadyForReset() {
    direction_queue.clear(); // Clear this here to avoid race condition of receiving direction before reset called.
    ready_for_reset = true;
  }

  public boolean readyForReset() {
    return ready_for_reset;
  }

  public synchronized void setDirection(Direction dir) {
    if (direction_queue.size() < MAX_DIR_QUEUE_SIZE) {
      direction_queue.offer(dir);
    }
  }

  public SnakeConnection getConnection() {
    return conn;
  }

  /**
  * @return true on success, false on failure.
  */
  public synchronized boolean reset() {
    // Only reset if the snake has died.
    if (dead) {
      if (resetSnake()) {
        sendInitialReport();
      } else {
        return false;
      }
    }

    return true;
  }

  public void roundReset() {
    if (disconnected()) {
      return;
    }

    score = 0;

    // Reset food timestamps since food is reset between rounds.
    int vwindows = board.getNumVerticalWindows();
    int hwindows = board.getNumHorizontalWindows();
    for (int i = 0; i < vwindows; i++) {
      for (int j = 0; j < hwindows; j++) {
        reported_food_window_timestamps[i][j] = 0;
      }
    }

    dead = true; // Slight hack... Reset only does something when snake is dead.
    reset();
  }

  private synchronized Direction getNextDirection() {
    Direction opposite = null;
    if (direction != null) {
      opposite = direction.getOpposite();
    }

    int snake_len = getLength();
    Direction d = null;

    do {
      d = direction_queue.poll();
    } while (d != null && ((snake_len > 1 && d == opposite) || d == direction));

    if (d != null) {
      direction = d;
    }

    return direction;
  }

  public Position prepareNextHeadPosition() {
    if (dead) {
      return null;
    }

    Direction dir = getNextDirection();
    if (dir != null && head_pos != null) {
      next_head_pos = board.getOffsetPosition(dir, head_pos);
    } else {
      next_head_pos = head_pos;
    }
    return next_head_pos;
  }

  public void registerKill(String victim_id) {
    conn.reportKill(victim_id);
    score += KILL_REWARD;
  }

  private void resetWatchdog() {
    inactivity_watchdog = INACTIVITY_TIMEOUT;
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

  protected void growBy(int amt) {
    remaining_growth += amt;
    score += amt;

    pos_queue.offer(head_pos);
    pos_set.add(head_pos);
    conn.sendGrow();
  }

  protected void addHeadPosition(Position p) {
    pos_queue.offer(head_pos);
    pos_set.add(head_pos);
  }

  protected void resolveTailPositions() {
    if (remaining_growth > 0) {
      remaining_growth--;
    } else {
      Position old_pos = pos_queue.poll();
      board.releasePosition(old_pos);
      pos_set.remove(old_pos);
    }
  }

  protected void handleKilledBy(Snake other) {
    assert(other != this);
    assert(other != null);

    if (dead) {
      return;
    }

    score -= DEATH_PENALTY;
    dead = true;

    other.registerKill(id);
    conn.reportKilledBy(other.getId());
  }

  protected void handleSelfDeath() {
    if (dead) {
      return;
    }

    score -= DEATH_PENALTY;
    dead = true;
    conn.die();
  }

  /*
  * @brief Causes snake to take one step in its current direction.
  * prepareNextHeadPosition must be called before this.
  * @return true if snake did not just die, else false (if snake just died).
  */
  public synchronized Snake step(Set<Position> collision_set, Map<Position, Snake> single_unit_map) {
    if (!initial_report_sent || dead) {
      if (!decrementAndTestWatchdog()) {
        conn.disconnect();
        return this;
      } else {
        return null;
      }
    }

    last_head_pos = head_pos;

    if (next_head_pos != null && head_pos != null && !next_head_pos.equals(head_pos)) {
      resetWatchdog();
      return stepCommon(collision_set, single_unit_map);
    } else {
      // Not moving is also considered inactivity.
      if (!decrementAndTestWatchdog()) {
        conn.disconnect();
        return this;
      } else {
        return null;        
      }
    }
  }

  private void sendFoodAndObstacleReport(Position topleft) {
    if (!disconnected() && board.isValidPosition(topleft)) {
      int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
      int mark_col = topleft.getCol() / Board.WINDOW_COLS;

      int board_time = board.getFoodWindowTimestamp(mark_row, mark_col);
      if (board_time != reported_food_window_timestamps[mark_row][mark_col]) {
        conn.sendFoodReport(topleft, board.getPositionsInWindow(topleft, Board.FOOD));
        reported_food_window_timestamps[mark_row][mark_col] = board_time;
      }

      board_time = board.getWallWindowTimestamp(mark_row, mark_col);
      if (board_time != reported_obstacle_window_timestamps[mark_row][mark_col]) {
        conn.sendObstacleReport(topleft, board.getPositionsInWindow(topleft, Board.WALL));
        reported_obstacle_window_timestamps[mark_row][mark_col] = board_time;
      }
    }
  }

  public void sendGameData() {
    if (!initial_report_sent || head_pos == null) {
      return;
    }

    Position upleft = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos, -WINDOW_ROW_OFFSET,
      -WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(upleft);

    Position upright = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos, -WINDOW_ROW_OFFSET,
      WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(upright);

    Position downleft = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos, WINDOW_ROW_OFFSET,
      -WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(downleft);

    Position downright = board.getContainingWindowTopLeft(board.getOffsetPosition(head_pos, WINDOW_ROW_OFFSET,
      WINDOW_COL_OFFSET));
    sendFoodAndObstacleReport(downright);

    //Send enemy locations before setting position. This fixes #67.
    sendSnakeLocations();

    if (!head_pos.equals(last_head_pos)) {
      conn.sendAbsolutePosition(head_pos);
    } else {
      conn.sendTick(); // Send a smaller piece of data which does the same thing if position is unchanged.
    }
  }
}
