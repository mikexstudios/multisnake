/**
 * Multisnake server - Snake class.
 * Eugene Marinelli
 */

package supplelabs.multisnake;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public abstract class Snake implements Comparable<Snake> {
  public abstract boolean readyForReset();
  public abstract Position prepareNextHeadPosition();
  public abstract void sendGameData();
  public abstract void sendNumClients(int numclients);
  public abstract void send(String msg);
  public abstract void reportRoundEnd(String s, int rank);
  public abstract void flush_connection();
  public abstract void disconnect();
  public abstract void sendRoundStatus(SnakeServer.GameMode mode, long time_remaining);
  public abstract boolean disconnected();
  public abstract String getIP();
  protected abstract Direction direction();

  protected static final int FOOD_REWARD = 1;
  protected static final int DEATH_PENALTY = -5;
  protected static final int KILL_REWARD = 5;
  protected static final int GROWTH_PER_FOOD = 1;

  protected Board board;
  protected String id;
  protected boolean dead;
  protected Position next_head_pos;
  protected LinkedList<Position> pos_queue;
  protected SnakeColor color;
  protected int kills, deaths, food_eaten; // Scoring
  protected int remaining_growth;

  protected Collection<Snake> killed_this_step;

    public SnakeConnection conn;

  public Snake(Board board) {
    this.board = board;
    this.kills = this.deaths = this.food_eaten = 0;
    this.pos_queue = new LinkedList<Position>();
    this.killed_this_step = new ArrayList<Snake>(5);
    this.next_head_pos = null;
  }

  public void setBoard(Board board) {
    this.board = board;
  }

  public int getScore() {
    return KILL_REWARD * this.kills + FOOD_REWARD * this.food_eaten + DEATH_PENALTY * this.deaths;
  }

  public synchronized void clearBody() {
    if (SnakeServer.DEBUG_MODE) {
      this.checkConsistency();
    }

    Position p;
    while ((p = this.pos_queue.poll()) != null) {
      this.leavePosition(p);
    }

    if (SnakeServer.DEBUG_MODE) {
      this.checkConsistency();
      assert(this.pos_queue.isEmpty());
    }
  }

  public void handleKilledBy(Snake other) {
    this.dead = true;
    this.deaths++;
  }

  public boolean reset() {
    assert(this.readyForReset());
    assert(this.pos_queue.isEmpty());

    // Snake starts with just a head.  Occupy a random initial position.
    Position p = this.occupyRandomPosition();
    if (p == null) {
      System.out.println("Failed to occupy random position!");
      return false;
    }

    this.remaining_growth = 0;
    this.next_head_pos = null;

    if (SnakeServer.DEBUG_MODE) {
      assert(this.getLength() == 1);
      assert(this.occupiesPosition(p));
      assert (board.getSnakeAtPosition(p) == this);

      this.checkConsistency();
    }

    return true;
  }

  public void handleRoundEnd() {
    this.clearBody();
    this.dead = true;
  }

  protected Position head_pos() {
    assert(this.pos_queue != null);

    if (this.pos_queue.isEmpty()) {
      return null;
    } else {
      return this.pos_queue.getLast();
    }
  }

  public void addKilledSnake(Snake other) {
    if (other != this) {
      this.kills++;
      this.growBy(KILL_REWARD);
    }
    this.killed_this_step.add(other);
  }

  private void addHeadPosition(Position p) {
    if (SnakeServer.DEBUG_MODE) {
      this.checkConsistency();
    }

    assert(!this.occupiesPosition(p)) : "Already occupy position " + p + ", pos q is " + pos_queue;
    this.pos_queue.offer(p);
    assert(this.occupiesPosition(p)) : "even though we just offered it, do not occupy position " + p + ", pos q is " + pos_queue;

    // Snake may be out of sync with board at this point, so consistency will fail.
  }

  private void leavePosition(Position p) {
    if (SnakeServer.DEBUG_MODE) {
      this.checkConsistency();
    }

    assert(p != null) : "old_pos is null for " + this;
    assert(board.getSnakeAtPosition(p) == this) : "this is " + this + "; position is " + p + "; snake at this pos is " + board.getSnakeAtPosition(p);

    this.board.releasePosition(p);

    if (SnakeServer.DEBUG_MODE) {
      this.checkConsistency();
    }
  }

  public void resolveTailPositions() {
    if (SnakeServer.DEBUG_MODE) {
      assert(this.getLength() >= 1);
      this.checkConsistency();
      this.board.checkBoardConsistency();
    }

    if (!pos_queue.isEmpty()) {
      if (remaining_growth > 0) {
        this.remaining_growth--;
      } else {
        Position old_pos = this.pos_queue.poll();
        this.leavePosition(old_pos);
      }
    }

    if (SnakeServer.DEBUG_MODE) {
      assert(this.getLength() > 0);
      this.checkConsistency();
    }
  }

  @Override
  public String toString() {
    return this.id + "("+ this.getLength() +")";
  }

  public int compareTo(Snake other_snake) {
    int other_score = other_snake.getScore();
    if (other_score > this.getScore()) { // Want to sort from high to low score.
      return 1;
    } else { // Returning 0 causes stuff to get overwritten or something in sorted set.
      return -1;
    }
  }

  protected void growBy(int amt) {
    this.remaining_growth += amt;
  }

  public void roundReset() {
    this.kills = this.deaths = this.food_eaten = 0;
    this.reset();
  }

  public Collection<Snake> snakesKilledThisStep() {
    return this.killed_this_step;
  }

  public void clearKills() {
    this.killed_this_step.clear();
  }

  protected Position occupyRandomPosition () {
    Position p = board.occupyRandomOpenPosition(this);
    if (p == null) { // No open positions.
      System.out.println("No open positions for " + this);
      return null;
    } else {
      this.addHeadPosition(p);

      if (SnakeServer.DEBUG_MODE) {
        assert(this.occupiesPosition(p));
        assert(board.getSnakeAtPosition(p) == this);
        this.checkConsistency();
      }

      return p;
    }
  }

  protected void occupyPosition(Position p) {
    if (SnakeServer.DEBUG_MODE) {
      this.checkConsistency();

      assert(board.getSnakeAtPosition(p) != this);
      assert(!this.occupiesPosition(p));

      if (pos_queue.size() != 0) {
        int coldiff = p.getCol() - head_pos().getCol();
        int rowdiff = p.getCol() - head_pos().getCol();
        assert(coldiff == 0 || coldiff == -1 || coldiff == 1);
        assert(rowdiff == 0 || rowdiff == -1 || rowdiff == 1);
        assert(!p.equals(head_pos()));
      }
    }

    this.addHeadPosition(p);
    this.board.grabPosition(p, this);

    if (SnakeServer.DEBUG_MODE) {
      assert(this.occupiesPosition(p));
      assert(board.getSnakeAtPosition(p) == this);
      this.checkConsistency();
    }
  }

  public void handleFood() {
    this.food_eaten++;
    this.growBy(FOOD_REWARD);
  }

  public void commitMove() {
    assert(this.getLength() >= 1);

    this.occupyPosition(this.next_head_pos);
    this.resolveTailPositions();
  }

  public int getLength() {
    //assert(this.pos_queue.size() > 0);
    return this.pos_queue.size();
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

  public boolean occupiesPosition(Position p) {
    return pos_queue.contains(p);
  }

  public Queue<Position> getPosQueue() {
    return pos_queue;
  }

  public boolean moving() {
    return this.direction() != null;
  }

  private void checkConsistency() {
    if (pos_queue.isEmpty()) {
      return;
    }

    Iterator<Position> itr = pos_queue.iterator();
    Position cur = itr.next();

    while (itr.hasNext()) {
      Position p = itr.next();

      assert(this.board.getSnakeAtPosition(p) == this) : "Snake at our position " + p + " is not us("+ this +"): type is " + this.board.getBoardValue(p).getStatus() + " snake is " + this.board.getBoardValue(p).getSnake();

      assert(!cur.equals(p)) : "repeat position in snake! " + p + "," + cur;

      int rowdiff = p.getRow() - cur.getRow();
      int coldiff = p.getCol() - cur.getCol();

      String err = p + " does not match up with " + cur;
      assert(rowdiff == -1 || rowdiff == 0 || rowdiff == 1) : err;
      assert(coldiff == -1 || coldiff == 0 || coldiff == 1) : err;
      cur = p;
    }
  }
}
