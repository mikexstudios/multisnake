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
  private boolean intelligent;
  private Direction direction;

  public ComputerSnake(Board board, String id, boolean intelligent) {
    super(board);

    System.out.println("Bot: " + id);
    this.id = id;
    this.intelligent = intelligent;
    this.dead = true;
    this.randgen = new Random();
    this.color = SnakeColor.random();
  }

  public boolean readyForReset() {
    if (this.dead) {
      assert(this.getLength() == 0);
    }

    return this.dead;
  }

  public boolean reset() {
    if (!super.reset()) {
      return false;
    }

    this.dead = false;
    this.direction = Direction.random();
    return true;
  }

  protected Direction direction() {
    return direction;
  }

  public Position prepareNextHeadPosition() {
    if (this.dead) {
      return null;
    }

    assert(this.getLength() > 0);
    assert(this.head_pos() != null);

    if (this.direction() == null) {
      this.next_head_pos = this.head_pos();
      return this.next_head_pos;
    }

    Position next_pos_current_dir = this.head_pos().getDirectionOffset(this.direction);
    Board.Cell board_val = board.getBoardValue(next_pos_current_dir.getRow(), next_pos_current_dir.getCol());
    if (!this.intelligent || board_val.getStatus() == Board.CellStatus.FOOD) { // Definitely try to get the food.
      this.next_head_pos = next_pos_current_dir;
      return this.next_head_pos;
    }

    Direction left_dir = direction.getLeft(), right_dir = direction.getRight();
    Position left = head_pos().getDirectionOffset(left_dir);
    board_val = board.getBoardValue(left.getRow(), left.getCol());
    boolean left_open = false;
    if (board_val.getStatus() == Board.CellStatus.FOOD) {
      this.next_head_pos = left;
      return next_head_pos;
    } else if (board_val.getStatus() == Board.CellStatus.FREE) {
      left_open = true;
    }

    Position right = head_pos().getDirectionOffset(right_dir);
    board_val = board.getBoardValue(right.getRow(), right.getCol());
    boolean right_open = false;
    if (board_val.getStatus() == Board.CellStatus.FOOD) {
      next_head_pos = right;
      return next_head_pos;
    } else if (board_val.getStatus() == Board.CellStatus.FREE) {
      right_open = true;
    }

    double float_test = randgen.nextDouble();
    if (randgen.nextDouble() > TURN_PROBABILITY) {
      // Try to go straight
      board_val = board.getBoardValue(next_pos_current_dir.getRow(), next_pos_current_dir.getCol());
      if (board_val.getStatus() == Board.CellStatus.FREE || board_val.getStatus() == Board.CellStatus.FOOD) {
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

  public boolean disconnected() {
    return false;
  }

  public void sendGameData() {}
  public void sendNumClients(int numclients) {}
  public void send(String msg) {}
  public void reportRoundEnd(String s, int rank) {}
  public void flush_connection() {}
  public void disconnect() {}
  public void sendRoundStatus(SnakeServer.GameMode mode, long time_remaining) {}
  public String getIP() { return null; }
}
