/* Eugene Marinelli
 * Multisnake - ArrayGrid class.
 */

package supplelabs.multisnake;

public class ArrayGrid<T> implements Grid<T> {
  private T[][] grid;

  public ArrayGrid(int rows, int cols) {
    grid = (T[][]) (new Object[rows][cols]);
  }

  /*
  private boolean in_grid(int row, int col) {
    return (row >= 0 && row < rows() && col >= 0 && col < columns());
  }*/

  public T get(int row, int col) {
    return grid[row][col];
  }

  public void set(int row, int col, T t) {
    grid[row][col] = t;
  }

  public int rows() {
    return grid.length;
  }

  public int columns() {
    return grid[0].length;
  }
}
