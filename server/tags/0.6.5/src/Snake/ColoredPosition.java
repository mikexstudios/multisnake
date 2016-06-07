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

  public String toString() {
    return "(" + Integer.toString(row) + "," + Integer.toString(col) + "," + color + ")";
  }
}
