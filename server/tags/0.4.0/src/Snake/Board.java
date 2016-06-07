/**
 * Eugene Marinelli
 */

package Snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Board {
  public static final char WALL = '3';
  public static final char FOOD = '2';
  public static final char OCCUPIED = '1';
  public static final char FREE = '0';
  public static final char NULL = (char)0;

  public static final int WINDOW_ROWS = 30;
  public static final int WINDOW_COLS = 40;

  private static final String obstacle_map_file = "maps/simple_ascii.ppm"; //TODO make this an input

  private char[][] grid;
  private int amount_of_food;
  private int[][] window_food_timestamps;
  private int vert_windows;
  private int horiz_windows;

  // Two position free lists are used to avoid gameplay affecting which positions are chosen and to avoid constant
  // shuffling.
  private LinkedList<Position> free_positions;
  private LinkedList<Position> used_positions;

  public Board() {
    amount_of_food = 0;
    free_positions = new LinkedList<Position>();
    used_positions = new LinkedList<Position>();

    // Load and construct opstacle grid based on map file.
    char[][] obstacle_map = ObstacleMapReader.read(obstacle_map_file);

    int rows = obstacle_map.length;
    int cols = obstacle_map[0].length;

    grid = new char[rows][cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        if (obstacle_map[i][j] == 0) {
          grid[i][j] = WALL;
        } else {
          grid[i][j] = FREE;
          free_positions.addFirst(new Position(i, j));
        }
      }
    }

    // This is acceptable because Collections dumps the linked list into an array before shuffling.
    Collections.shuffle(free_positions);

    horiz_windows = (int)((((float)getHeight()) / (float)WINDOW_COLS) + 0.5);
    vert_windows = (int)((((float)getWidth()) / (float)WINDOW_ROWS) + 0.5);
    window_food_timestamps = new int[vert_windows][horiz_windows];
    for (int i = 0; i < vert_windows; i++) {
      for (int j = 0; j < horiz_windows; j++) {
        window_food_timestamps[i][j] = 0;
      }
    }
  }

  public int getNumVerticalWindows() {
    return vert_windows;
  }

  public int getNumHorizontalWindows() {
    return horiz_windows;
  }

  public Position occupyRandomOpenPosition() {
    return fillRandomOpenPosition(OCCUPIED);
  }

  public synchronized void placeRandomFood() {
    Position fpos = fillRandomOpenPosition(FOOD);
    Position topleft = getContainingWindowTopLeft(fpos);

    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS;
    window_food_timestamps[mark_row][mark_col]++;

    amount_of_food++;
  }

  public int getFoodWindowTimestamp(Position topleft) {
    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS;
    return window_food_timestamps[mark_row][mark_col];
  }

  public int getWidth() {
    return grid.length;
  }

  public int getHeight() {
    return grid[0].length;
  }

  public int getFoodAmount() {
    return amount_of_food;
  }

  public synchronized Position fillRandomOpenPosition(char fill_type) {
    Position free_pos = free_positions.removeFirst();
    if (free_pos == null) {
      // If we are out of free positions, we recycle the used positions.
      Collections.shuffle(used_positions);
      free_positions = used_positions;
      used_positions = new LinkedList<Position>();
    }

    grid[free_pos.getRow()][free_pos.getCol()] = fill_type;

    return free_pos;
  }

  public boolean isValidPosition(Position p) {
    return p != null && p.getRow() >= 0 && p.getRow() < getHeight() && p.getCol() >= 0 && p.getCol() < getWidth();
  }

  private boolean isValidPosition(int r, int c) {
    return r >= 0 && r < getHeight() && c >= 0 && c < getWidth();
  }

  public synchronized char grabPosition(Position p) {
    if (!isValidPosition(p)) {
      return NULL;
    }

    char old_val = grid[p.getRow()][p.getCol()];

    if (old_val != WALL && old_val != OCCUPIED) {
      grid[p.getRow()][p.getCol()] = OCCUPIED;
    }

    if (old_val == FOOD) {
      amount_of_food--;
    }

    return old_val;
  }

  public synchronized void releasePosition(Position p) {
    if (!isValidPosition(p)) {
      return;
    }

    assert(grid[p.getRow()][p.getCol()] == OCCUPIED);

    grid[p.getRow()][p.getCol()] = FREE;
    used_positions.addFirst(p);
  }

  public Position getOffsetPosition(Position p, int rows, int cols) {
    int new_row = p.getRow() + rows;
    int new_col = p.getCol() + cols;
    return new Position(new_row, new_col);
  }

  public Position getOffsetPosition(Direction dir, Position p) {
    assert(dir != null);

    if (dir == Direction.NORTH) {
      return new Position(p.getRow() - 1, p.getCol());
    } else if (dir == Direction.SOUTH) {
      return new Position(p.getRow() + 1, p.getCol());
    } else if (dir == Direction.WEST) {
      return new Position(p.getRow(), p.getCol() - 1);
    } else if (dir == Direction.EAST) {
      return new Position(p.getRow(), p.getCol() + 1);
    } else {
      return null;
    }
  }

  public List<Position> getPositionsInWindow(Position topleft, int type) {
    List<Position> positions = new ArrayList();
    int rstart = topleft.getRow();
    int cstart = topleft.getCol();
    int board_cols = getWidth();
    int board_rows = getHeight();

    if (rstart + WINDOW_ROWS < 0 || cstart + WINDOW_COLS < 0 || rstart >= board_rows || cstart >= board_cols) {
      // Window is completely outside of the grid.
      return positions;
    }

    int istart = 0, jstart = 0;
    if (rstart < 0) {
      istart = -rstart;
    }
    if (cstart < 0) {
      jstart = -cstart;
    }

    int r = rstart + istart;
    for (int i = istart; i < WINDOW_ROWS && r < board_rows; i++) {
      int c = cstart + jstart;
      for (int j = jstart; j < WINDOW_COLS && c < board_cols; j++) {
        if (grid[r][c] == type) {
          positions.add(new Position(i, j));
        }
        c++;
      }
      r++;
    }

    return positions;
  }

  public static Position getContainingWindowTopLeft(Position pos) {
    return new Position((pos.getRow() / WINDOW_ROWS) * WINDOW_ROWS, (pos.getCol() / WINDOW_COLS) * WINDOW_COLS);
  }

  // TODO - return a list of locations instead of the entire grid.
  public boolean[][] getBoolGridOfType(Position topleft, int type, int rows, int cols) {
    boolean[][] window = new boolean[rows][cols];
    int rstart = topleft.getRow();
    int cstart = topleft.getCol();
    int board_cols = getWidth();
    int board_rows = getHeight();

    if (rstart + rows < 0 || cstart + cols < 0 || rstart >= board_rows || cstart >= board_cols) {
      return window;
    }

    int istart = 0, jstart = 0;
    if (rstart < 0) {
      istart = -rstart;
    }
    if (cstart < 0) {
      jstart = -cstart;
    }

    int r = rstart + istart;
    for (int i = istart; i < rows && r < board_rows; i++) {
      int c = cstart + jstart;
      for (int j = jstart; j < cols && c < board_cols; j++) {
        if (grid[r][c] == type) {
          window[i][j] = true;
        }
        c++;
      }
      r++;
    }

    return window;
  }
}
