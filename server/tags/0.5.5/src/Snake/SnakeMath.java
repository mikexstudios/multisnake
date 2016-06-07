/**
* Eugene Marinelli
*/

package Snake;

public class SnakeMath {
  // a mod b, always output in the range [0,b).
  public static int mod(int a, int b) {
    int t = a % b;
    if (t < 0) {
      t += b;
    }
    return t;
  }
}
