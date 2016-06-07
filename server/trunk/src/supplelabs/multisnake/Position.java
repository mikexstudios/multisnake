/**
 */

package supplelabs.multisnake;

public class Position implements Comparable<Position> {
  protected int row;
  protected int col;

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

  @Override
  public String toString() {
    return "(" + Integer.toString(row) + "," + Integer.toString(col) + ")";
  }

  public boolean equals(Position o) {
    return compareTo(o) == 0;
  }

  @Override
  public int hashCode() {
    return (row<<16) | col;
  }

  public int compareTo(Position p) {
    if (p.getRow() == row && p.getCol() == col) {
      return 0;
    } else if (row < p.row) {
      return -1;
    } else {
      return 1;
    }
  }
}
