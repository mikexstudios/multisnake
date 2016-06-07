/**
 * Eugene Marinelli
 * Multisnake - SnakeColor class.
 */

package Snake;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SnakeColor {
  public static final SnakeColor BLUE = new SnakeColor(5);
  public static final SnakeColor BROWN = new SnakeColor(6);
  public static final SnakeColor DARKBLUE = new SnakeColor(7);
  public static final SnakeColor DARKGRAY = new SnakeColor(8);
  public static final SnakeColor DARKGREEN = new SnakeColor(9);
  public static final SnakeColor GREEN = new SnakeColor(0xa);
  public static final SnakeColor ORANGE = new SnakeColor(0xb);
  public static final SnakeColor PINK = new SnakeColor(0xc);
  public static final SnakeColor PURPLE = new SnakeColor(0xd);
  public static final SnakeColor YELLOW = new SnakeColor(0xe);

  private static final SnakeColor[] COLORS = {BLUE, BROWN, DARKBLUE, DARKGRAY, DARKGREEN, GREEN, ORANGE, PINK, PURPLE,
    YELLOW};

  private static Map<Integer, SnakeColor> strColorMap;
  private static boolean map_inited = false;
  private static Random rand = new Random();

  private int id;

  private SnakeColor(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static SnakeColor fromInt(int id) {
    if (!map_inited) {
      strColorMap = new HashMap<Integer, SnakeColor>();
      for (SnakeColor c : COLORS) {
        strColorMap.put(c.getId(), c);
      }
      map_inited = true;
    }

    return strColorMap.get(id);
  }

  public String toString() {
    return Integer.toString(this.id, 16);
  }

  public static SnakeColor random() {
    return COLORS[rand.nextInt(COLORS.length)];
  }
}
