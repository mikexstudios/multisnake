/**
 * Eugene Marinelli
 */

package Snake;

import java.util.Random;

public class Board {
  public static final char WALL = '3';
  public static final char FOOD = '2';
  public static final char OCCUPIED = '1';
  public static final char FREE = '0';

  // Static height and width for now, dynamic later.
  public static final int COLS = 100;
  public static final int ROWS = 100;
  private char[][] grid;

  private int amount_of_food;

  public Board() {
    amount_of_food = 0;

    // Free entire board.
    grid = new char[ROWS][COLS];
    for (int i = 0; i < ROWS; i++) {
      for (int j = 0; j < COLS; j++) {
        if (i == 0 || j == 0 || i == ROWS-1 || j == COLS-1) {
          grid[i][j] = WALL;
        } else {
          grid[i][j] = FREE;
        }
      }
    }
  }

  public Position occupyRandomOpenPosition() {
    return fillRandomOpenPosition(OCCUPIED);
  }

  public synchronized void placeRandomFood() {
    fillRandomOpenPosition(FOOD);
    amount_of_food++;
  }

  public synchronized Position fillRandomOpenPosition(char fill_type) {
    int r, c;
    Random rg = new Random();

    do {
      r = mod(rg.nextInt(), ROWS);
      c = mod(rg.nextInt(), COLS);
    } while(grid[r][c] != FREE);

    grid[r][c] = fill_type;

    return new Position(r, c);
  }

  private boolean isValidPosition(Position p) {
    return p != null && p.getRow() >= 0 && p.getRow() < ROWS && p.getCol() >= 0 && p.getRow() < COLS;
  }

  public synchronized char grabPosition(Position p) {
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
    assert(grid[p.getRow()][p.getCol()] == OCCUPIED);
    grid[p.getRow()][p.getCol()] = FREE;
  }

  // a mod b, always in the range [0,b)
  private static int mod(int a, int b) {
    int t = a % b;
    if (t < 0) {
      t += b;
    }

    return t;
  }

  public static Position getOffsetPosition(Position p, int rows, int cols) {
    int new_row = mod(p.getRow() + rows, ROWS);
    int new_col = mod(p.getCol() + cols, COLS);
    return new Position(new_row, new_col);
  }

  public Position getOffsetPosition(Direction dir, Position p) {
    assert(dir != null);
    //assert(isValidPosition(p));
    
    if (dir == Direction.NORTH) {
      return new Position(mod(p.getRow() - 1, ROWS), p.getCol());
    } else if (dir == Direction.SOUTH) {
      return new Position(mod(p.getRow() + 1, ROWS), p.getCol());
    } else if (dir == Direction.WEST) {
      return new Position(p.getRow(), mod(p.getCol() - 1, COLS));
    } else if (dir == Direction.EAST) {
      return new Position(p.getRow(), mod(p.getCol() + 1, ROWS));
    } else {
      return null;
    }
  }

  public synchronized char[][] getTopLeftWindow(Position topleft, int rows, int cols) {
    char[][] window = new char[rows][cols];
    int rstart = topleft.getRow();
    int cstart = topleft.getCol();

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        window[i][j] = grid[mod(rstart + i, ROWS)][mod(cstart + j, COLS)];
      }
    }

    return window;
  }

  public synchronized char[][] getCenterWindow(Position center, int rows, int cols, int roffset, int coffset) {
    char[][] window = new char[rows][cols];
    
    int rstart = mod(center.getRow() - roffset, ROWS);
    int cstart = mod(center.getCol() - coffset, COLS);

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        window[i][j] = grid[mod(rstart + i, ROWS)][mod(cstart + j, COLS)];
      }
    }

    return window;
  }

  public synchronized int getFoodAmount() {
    return amount_of_food;
  }
}
