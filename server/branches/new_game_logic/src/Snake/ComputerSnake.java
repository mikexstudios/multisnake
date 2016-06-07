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
  private Random randgen;
  private static final double TURN_PROBABILITY = 0.15;

  public ComputerSnake(Board board, String id) {
    super(board);

    System.out.println("Bot: " + id);
    this.id = id;
    this.dead = true;
    this.randgen = new Random();
    this.conn = null;
    this.color = SnakeColor.random();
  }

  public boolean readyForReset() {
    return this.dead;
  }

  public boolean reset() {
    this.clearBody();

    // Snake starts with just a head.  Occupy a random initial position.
    this.head_pos = board.occupyRandomOpenPosition();
    if (head_pos == null) { // No open positions.
      return false;
    }

    this.last_head_pos = head_pos;
    this.pos_queue.offer(head_pos);

    this.remaining_growth = 0;
    this.dead = false;
    this.next_head_pos = null;

    //this.direction = Direction.NORTH;
    this.direction = null;

    this.killer = null;
    this.kill_reported = false;

    return true;
  }

  public Position prepareNextHeadPosition() {
    if (this.dead) {
      return null;
    }

    if (this.direction == null) {
      this.next_head_pos = this.head_pos;
      return this.next_head_pos;
    }

    Position next_pos_current_dir = this.head_pos.getDirectionOffset(this.direction);
    Board.CellStatus board_val = board.getBoardValue(next_pos_current_dir.getRow(), next_pos_current_dir.getCol());
    if (board_val == Board.CellStatus.FOOD) { // Definitely try to get the food.
      this.next_head_pos = next_pos_current_dir;
      return this.next_head_pos;
    }

    Direction left_dir = direction.getLeft(), right_dir = direction.getRight();
    Position left = head_pos.getDirectionOffset(left_dir);
    board_val = board.getBoardValue(left.getRow(), left.getCol());
    boolean left_open = false;
    if (board_val == Board.CellStatus.FOOD) {
      this.next_head_pos = left;
      return next_head_pos;
    } else if (board_val == Board.CellStatus.FREE) {
      left_open = true;
    }

    Position right = head_pos.getDirectionOffset(right_dir);
    board_val = board.getBoardValue(right.getRow(), right.getCol());
    boolean right_open = false;
    if (board_val == Board.CellStatus.FOOD) {
      next_head_pos = right;
      return next_head_pos;
    } else if (board_val == Board.CellStatus.FREE) {
      right_open = true;
    }

    double float_test = randgen.nextDouble();
    if (randgen.nextDouble() > TURN_PROBABILITY) {
      // Try to go straight
      board_val = board.getBoardValue(next_pos_current_dir.getRow(), next_pos_current_dir.getCol());
      if (board_val == Board.CellStatus.FREE || board_val == Board.CellStatus.FOOD) {
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

  public Snake step(Set<Position> collision_set, Map<Position, Snake> single_unit_map) {
    last_head_pos = head_pos;

    if (next_head_pos != null && head_pos != null && !next_head_pos.equals(head_pos)) {
      return stepCommon(collision_set, single_unit_map);
    } else {
      return null;
    }
  }
}
