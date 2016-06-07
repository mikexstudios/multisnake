/**
 */

package Snake;

public class ColoredPosition extends Position {
  SnakeColor color;

  public ColoredPosition(int row, int col, SnakeColor color) {
    super(row, col);
    this.color = color;
  }

  public SnakeColor getColor() {
    return color;
  }

/*
  public int compareTo(Object o) {
    ColoredPosition p = (ColoredPosition)o;
    if (p.getRow() == row && p.getCol() == col && p.getColor() == color) {
      return 0;
    } else if (row < p.getRow()) {
      return -1;
    } else {
      return 1;
    }
  }
  */

  public String toString() {
    return "(" + Integer.toString(row) + "," + Integer.toString(col) + "," + color + ")";
  }
}
