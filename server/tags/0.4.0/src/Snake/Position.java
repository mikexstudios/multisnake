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
}
