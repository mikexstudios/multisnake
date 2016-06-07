/**
 * Eugene Marinelli
 *
 */

package Snake;

import java.util.Set;

public interface Snake {
  public void clearBody();
  public boolean isDead();
  public Position prepareNextHeadPosition();
  public boolean step(Set<Position> collision_set);
  public void sendGameData();
  public String getId();
  public boolean disconnected();
  public SnakeConnection getConnection();
  public boolean readyForReset();
  public boolean reset();
  public boolean readyToReport();
}
