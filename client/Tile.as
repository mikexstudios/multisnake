/**
 * Multisnake - Tile
 */

class Tile {
	public var is_walkable:Boolean = true; //If not walkable
	public static var size:Number = 10; // px
	public var library_symbol:String; //The graphic name that is defined in the library.
	private var idx;

	public static var EMPTY:Tile = new Tile('empty', true, 0);
	public static var SNAKE:Tile = new Tile('snake_tile', false, 1);
	public static var FOOD:Tile = new Tile('food_tile', true, 2);
	public static var WALL:Tile = new Tile('wall_tile', false, 3);
	public static var ENEMY:Tile = new Tile('enemy_tile', false, 4);

	public static var BLUE:Tile = new Tile('blue_tile', false, 5);
	public static var BROWN:Tile = new Tile('brown_tile', false, 6);
	public static var DARKBLUE:Tile = new Tile('darkblue_tile', false, 7);
	public static var DARKGRAY:Tile = new Tile('darkgray_tile', false, 8);
	public static var DARKGREEN:Tile = new Tile('darkgreen_tile', false, 9);
	public static var GREEN:Tile = new Tile('green_tile', false, 10);
	public static var ORANGE:Tile = new Tile('orange_tile', false, 11);
	public static var PINK:Tile = new Tile('pink_tile', false, 12);
	public static var PURPLE:Tile = new Tile('purple_tile', false, 13);
	public static var YELLOW:Tile = new Tile('yellow_tile', false, 14);

	public static var COLORS:Array = [ORANGE, YELLOW, GREEN, DARKGREEN, BLUE, DARKBLUE, PURPLE, PINK, BROWN];

	private static var idColorMapInited:Boolean = false;
	private static var idColorMap:Object = {};

	public function index():Number {
		return idx;
	}

	public function to_string():String {
		return idx.toString(16);
	}

	public static function from_index(idx:Number):Tile {
		if (!idColorMapInited) {
			for (var i:Number = 0; i < COLORS.length; i++) {
				idColorMap[COLORS[i].index()] = COLORS[i];
			}
			idColorMapInited = true;
		}

		return idColorMap[idx];
	}

	public static function random():Tile {
		return COLORS[Math.floor(Math.random() * COLORS.length)];
	}

	private function Tile(in_symbol:String, is_walkable:Boolean, idx:Number) {
		this.is_walkable = is_walkable;
		this.library_symbol = in_symbol;
		this.idx = idx;
	}
}
