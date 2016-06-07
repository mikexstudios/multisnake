/**
 *
 */

package Snake;

public class Direction {
  public static final Direction NORTH = new Direction(0);
  public static final Direction EAST = new Direction(1);
  public static final Direction SOUTH = new Direction(2);
  public static final Direction WEST = new Direction(3);

  private int dir;
  private Direction(int dir) {
    this.dir = dir;
  }
}
