/**
 * Multisnake server - Snake class.
 * Eugene Marinelli
 */

package Snake;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public abstract class Snake implements Comparable {
  public abstract boolean readyForReset();
  public abstract boolean reset();
  public abstract Snake step(Set<Position> collision_set, Map<Position, Snake> single_unit_map);
  public abstract Position prepareNextHeadPosition();

  protected static final int FOOD_REWARD = 1;
  protected static final int DEATH_PENALTY = -5;
  protected static final int KILL_REWARD = 5;
  protected static final int GROWTH_PER_FOOD = 1;

  protected Board board;
  protected String id;
  protected boolean dead;
  protected SnakeConnection conn;
  protected Position last_head_pos, head_pos, next_head_pos;
  protected Snake killer;
  protected LinkedList<Position> pos_queue;
  protected boolean kill_reported;
  protected SnakeColor color;
  protected int kills, deaths, food_eaten;
  protected int remaining_growth;
  protected Direction direction;

  public Snake(Board board) {
    this.board = board;
    this.kills = this.deaths = this.food_eaten = 0;
    killer = null;
    pos_queue = new LinkedList<Position>();
  }

  public int getScore() {
    return KILL_REWARD * this.kills + FOOD_REWARD * this.food_eaten + DEATH_PENALTY * this.deaths;
  }

  public synchronized void clearBody() {
    Position p;
    while ((p = this.pos_queue.poll()) != null) {
      this.board.releasePosition(p);
    }
  }

  protected void addHeadPosition(Position p) {
    this.pos_queue.offer(head_pos);
  }

  protected void resolveTailPositions() {
    if (remaining_growth > 0) {
      this.remaining_growth--;
    } else {
      Position old_pos = this.pos_queue.poll();
      this.board.releasePosition(old_pos);
    }
  }

  protected void handleSelfDeath() {
    if (!this.dead) {
      this.deaths++;
      this.dead = true;
    }
  }

  public int compareTo(Object other) {
    Snake other_snake = (Snake)other;
    int other_score = other_snake.getScore();
    if (other_score > this.getScore()) { // Want to sort from high to low score.
      return 1;
    } else { // Returning 0 causes stuff to get overwritten or something in sorted set.
      return -1;
    }
  }

  protected void growBy(int amt) {
    this.remaining_growth += amt;
    this.pos_queue.offer(head_pos);
  }

  public void registerKill(Snake other) {
    this.kills++;
    this.growBy(KILL_REWARD);
    System.out.println("kill - score is now" + getScore());
  }

  public void gotEatenBy(Snake eater) {
    this.killer = eater;
    this.handleKilledBy(this.killer);
  }

  protected void handleKilledBy(Snake killer) {
    assert(killer != this);
    assert(killer != null);

    if (!this.dead) {
      this.deaths++;
      this.dead = true;
      this.killer = killer;
      this.clearBody();
    }
  }

  public void roundReset() {
    this.kills = this.deaths = this.food_eaten = 0;
    this.reset();
  }

  protected Snake stepCommon(Set<Position> collision_set, Map<Position, Snake> single_unit_map) {
    if (this.killer != null) {
      // Already got eaten - don't step.
      return this.killer;
    }

    if (this.getLength() > 1) {
      // If we are of length > 1, then see if our next head position is in the set of single units that will be in
      // that position in the next step.  If there is one, we eat it and it dies.

      Snake prey = single_unit_map.get(this.next_head_pos);
      if (prey != null) {
        // There is a single unit snake in our next head position - we eat it.
        prey.clearBody();
        this.board.grabPosition(this.next_head_pos, this);
        this.head_pos = this.next_head_pos;

        System.out.println("eating "+ prey.getId());
        prey.gotEatenBy(this);

        this.resolveTailPositions();
        return null;
      }
    }

    // Check for head-to-head collision in which both heads would occupy the same position.
    if (collision_set.contains(this.next_head_pos)) {
      Snake single = single_unit_map.get(this.next_head_pos);
      if (this.getLength() > 1 || single == this || single == null) { //???
        System.out.println("handle self death in Snake.step, snake is " + id);

        this.handleSelfDeath();
        return this;
      } else {
        // We are a single-unit and we ran into a single-unit stationary -- it killed us.
        this.handleKilledBy(single);
        return single;
      }
    }

    // Try to get new position in game board.  React to the previous value of that position.
    Board.CellStatus pos_val = board.grabPosition(next_head_pos, this);
    switch (pos_val) {
    case FREE:
      // Successfully occupied position.
      head_pos = next_head_pos;
      addHeadPosition(head_pos);
      break;
    case FOOD:
      head_pos = next_head_pos;
      food_eaten++;
      growBy(FOOD_REWARD);
      break;
    case OCCUPIED:
      Snake other = board.getSnakeAtPosition(next_head_pos);

      if (this.getLength() != 1) {
        if (other == this) {
          // Ran into ourself.
          this.handleSelfDeath();
          return this;
        } else {
          if (other == null || other.getLength() == 1) {
            if (other != null) {
              System.out.println("eating(2) " + other.getId());

              other.clearBody();
              other.gotEatenBy(this);
            }

            this.board.grabPosition(next_head_pos, this);
            this.head_pos = next_head_pos;
          } else {
            this.handleKilledBy(other);
            return other;
          }
        }
      } else {
        // Length of this is 1.
        if (other != null) {
          this.handleKilledBy(other);
          return other;
        } else {
          assert(false);
          handleSelfDeath();  // TODO emarinel this case should not occur!
          return this;
        }
      }
      break;
    case WALL:
    case NULL:
      // Ran into wall or outer space -> DIE.
      this.handleSelfDeath();
      return this;
    }

    resolveTailPositions();
    return null;
  }

  public SnakeConnection getConnection() {
    return conn;
  }

  public int getLength() {
    return this.pos_queue.size();
  }

  public Snake getKiller() {
    return this.killer;
  }

  public void setKillReported() {
    this.kill_reported = true;
  }

  public boolean getKillReported() {
    return this.kill_reported;
  }

  public String getId() {
    return this.id;
  }

  public void setColor(SnakeColor c) {
    this.color = c;
  }

  public SnakeColor getColor() {
    return this.color;
  }

  public boolean isDead() {
    return this.dead;
  }
}
