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
  private static final int COLS = 100;
  private static final int ROWS = 100;
  private char[][] grid;

  public Board() {
    // Free entire board.
    grid = new char[ROWS][COLS];
    for (int i = 0; i < ROWS; i++) {
      for (int j = 0; j < COLS; j++) {
        if (i == 0 || j == 0) {
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

  public void placeRandomFood() {
    fillRandomOpenPosition(FOOD);
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

  public synchronized char grabPosition(Position p) {
    char old_val = grid[p.getRow()][p.getCol()];

    if (old_val != WALL && old_val != OCCUPIED) {
      grid[p.getRow()][p.getCol()] = OCCUPIED;
    }

    return old_val;
  }

  public synchronized void releasePosition(Position p) {
    assert(grid[p.getRow()][p.getCol()] == OCCUPIED);
    grid[p.getRow()][p.getCol()] = FREE;
  }

  // a mod b, always in the range [0,b)
  private int mod(int a, int b) {
    int t = a % b;
    if (t < 0) {
      t += b;
    }
    return t;
  }

  public Position getOffsetPosition(Direction dir, Position p) {
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

  public synchronized char[][] getWindow(Position center, int rows, int cols, int roffset, int coffset) {
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
}
