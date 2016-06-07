/**
 * Multisnake server - Snake class.
 * Eugene Marinelli
 */

package Snake;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public abstract class Snake implements Comparable {
  public abstract void clearBody();
  public abstract boolean readyForReset();
  public abstract boolean reset();
  public abstract Snake step(Set<Position> collision_set, Map<Position, Snake> single_unit_map);
  public abstract void registerKill(String other_id);
  public abstract void gotEatenBy(Snake eater);
  public abstract Position prepareNextHeadPosition();

  protected abstract void handleKilledBy(Snake killer);
  protected abstract void handleSelfDeath();
  protected abstract void resolveTailPositions();
  protected abstract void growBy(int amt);
  protected abstract void addHeadPosition(Position p);

  protected static final int FOOD_REWARD = 1;
  protected static final int DEATH_PENALTY = 5;
  protected static final int KILL_REWARD = 5;
  protected static final int GROWTH_PER_FOOD = 1;

  protected Board board;
  protected int score;
  protected String id;
  protected boolean dead;
  protected SnakeConnection conn;
  protected Position last_head_pos, head_pos, next_head_pos;
  protected Snake eater;
  protected LinkedList<Position> pos_queue;
  protected boolean kill_reported;

  protected SnakeColor color;

  public int getLength() {
    return pos_queue.size();
  }

  public Snake getEater() {
    return eater;
  }

  public void setKillReported() {
    kill_reported = true;
  }

  public boolean getKillReported() {
    return kill_reported;
  }

  public String getId() {
    return id;
  }

  public boolean isDead() {
    return dead;
  }

  public int getScore() {
    return score;
  }

  public int compareTo(Object other) {
    Snake other_snake = (Snake)other;
    int other_score = other_snake.getScore();
    if (other_score > score) { // Want to sort from high to low score.
      return 1;
    } else { // Returning 0 causes stuff to get overwritten or something in sorted set.
      return -1;
    }
  }

  public void setColor(SnakeColor c) {
    this.color = c;
  }

  public SnakeColor getColor() {
    return this.color;
  }

  protected Snake stepCommon(Set<Position> collision_set, Map<Position, Snake> single_unit_map) {
    if (eater != null) {
      return eater;
    }

    if (getLength() > 1) {
      Snake prey = single_unit_map.get(next_head_pos);
      if (prey != null) {
        prey.clearBody();
        board.grabPosition(next_head_pos, this);
        head_pos = next_head_pos;

        growBy(KILL_REWARD);
        prey.gotEatenBy(this);

        resolveTailPositions();
        return null;
      }
    }

    // Check for head-to-head collision in which both heads would occupy the same position.
    if (collision_set.contains(next_head_pos)) {
      Snake single = single_unit_map.get(next_head_pos);
      if (getLength() > 1 || single == this || single == null) {
        handleSelfDeath();
        return this;
      } else {
        // We are a single-unit and we ran into a single-unit stationary -- it killed us.
        handleKilledBy(single);
        return single;
      }
    }

    // Try to get new position in game board.  React to the previous value of that position.
    char pos_val = board.grabPosition(next_head_pos, this);
    switch (pos_val) {
    case Board.FREE:
      // Successfully occupied position.
      head_pos = next_head_pos;
      addHeadPosition(head_pos);
      break;
    case Board.FOOD:
      head_pos = next_head_pos;
      growBy(FOOD_REWARD);
      break;
    case Board.OCCUPIED:
      Snake other = board.getSnakeAtPosition(next_head_pos);

      if (getLength() != 1) {
        if (other == this) {
          // Ran into ourself.
          handleSelfDeath();
          return this;
        } else {
          if (other == null || other.getLength() == 1) {
            if (other != null) {
              other.clearBody();
              other.gotEatenBy(this);
            }

            board.grabPosition(next_head_pos, this);
            head_pos = next_head_pos;
            growBy(KILL_REWARD);
          } else {
            handleKilledBy(other);
            return other;
          }
        }
      } else {
        if (other != null) {
          handleKilledBy(other);
          return other;
        } else {
          assert(false);
          handleSelfDeath();  // TODO emarinel this case should not occur!
          return this;
        }
      }
      break;
    case Board.WALL:
    case Board.NULL:
      // Ran into wall or outer space -> DIE.
      handleSelfDeath();
      return this;
    }

    resolveTailPositions();
    return null;
  }

  public SnakeConnection getConnection() {
    return conn;
  }
}
