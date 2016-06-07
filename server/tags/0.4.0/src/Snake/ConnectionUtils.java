/**
* Eugene Marinelli
*/

package Snake;

import java.io.BufferedReader;
import java.io.IOException;

public final class ConnectionUtils {
  /* Copied from master policy server example. */
  public static String read(BufferedReader in) {
    StringBuffer buffer = new StringBuffer();
    boolean zeroByteRead = false;

    try {
      do {
        int codePoint = in.read();
        if (codePoint == 0) {
          zeroByteRead=true;
        } else {
          try {
            buffer.appendCodePoint(codePoint);
          } catch (IllegalArgumentException e) {
            // This generally occurs when the client suddenly disconnects.  Return empty string to signify broken
            // connection.
            return "";
          }
        }
      } while (!zeroByteRead && buffer.length() < 50);
    } catch (IOException e) {}

    return buffer.toString();
  }
}
