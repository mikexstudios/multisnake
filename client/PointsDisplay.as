/**
 * Multiplayer Snake Game
 *
 * $Id:$
 */

class PointsDisplay extends MovieClip {
	public static var symbolName:String = "__Packages.PointsDisplay";
	public static var symbolOwner:Function = PointsDisplay;
	public static var symbolLinked:Boolean = Object.registerClass(symbolName, symbolOwner);

	private static var POINTS_PER_DEATH:Number = -5;
	private static var POINTS_PER_FOOD:Number = 1;
	private static var POINTS_PER_KILL:Number = 5;

	private var points_label:TextField;
	private var points_text:TextField;

	//Variables to keep track of what constitutes the points
	private var deaths:Number;
	private var kills:Number;
	private var food:Number;

	public function PointsDisplay() {
		//Top right
		this._x = Screen.get_width() - 65
		this._y = 5;

		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'left';
		txtfmt.font = 'Arial';
		txtfmt.size = 18;
		txtfmt.bold = true;
		txtfmt.color = 0x000000;

		this.createTextField('points_label', this.getNextHighestDepth(), 0, 0, 58, 15);
		this.points_label._x = 0;
		this.points_label._y = 0;
		//time_label_tf.border = true;
		//time_label_tf.width = 100;
		this.points_label.text = 'Score:';
		txtfmt.color = 0x444444;
		txtfmt.size = 10;
		txtfmt.bold = false;
		this.points_label.setTextFormat(txtfmt);

		//Handles up to 4 digits while being aligned with the points_label
		createTextField("points_text", getNextHighestDepth(), 0, 0, 0, 0);
		points_text._x = -5;
		points_text._y = 15;
		//points_text.border = true;
		//points_text.autoSize = true;
		points_text._width = 70;
		points_text._height = 18;
		txtfmt.align = 'center';
		txtfmt.color = 0x000000;
		txtfmt.size = 11;
		txtfmt.bold = true;
		points_text.setNewTextFormat(txtfmt);

		this.clearPoints();
		this.hide();
	}

	public function hide():Void {
		this._visible = false;
	}

	public function show():Void {
		this._visible = true;
	}

	public function getKills():Number {
		return this.kills;
	}

	public function getDeaths():Number {
		return this.deaths;
	}

	public function getFood():Number {
		return this.food;
	}

	public function clearPoints():Void {
		this.deaths = 0;
		this.kills = 0;
		this.food = 0;
		this.updateDisplay();
	}

	public function addDeath():Void {
		this.deaths += 1;
		this.updateDisplay();
	}

	public function addFood():Void {
		this.food++;
		this.updateDisplay();
	}

	public function addKill():Void {
		this.kills++;
		this.updateDisplay();
	}

	private function getPoints():Number {
		return this.kills * POINTS_PER_KILL + this.deaths * POINTS_PER_DEATH + this.food * POINTS_PER_FOOD;
	}

	private function updateDisplay():Void {
		var p:Number = this.getPoints();
		var word:String;
		if (p == 1) {
			word = "point";
		} else {
			word = "points";
		}
		this.points_text.text = p + " " + word;
	}
}
