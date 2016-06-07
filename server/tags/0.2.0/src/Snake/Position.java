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

  public int compareTo(Object o) {
    Position p = (Position)o;
    if (row < p.row) {
      return -1;
    } else if (row > p.row) {
      return 1;
    } else {
      return 0;
    }
  }
}
