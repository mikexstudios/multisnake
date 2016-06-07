/**
 */

package Snake;

public class Position implements Comparable {
  private int row;
  private int col;

  public Position(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  public Position getDirectionOffset(Direction direction) {
    int offset_row = row, offset_col = col;
    if (direction == Direction.SOUTH) {
      offset_row = row + 1;
    } else if (direction == Direction.NORTH) {
      offset_row = row - 1;
    } else if (direction == Direction.EAST) {
      offset_col = col + 1;
    } else {
      offset_col = col - 1;
    }

    return new Position(offset_row, offset_col);
  }

  public String toString() {
    return "(" + Integer.toString(row) + "," + Integer.toString(col) + ")";
  }

  public boolean equals(Object o) {
    return compareTo(o) == 0;
  }

  public int compareTo(Object o) {
    Position p = (Position)o;
    if (p.getRow() == row && p.getCol() == col) {
      return 0;
    } else if (row < p.row) {
      return -1;
    } else {
      return 1;
    }
  }

  public int hashCode() {
    return (row<<16) | col;
  }
}
