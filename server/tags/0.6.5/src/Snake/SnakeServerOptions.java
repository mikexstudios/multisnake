/*
 * SnakeServerOptions
 * Saves all options needed to start server
 */

package Snake;

public class SnakeServerOptions {
  public Integer max_rows, max_cols;
  public Integer num_ai;
  public String hostname;
  public String map_filename;
  public String map_directory;
  public Boolean limit_connections_per_ip;
  public Boolean disable_notifs;
  public Integer intermission_duration;
  public Integer round_duration;
  public Boolean debug_mode;
}
