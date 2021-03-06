/**
 * Eugene Marinelli
 */

package Snake;

public class Direction {
  public static final Direction NORTH = new Direction(0);
  public static final Direction EAST = new Direction(1);
  public static final Direction SOUTH = new Direction(2);
  public static final Direction WEST = new Direction(3);

  private static Direction[] directions = {NORTH, EAST, SOUTH, WEST};

  private int dir;
  private Direction(int dir) {
    this.dir = dir;
  }

  public Direction getOpposite() {
    return directions[(dir + 2) % 4];
  }

  public String toString() {
    switch (dir) {
    case 0:
      return "NORTH";
    case 1:
      return "EAST";
    case 2:
      return "SOUTH";
    case 3:
      return "WEST";
    }
    
    return "NOT-A-DIRECTION";
  }
}
