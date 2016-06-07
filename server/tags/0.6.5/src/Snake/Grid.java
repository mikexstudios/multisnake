/* Eugene Marinelli
 * Multisnake - Grid interface.
 */

package Snake;

public interface Grid<T> {
  public T get(int row, int col);
  public void set(int row, int col, T t);
  public int rows();
  public int columns();
}
