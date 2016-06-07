/**
 * @author Eugene Marinelli
 */

package Snake;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

public final class ComputerSnake implements Snake {
  private Board board;
  private boolean dead;
  private LinkedList<Position> pos_queue;
  private Position head_pos, last_head_pos, next_head_pos;
  private int remaining_growth;
  private Random randgen;
  private Direction direction;

  private static final int GROWTH_PER_FOOD = 1;
  private static final double TURN_PROBABILITY = 0.15;

  public ComputerSnake(Board board) {
    this.board = board;
    dead = true;
    pos_queue = new LinkedList<Position>();
    randgen = new Random();
  }

  public synchronized void clearBody() {
    Position p;
    while ((p = pos_queue.poll()) != null) {
      //board.transmuteToFood(p);
      board.releasePosition(p);
    }
  }

  public boolean isDead() {
    return dead;
  }

  public boolean readyForReset() {
    return dead;
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

    return true;
  }

  public Position prepareNextHeadPosition() {
    if (head_pos == null) {
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

  public boolean step(Set<Position> collision_set) {
    last_head_pos = head_pos;

    if (next_head_pos != null && !next_head_pos.equals(head_pos)) {
      // Check for head-to-head collision in which both heads would occupy the same position.
      if (collision_set.contains(next_head_pos)) {
        dead = true;
        return false;
      }

      // Try to get new position in game board.  React to the previous value of that position.
      char pos_val = board.grabPosition(next_head_pos);
      switch (pos_val) {
      case Board.FREE:
        // Successfully occupied position.
        head_pos = next_head_pos;
        pos_queue.offer(head_pos);
        break;
      case Board.FOOD:
        // Occupied position, grow.
        remaining_growth += GROWTH_PER_FOOD;
        head_pos = next_head_pos;
        pos_queue.offer(head_pos);
        break;
      case Board.OCCUPIED:
      case Board.WALL:
      case Board.NULL:
        // Ran into wall or snake body -> DIE.
        dead = true;
        return false; // Indicates that snake just died.
      }

      if (remaining_growth > 0) {
        remaining_growth--;
      } else {
        Position old_pos = pos_queue.poll();
        board.releasePosition(old_pos);
      }
    }

    return true;
  }

  public String getId() {
    return "Bot";
  }

  public boolean readyToReport() {
    return true;
  }

  public void sendGameData() {}
  
  public SnakeConnection getConnection() {
    return null;
  }

  public boolean disconnected() {
    return false;
  }
}
