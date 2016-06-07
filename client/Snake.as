/**
 * Multisnake - Snake class.
 * Michael Huynh (http://www.mikexstudios.com)
 */

class Snake {
	private var position:Object = {x:0, y:0};
	private var focal_offset:Object = {x:0, y:0};

	private var dir_buffer : KeyBuffer;
	private var direction : Direction;
	private var length : Number;
	private var tile : Tile;

	public function Snake() {
		this.dir_buffer = new KeyBuffer();
		this.direction = undefined;
		this.length = 1;
		this.tile = Tile.GREEN;
	}

	public function set_tile(t:Tile):Void {
		this.tile = t;
	}

	public function get_tile():Tile {
		return this.tile;
	}

	public function set_random_direction():Void {
		this.direction = Direction.random();

		//Send the initial direction to the server
		Main.app.connection.sendDirection(direction);
	}

	public function push_move(dir:Direction) : Void {
		this.dir_buffer.push(dir);
		this.move();
	}

	/**
	 * While the server handles the growing of the snake. This function
	 * is needed because for snake's length > 1, we can't move backwards
	 * on the keys.
	 *
	 * @param   amount
	 */
	public function grow(amount : Number):Void {
		this.length += amount;
	}

	public function reset():Void {
		this.direction = undefined; // reset snake direction so that any starting direction is possible.
		this.length = 1;
	}

	/**
	 * Called on each iteration, move sends the direction
	 * of the snake to the server.
	 */
	public function move():Void {
		var last_dir:Direction = direction;
		var opposite:Direction = direction.opposite();

		var dir:Direction;
		do {
			dir = Direction(dir_buffer.pop());
		} while (dir != undefined && (dir == direction || (length > 1 && dir == direction.opposite())));

		if (dir != undefined) {
			direction = dir;
		}

		switch (direction) {
			case Direction.NORTH:
				this.setFocalOffset(0,-1);
				break;
			case Direction.SOUTH:
				this.setFocalOffset(0,1);
				break;
			case Direction.WEST:
				this.setFocalOffset(-1,0);
				break;
			case Direction.EAST:
				this.setFocalOffset(1,0);
				break;
			default:
				trace("ERROR Invalid direction");
				return;
		}

		if (last_dir != direction) {
			Main.app.connection.sendDirection(direction);
		}
	}

	public function setPosition(x:Number, y:Number) : Void {
		this.position.x = x;
		this.position.y = y;

		this.setFocalOffset(0, 0);
	}

	private function setFocalOffset(x:Number, y:Number) : Void {
		this.focal_offset.x = x;
		this.focal_offset.y = y;
	}

	public function getFocalPosition() : Object {
		return {x: this.position.x + this.focal_offset.x, y: this.position.y + this.focal_offset.y};
	}
}
