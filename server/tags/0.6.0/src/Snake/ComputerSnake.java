/**
 * @author Eugene Marinelli
 */

package Snake;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ComputerSnake extends Snake {
  private int remaining_growth;
  private Random randgen;
  private Direction direction;

  private static final double TURN_PROBABILITY = 0.15;

  public ComputerSnake(Board board, String id) {
    System.out.println("Bot: " + id);
    this.board = board;
    dead = true;
    pos_queue = new LinkedList<Position>();
    randgen = new Random();
    this.id = id;
    conn = null;
    eater = null;
    color = SnakeColor.random();
  }

  public synchronized void clearBody() {
    Position p;
    while ((p = pos_queue.poll()) != null) {
      //board.transmuteToFood(p);
      board.releasePosition(p);
    }

    last_head_pos = null;
    head_pos = null;
    next_head_pos = null;
  }

  public void gotEatenBy(Snake eater) {
    this.eater = eater;
    handleKilledBy(eater);
  }

  public boolean readyForReset() {
    return dead;
  }

  public void roundReset() {
    score = 0;
    reset();
  }

  public boolean reset() {
    clearBody();

    // Snake starts with just a head.  Occupy a random initial position.
    head_pos = board.occupyRandomOpenPosition();
    if (head_pos == null) { // No open positions.
      return false;
    }

    last_head_pos = head_pos;
    pos_queue.offer(head_pos);

    remaining_growth = 0;
    dead = false;
    next_head_pos = null;
    direction = Direction.NORTH;
    eater = null;
    kill_reported = false;

    return true;
  }

  public Position prepareNextHeadPosition() {
    if (dead) {
      return null;
    }

    Position next_pos_current_dir = head_pos.getDirectionOffset(direction);
    int board_val = board.getBoardValue(next_pos_current_dir.getRow(), next_pos_current_dir.getCol());
    if (board_val == Board.FOOD) { // Definitely try to get the food.
      next_head_pos = next_pos_current_dir;
      return next_head_pos;
    }

    Direction left_dir = direction.getLeft(), right_dir = direction.getRight();
    Position left = head_pos.getDirectionOffset(left_dir);
    board_val = board.getBoardValue(left.getRow(), left.getCol());
    boolean left_open = false;
    if (board_val == Board.FOOD) {
      next_head_pos = left;
      return next_head_pos;
    } else if (board_val == Board.FREE) {
      left_open = true;
    }

    Position right = head_pos.getDirectionOffset(right_dir);
    board_val = board.getBoardValue(right.getRow(), right.getCol());
    boolean right_open = false;
    if (board_val == Board.FOOD) {
      next_head_pos = right;
      return next_head_pos;
    } else if (board_val == Board.FREE) {
      right_open = true;
    }

    double float_test = randgen.nextDouble();
    if (randgen.nextDouble() > TURN_PROBABILITY) {
      // Try to go straight
      board_val = board.getBoardValue(next_pos_current_dir.getRow(), next_pos_current_dir.getCol());
      if (board_val == Board.FREE || board_val == Board.FOOD) {
        next_head_pos = next_pos_current_dir;
        return next_head_pos;
      }
    }

    // Else, next position in this direction is occupied, so turn left or right.
    if (left_open && !right_open) {
      direction = left_dir;
      next_head_pos = left;
    } else if (right_open && !left_open) {
      direction = right_dir;
      next_head_pos = right;
    } else if (!right_open && !left_open) {
      next_head_pos = next_pos_current_dir;
    } else {
      if ((randgen.nextInt() & 1) == 0) {
        direction = right_dir;
        next_head_pos = right;
      } else {
        direction = left_dir;
        next_head_pos = left;
      }
    }

    return next_head_pos;
  }

  public void registerKill(String other_id) {
    score += KILL_REWARD;
  }

  protected void handleKilledBy(Snake killer) {
    if (dead) {
      return;
    }

    score -= DEATH_PENALTY;
    dead = true;

    killer.registerKill(id);
  }

  protected void handleSelfDeath() {
    if (dead) {
      return;
    }

    score -= DEATH_PENALTY;
    dead = true;
  }

  protected void resolveTailPositions() {
    if (remaining_growth > 0) {
      remaining_growth--;
    } else {
      Position old_pos = pos_queue.poll();
      board.releasePosition(old_pos);
    }
  }

  protected void growBy(int amt) {
    score += amt;
    remaining_growth += amt;
    pos_queue.offer(head_pos);
  }

  protected void addHeadPosition(Position p) {
    pos_queue.offer(head_pos);
  }

  public Snake step(Set<Position> collision_set, Map<Position, Snake> single_unit_map) {
    last_head_pos = head_pos;

    if (next_head_pos != null && head_pos != null && !next_head_pos.equals(head_pos)) {
      return stepCommon(collision_set, single_unit_map);
    } else {
      return null;
    }
  }
}
