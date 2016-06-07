/**
 * Eugene Marinelli
 */

package Snake;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Board {
  public static final char WALL = '3';
  public static final char FOOD = '2';
  public static final char OCCUPIED = '1';
  public static final char FREE = '0';
  public static final char NULL = (char)0;

  public static final int WINDOW_ROWS = 32;
  public static final int WINDOW_COLS = 44;

  private static final int DEFAULT_INITIAL_ROWS = 35; // TODO input for this
  private static final int DEFAULT_INITIAL_COLS = 35;

  private static final int DEFAULT_MAX_ROWS = 200;
  private static final int DEFAULT_MAX_COLS = 200;

  private static final String DEFAULT_MAP_FILENAME = "maps/simple_ascii.ppm";

  private char[][] full_grid; // Full map loaded from map file.
  private char[][] live_grid; // A subset of the full map.
  int live_rows, live_cols;
  int full_rows, full_cols;

  private int amount_of_food;
  private int[][] window_food_timestamps;
  private int[][] window_wall_timestamps;
  private int vert_windows;
  private int horiz_windows;

  // Two position free lists are used to avoid gameplay affecting which positions are chosen and to avoid constant
  // shuffling.
  private LinkedList<Position> free_positions;
  private LinkedList<Position> used_positions;

  private Map<Position, Snake> position_snake_map;

  public Board() {
    initBoard(DEFAULT_MAX_ROWS, DEFAULT_MAX_COLS, DEFAULT_MAP_FILENAME);
  }

  public Board(String map_filename) {
    initBoard(DEFAULT_MAX_ROWS, DEFAULT_MAX_COLS, map_filename);
  }

  public Board(String map_filename, int max_rows, int max_cols) {
    initBoard(max_rows, max_cols, map_filename);
  }

  public Board(int max_rows, int max_cols) {
    initBoard(max_rows, max_cols, DEFAULT_MAP_FILENAME);
  }

  private void initBoard(int max_rows, int max_cols, String map_filename) {
    // Load and construct opstacle grid based on map file.
    System.out.println("Loading map: " + map_filename);
    char[][] obstacle_map = ObstacleMapReader.read(map_filename);
    if (obstacle_map == null) {
      System.out.println("Failed to load map file.");
      System.exit(1);
    }

    full_rows = obstacle_map.length > max_rows ? max_rows : obstacle_map.length;
    full_cols = obstacle_map[0].length > max_cols ? max_cols : obstacle_map[0].length;
    full_grid = new char[full_rows][full_cols];
    for (int i = 0; i < full_rows; i++) {
      for (int j = 0; j < full_cols; j++) {
        if (obstacle_map[i][j] == 0 || i == full_rows - 1 || j == full_cols - 1 || i == 0 || j == 0) {
          full_grid[i][j] = WALL;
        } else {
          full_grid[i][j] = FREE;
        }
      }
    }

    // Initialize timestamp matrices.
    vert_windows = (int)Math.ceil((float)full_rows / WINDOW_ROWS);
    horiz_windows = (int)Math.ceil((float)full_cols / WINDOW_COLS);
    window_food_timestamps = new int[vert_windows][horiz_windows];
    window_wall_timestamps = new int[vert_windows][horiz_windows];
    for (int i = 0; i < vert_windows; i++) {
      for (int j = 0; j < horiz_windows; j++) {
        window_food_timestamps[i][j] = 0;
        window_wall_timestamps[i][j] = 0;
      }
    }

    // Now initialize the "live" grid - the subset of the full grid which is reported to the client.
    free_positions = new LinkedList<Position>();
    used_positions = new LinkedList<Position>();

    position_snake_map = new HashMap<Position, Snake>();

    live_rows = DEFAULT_INITIAL_ROWS < full_rows ? DEFAULT_INITIAL_ROWS : full_rows;
    live_cols = DEFAULT_INITIAL_COLS < full_cols ? DEFAULT_INITIAL_COLS : full_cols;
    live_grid = new char[full_rows][full_cols]; //allocate the full size so that we don't have to copy later.
    for (int i = 0; i < live_rows; i++) {
      for (int j = 0; j < live_cols; j++) {
        if (obstacle_map[i][j] == 0 || i == live_rows - 1 || j == live_cols - 1 || i == 0 || j == 0) {
          live_grid[i][j] = WALL;
        } else {
          live_grid[i][j] = FREE;
          free_positions.addFirst(new Position(i, j));
        }
      }
    }

    // Collections adumps the linked list into an array before shuffling, so it doesn't take forever.
    if (free_positions.size() > 0) {
      Collections.shuffle(free_positions);
    }

    amount_of_food = 0;
  }

  public void clearFood() {
    for (int i = 0; i < vert_windows; i++) {
      for (int j = 0; j < horiz_windows; j++) {
        window_food_timestamps[i][j] = 0;
      }
    }

    for (int i = 0; i < full_rows; i++) {
      for (int j = 0; j < full_cols; j++) {
        if (live_grid[i][j] == FOOD) {
          Position p = new Position(i, j);
          used_positions.addFirst(p);
          live_grid[i][j] = FREE;
          amount_of_food--;
        }
      }
    }

    assert(amount_of_food == 0);
  }

  public boolean full_size() {
    return live_rows == full_rows && live_cols == full_cols;
  }

  /**
  * @brief resizeBoard - Used to increase the size of the board.
  * @param new_rows New number of rows.  Must be at least the current number of rows.
  * @param new_cols Corresponding input for cols.
  * @return false if unsuccessful, else true.
  */
  public boolean resize(int new_rows, int new_cols) {
    new_rows = new_rows > full_rows ? full_rows : new_rows;
    new_cols = new_cols > full_cols ? full_cols : new_cols;

    System.out.println("resizing to " + new_rows + "," + new_cols);

    if (new_rows < live_rows || new_cols < live_cols) {
      return false;
    } else if (new_rows == live_rows && new_cols == live_cols) {
      return true;
    } else {
      for (int i = 0; i < live_rows; i++) {
        live_grid[i][live_cols - 1] = full_grid[i][live_cols - 1];
      }
      for (int i = 0; i < live_cols; i++) {
        live_grid[live_rows - 1][i] = full_grid[live_rows - 1][i];
      }

      // Fill in the new region with the values from the original obstacle map.
      for (int i = 0; i < new_rows; i++) { // i=live_rows-1 -- to erase the programmatically generated wall.
        for (int j = 0; j < new_cols; j++) {
          if (i >= live_rows-1 || j >= live_cols-1) {
            Position p = new Position(i, j);
            if (i == new_rows - 1 || j == new_cols - 1 || i == 0 || j == 0) {
              if (live_grid[i][j] != WALL) {
                live_grid[i][j] = WALL;
                incrementWallTimestamp(p);
              }
            } else {
              if (live_grid[i][j] != full_grid[i][j]) {
                live_grid[i][j] = full_grid[i][j];
                incrementWallTimestamp(p);

                if (live_grid[i][j] == FREE) {
                  free_positions.addFirst(new Position(i, j));
                }
              }
            }
          }
        }
      }

      if (free_positions.size() > 0) {
        Collections.shuffle(free_positions);
      }

      live_rows = new_rows;
      live_cols = new_cols;

      return true;
    }
  }

  public int getBoardValue(int r, int c) {
    return live_grid[r][c];
  }

  public static boolean inSameFrame(Position a, Position b) {
    return getContainingWindowTopLeft(a).equals(getContainingWindowTopLeft(b));
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

  private void incrementFoodTimestamp(Position p) {
    Position topleft = getContainingWindowTopLeft(p);
    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS;
    window_food_timestamps[mark_row][mark_col]++;
  }

  private void incrementWallTimestamp(Position p) {
    Position topleft = getContainingWindowTopLeft(p);
    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS;
    window_wall_timestamps[mark_row][mark_col]++;
  }

  public synchronized void placeRandomFood() {
    Position fpos = fillRandomOpenPosition(FOOD);
    incrementFoodTimestamp(fpos);
    amount_of_food++;
  }

  // Allows us to factor out mark_row and mark_col calculations.
  public int getFoodWindowTimestamp(int mark_row, int mark_col) {
    return window_food_timestamps[mark_row][mark_col];    
  }

  public int getWallWindowTimestamp(int mark_row, int mark_col) {
    return window_wall_timestamps[mark_row][mark_col];    
  }

  public int getFoodWindowTimestamp(Position topleft) {
    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS;
    return getFoodWindowTimestamp(mark_row, mark_col);
  }

  public int getWallWindowTimestamp(Position topleft) {
    int mark_row = topleft.getRow() / Board.WINDOW_ROWS;
    int mark_col = topleft.getCol() / Board.WINDOW_COLS; 
    return getWallWindowTimestamp(mark_row, mark_col);
  }

  public int getWidth() {
    return live_cols;
  }

  public int getHeight() {
    return live_rows;
  }

  public int getMaxWidth() {
    return full_cols;
  }

  public int getMaxHeight() {
    return full_rows;
  }

  public int getFoodAmount() {
    return amount_of_food;
  }

  public Position allocateFreePosition() {
    // Instead of removing free positions that get taken by snakes, which is costly, we just check to make sure that
    // the positions we remove from the list are actually free, else just discard them.
    Position free_pos = null;
    do {
      if (free_positions.size() == 0) {
        if (used_positions.size() == 0) { // Weird case of extremely small board.
          return null;
        }

        // If we are out of free positions, we recycle the used positions.
        Collections.shuffle(used_positions);
        free_positions = used_positions;
        used_positions = new LinkedList<Position>();
      }

      free_pos = free_positions.removeFirst();
    } while (live_grid[free_pos.getRow()][free_pos.getCol()] != FREE);

    return free_pos;
  }

  public synchronized Position fillRandomOpenPosition(char fill_type) {
    Position free_pos = allocateFreePosition();
    if (free_pos != null) {
      live_grid[free_pos.getRow()][free_pos.getCol()] = fill_type;
    }
    return free_pos;
  }

  public boolean isValidPosition(Position p) {
    return p != null && p.getRow() >= 0 && p.getRow() < getHeight() && p.getCol() >= 0 && p.getCol() < getWidth();
  }

  private boolean isValidPosition(int r, int c) {
    return r >= 0 && r < getHeight() && c >= 0 && c < getWidth();
  }

  public synchronized char grabPosition(Position p, Snake snake) {
    if (!isValidPosition(p)) {
      return NULL;
    }

    char old_val = live_grid[p.getRow()][p.getCol()];

    if (old_val != WALL && old_val != OCCUPIED) {
      live_grid[p.getRow()][p.getCol()] = OCCUPIED;
      position_snake_map.put(p, snake);
    }

    if (old_val == FOOD) {
      amount_of_food--;
      incrementFoodTimestamp(p);
    }

    return old_val;
  }

  public Snake getSnakeAtPosition(Position p) {
    return position_snake_map.get(p);
  }

  public synchronized void releasePosition(Position p) {
    if (!isValidPosition(p)) {
      return;
    }

    assert(live_grid[p.getRow()][p.getCol()] == OCCUPIED);
    position_snake_map.remove(p);

    live_grid[p.getRow()][p.getCol()] = FREE;
    used_positions.addFirst(p);
  }

  public synchronized void transmuteToFood(Position p) {
    live_grid[p.getRow()][p.getCol()] = FOOD;
    amount_of_food++;
    incrementFoodTimestamp(p);
  }

  public Position getOffsetPosition(Position p, int rows, int cols) {
    int new_row = p.getRow() + rows;
    int new_col = p.getCol() + cols;
    return new Position(new_row, new_col);
  }

  public Position getOffsetPosition(Direction dir, Position p) {
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

  public Collection<ColoredPosition> getAbsolutePositionsInWindow(Collection<ColoredPosition> positions,
    Position topleft, int type, int rows, int cols) {
    int rstart = topleft.getRow();
    int cstart = topleft.getCol();
    int board_cols = getWidth();
    int board_rows = getHeight();

    if (rstart + rows < 0 || cstart + cols < 0 || rstart >= board_rows || cstart >= board_cols) {
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
    for (int i = istart; i < rows && r < board_rows; i++) {
      int c = cstart + jstart;
      for (int j = jstart; j < cols && c < board_cols; j++) {
        if (live_grid[r][c] == type) {
          Snake snake_at_pos = getSnakeAtPosition(new Position(r, c));
          if (snake_at_pos != null) {
            positions.add(new ColoredPosition(r, c, snake_at_pos.getColor()));
          } else {
            positions.add(new ColoredPosition(r, c, null));
          }
        }
        c++;
      }
      r++;
    }

    return positions;
  }

  public Collection<Position> getPositionsInWindow(Position topleft, int type) {
    Collection<Position> positions = new ArrayList();
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
        if (live_grid[r][c] == type) {
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
}
