/**
 * Eugene Marinelli
 */

class Direction {
	public static var NORTH:Direction = new Direction();
	public static var EAST:Direction = new Direction();
	public static var SOUTH:Direction = new Direction();
	public static var WEST:Direction = new Direction();

	private static var DIRECTIONS:Array = [NORTH, EAST, SOUTH, WEST];

	private function Direction() {}

	public static function random() {
		return DIRECTIONS[Math.floor(Math.random() * DIRECTIONS.length)];
	}

	public function to_string() : String {
		switch (this) {
			case NORTH: return "n";
			case SOUTH: return "s";
			case EAST: return "e";
			case WEST: return "w";
			default: return "";
		}
	}

	public function opposite() {
		switch (this) {
			case NORTH: return SOUTH;
			case SOUTH: return NORTH;
			case EAST: return WEST;
			case WEST: return EAST;
		}
	}
}
