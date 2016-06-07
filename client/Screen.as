/**
 * Multisnake
 */

/**
 * Screen handles what is displayed on the client's screen.  It is comprised of a large movieclip (representing the
 * map) that is updated on new items on the map and is moved around when the snake moves around (saving re-toggling
 * CPU cycles).
 */
class Screen {
	public static var width:Number = 0; //Will be set by Stage.width below
	public static var height:Number = 0; //THESE ARE NOT IN PIXELS.

	private static var width_px:Number;
	private static var height_px:Number;
	private var last_screen:Array = []; //Contains positions for the last displayed screen. Used for diff.
	private var show_direction_tip:Boolean = true;

	private var map_rows:Array;

	public function set_show_direction_tip(s:Boolean) {
		show_direction_tip = s;
	}

	public function Screen() {
		//Get width and height from root
		//We have to assume that all the tiles are the same size.
		width_px = Stage.width;
		height_px = Stage.height;
		width = Math.floor(width_px / Tile.size);
		height = Math.floor(height_px / Tile.size);

		//Acts as a tiles container which holds the full graphical representation of our map.
		this.map_rows = [];

		//Initialize our screen:
		for (var y:Number = 0; y < height; y++) {
			this.last_screen[y] = [];
			this.map_rows[y] = Main.top_mc.createEmptyMovieClip('screen_container' + y, Main.top_mc.getNextHighestDepth());

			for (var x:Number = 0; x < width; x++) {
				this.last_screen[y][x] = Tile.EMPTY;

				this.map_rows[y].attachMovie('tile', x, this.map_rows[y].getNextHighestDepth());
				//Move tile to position
				this.map_rows[y][x]._x = x * Tile.size;
				this.map_rows[y][x]._y = y * Tile.size;
				//Go to correct frame of tile (our tile type)
				this.map_rows[y][x].gotoAndStop(1); //Add one since frames start at 1
			}
		}
	}

	public static function get_width() {
		return width_px;
	}

	public static function get_height() {
		return height_px;
	}

	/**
	 * Updates the flash screen to display the area around the snake's current position. Called on each iteration.
	 * The current method is to flip each tile movieclip to the right frame. We further save some CPU by flipping only
	 * the tiles that change.
	 */
	public function update_full():Void {
		//Get snake's position. Compute a top_left_corner from that. We divide width by 2 because we want the
		//snake to be centered on the screen.
		var snake_pos:Object = Main.app.snake.getFocalPosition();
	  var top_left_x:Number = snake_pos.x - width/2/*- (snake_pos.x % Math.floor(3*width/4)) - Math.floor(width/8)*/;
		var top_left_y:Number = snake_pos.y - height/2/*- (snake_pos.y % Math.floor(3*height/4)) - Math.floor(height/8)*/;

		if (top_left_x < 0) {
			top_left_x = 0;
		} else if (top_left_x + width > Main.app.map.get_width()) {
			top_left_x = Main.app.map.get_width() - width;
		}
		if (top_left_y < 0) {
			top_left_y = 0;
		} else if (top_left_y + height > Main.app.map.get_height()) {
			top_left_y = Main.app.map.get_height() - height;
		}

		//Go through the map array from the top left position and draw it on the screen
		// PERFORMANCE BOTTLENECK
		for (var y:Number = 0; y < height; y++) {
			var topleft_with_offset:Number = y + top_left_y;

			for (var x:Number = 0; x < width; x++) {
				var tile_type:Number = Main.app.map.get_map_element(top_left_x + x, topleft_with_offset);

				if (tile_type == undefined) {
					tile_type = 0;
				}

				//Compare with old screen and possibly change.
				if (this.last_screen[y][x] != tile_type) {
					this.map_rows[y][x].gotoAndStop(tile_type+1);
					this.last_screen[y][x] = tile_type;
				}
			}
		}

		//Should we show direction tip? Fixes #90.
		if (this.show_direction_tip) {
			Main.app.preset_screens.show_direction_tip();
			this.show_direction_tip = false;
		}
	}

	public function show():Void {
		this.map_over_rows(function(row) { row._visible = true; });
	}

	public function hide():Void {
		this.map_over_rows(function(row) { row._visible = false; });
	}

	public function destroy():Void {
		this.map_over_rows(function(row) { row.removeMovieClip(); });
	}

	public function set_map_alpha(a:Number) {
		this.map_over_rows(function(row) { row._alpha = a; })
	}

	public function map_over_rows(f:Function) {
		for (var i:Number = 0; i < this.map_rows.length; i++) {
			f(map_rows[i]);
		}
	}
}
