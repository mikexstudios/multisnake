/**
 * Multisnake - Map
 * Stores data about the full game map.
 */

class Map {
	private var width:Number;
	private var height:Number;
	private var map:Array;

	public function set_size(in_x:Number, in_y:Number) {
		this.width = in_x;
		this.height = in_y;

		//Run through variable and set all to EMPTY.
		this.map = [];
		for (var x : Number = 0; x < this.width; x++) {
			this.map[x] = [];
			for (var y : Number = 0; y < this.height; y++) {
				this.map[x][y] = Tile.EMPTY.index(); //Empty tile
			}
		}
	}

	public function get_width() {
		return this.width;
	}

	public function get_height() {
		return this.height;
	}

	public function get_map_element(x:Number, y:Number) {
		return this.map[x][y];
	}

	/**
	 * This function returns wrapped around coordinates when the input coordinates are beyond the map size.
	 * Helpers.mod is a little CPU expensive.
	 *
	 * @param	 in_x
	 * @param	 in_y
	 * @return	 Object use .x or .y to access the wrapped coordinates.
	 */
	public function get_coordinates(in_x:Number, in_y:Number):Object {
		return {x:Helpers.mod(in_x, this.width), y:Helpers.mod(in_y, this.height)};
	}

	/**
	 * Takes absolute position and tile type. Adds the tile
	 * to the full map. Deletes any existing tiles in that position.
	 *
	 * @param	 in_abs_x
	 * @param	 in_abs_y
	 * @param	 in_tile_type
	 */
	public function add_tile(in_abs_x:Number, in_abs_y:Number, in_tile_type:Tile):Void {
		this.map[in_abs_x][in_abs_y] = in_tile_type.index();
	}

	/**
	 * Deletes tile at given absolute position.
	 *
	 * @param	 in_abs_x
	 * @param	 in_abs_y
	 */
	public function delete_tile(in_abs_x:Number, in_abs_y:Number, in_tile_type:Tile):Void {
		if (in_tile_type == undefined) {
			this.map[in_abs_x][in_abs_y] = Tile.EMPTY.index();
		} else {
			//Otherwise, we only delete the specified tile
			if (this.map[in_abs_x][in_abs_y] == in_tile_type.index()) {
				this.map[in_abs_x][in_abs_y] = Tile.EMPTY.index();
			}
		}
	}

	/**
	 * Given the top left coordinates of a block (size determined by
	 * the size of the client viewable screen), deletes all tiles in
	 * that block.
	 *
	 * NOTE: This function is similar to Map.get_block(...). Therefore,
	 * any changes to this function should also probably change that
	 * function too.
	 *
	 * @param		top_left_x
	 * @param		top_left_y
	 */
	public function delete_block(top_left_x:Number, top_left_y:Number, in_tile_type:Tile):Void {
		for (var x:Number = 0; x < Screen.width; x++) {
			var off_x:Number = top_left_x + x;
			for (var y:Number = 0; y < Screen.height; y++) {
				this.delete_tile(off_x, top_left_y + y, in_tile_type);
			}
		}
	}

	public function delete_all(tile_type:Tile):Void {
		for (var x:Number = 0; x < this.width; x++) {
			for (var y:Number = 0; y < this.height; y++) {
				this.delete_tile(x, y, tile_type);
			}
		}
	}
}
