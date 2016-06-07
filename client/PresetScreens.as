/**
 * Multiplayer Snake Game
 *
 * $Id:$
 */

/**
 * Draws preset screens (like intro, game over)
 *
 * Idea: Perhaps use gotoAndStop() on movieclips?
 */
class PresetScreens {
	private var game_over_MC:MovieClip;
	public var direction_tip_MC:MovieClip; //Direction tip:

	public function PresetScreens() {}

	/**
	 * Displays the game over screen which shows user's statistics and then prompts to play another round
	 * by pressing the space key.
	 *
	 * (Stuff like this is probably easier to do with the Flash IDE. Oh well.)
	 */
	public function show_game_over():Void {
		hide_direction_tip();

		//Create MovieClips
		this.game_over_MC = Main.top_mc.createEmptyMovieClip('game_over', Main.top_mc.getNextHighestDepth());
		//this.game_over_MC._visible = false;

		//Decrease opacity of game screen
		Main.app.screen.set_map_alpha(75);

		//Set game over MC properties (ie. borders)
		//this.game_over_MC._visible = true;
		this.game_over_MC.width = 150;
		this.game_over_MC.height = 60;
		//Position the center of the MC at the center of the screen.
		this.game_over_MC._x = Screen.get_width()/2 - (this.game_over_MC.width / 2);
		this.game_over_MC._y = Screen.get_height()/2 - (this.game_over_MC.height / 2);

		//Fill the MC. Draw a box.
		this.game_over_MC.beginFill(0xFFFFFF, 75); //white, 100% opacity
		this.game_over_MC.lineStyle(0, 0x000000, 100); //0 = hairline thickness, 100% opacity
		this.game_over_MC.moveTo(0,0);
		this.game_over_MC.lineTo(this.game_over_MC.width,0);
		this.game_over_MC.lineTo(this.game_over_MC.width, this.game_over_MC.height);
		this.game_over_MC.lineTo(0, this.game_over_MC.height);
		this.game_over_MC.lineTo(0,0);

		//Create text
		var game_over_txtformat:TextFormat = new TextFormat();
		game_over_txtformat.align = 'center';
		game_over_txtformat.font = 'Arial';
		game_over_txtformat.size = 18;
		game_over_txtformat.bold = true;
		game_over_txtformat.color = 0x000000;

		this.game_over_MC.createTextField('game_over_header', this.game_over_MC.getNextHighestDepth(), 0, 0,
			this.game_over_MC.width, 50);
		//Position the TF at the center of the MC.
		//this.game_over_MC.game_over_header._x = (this.game_over_MC.width / 3);// - (this.game_over_MC.game_over_header.width / 2);
		//this.game_over_MC.game_over_header._y = 5;// - (this.game_over_MC.game_over_header.height / 2);
		this.game_over_MC.game_over_header._x = 0;
		this.game_over_MC.game_over_header._y = 0;
		//this.game_over_MC.game_over_header.autoSize = true; //Change width and height to fit contents.
		//this.game_over_MC.game_over_header.border = true;
		//this.game_over_MC.game_over_header.multiline = true;
		//this.game_over_MC.game_over_header.wordWrap = true;

		this.game_over_MC.game_over_header.text = 'Game Over';

		//this.game_over_MC.game_over_header.embedFonts = true;
		//this.game_over_MC.game_over_header.selectable = false;
		this.game_over_MC.game_over_header.antiAliasType = 'advanced';
		this.game_over_MC.game_over_header.setTextFormat(game_over_txtformat);

		//Now create (press/click to continue)
		this.game_over_MC.createTextField('game_over_body', this.game_over_MC.getNextHighestDepth(), 0, 0,
			this.game_over_MC.width, 60);
		this.game_over_MC.game_over_body._x = 0;
		this.game_over_MC.game_over_body._y = (this.game_over_MC.height / 3) + 5;
		this.game_over_MC.game_over_body.html = true;
		this.game_over_MC.game_over_body.htmlText = "<b>Click here</b> or <b>press any key</b> to play again.";
		this.game_over_MC.game_over_body.antiAliasType = 'advanced';
		this.game_over_MC.game_over_body.multiline = true;
		this.game_over_MC.game_over_body.wordWrap = true;

		var game_over_body_format:TextFormat = new TextFormat();
		game_over_body_format.align = 'center';
		game_over_body_format.font = 'Arial';
		game_over_body_format.size = 12;
		game_over_body_format.color = 0x00000;
		this.game_over_MC.game_over_body.setTextFormat(game_over_body_format);

		//Make MovieClip clickable
		this.game_over_MC.onRelease = function() {
			if (Main.app.connection.is_connected()) {
				Main.app.restart();
			} else {
				//We restart from the beginning. Destroy stuff:
				Main.app.destroy();
				Main.start_game();
			}
		}
	}

	/**
	 * Hides the Game Over MovieClip screen by removing it.
	 * We want this to be its own function since SnakeGame.restart()
	 * calls this.
	 */
	public function hide_game_over():Void {
		//Remove Game Over screen
		//NOTE: For some reason, the visible thing doesn't work. So we'll just
		//      create and delete MovieClip all the time.
		//Main.top_mc['game_over']._visible = false;
		Main.top_mc['game_over'].removeMovieClip();
		Main.app.screen.set_map_alpha(100);
	}

	//----------------

	public function show_direction_tip():Void {
		//Create MovieClips
		this.direction_tip_MC = Main.top_mc.createEmptyMovieClip('direction_tip', Main.top_mc.getNextHighestDepth());

		//Set game over MC properties (ie. borders)
		//this.game_over_MC._visible = true;
		this.direction_tip_MC.width = 170;
		this.direction_tip_MC.height = 20;
		//Position the MC
		this.direction_tip_MC._x = (Screen.get_width()/2) - (this.direction_tip_MC.width / 2);
		this.direction_tip_MC._y = (Screen.get_height()/2) - (this.direction_tip_MC.height / 2) - 40;

		//Fill the MC. Draw a box.
		Helpers.set_border(this.direction_tip_MC, 0x000000, 0, 0xFFFFFF); //1px black border, white fill.

		//Create text
		var direction_tip_txtformat:TextFormat = new TextFormat();
		direction_tip_txtformat.align = 'center';
		direction_tip_txtformat.font = 'Arial';
		direction_tip_txtformat.size = 12;
		direction_tip_txtformat.color = 0x000000;

		this.direction_tip_MC.createTextField('direction_tip_body', this.direction_tip_MC.getNextHighestDepth(), 0, 0,
			this.direction_tip_MC.width, 50);
		var direction_tip_tf:TextField = this.direction_tip_MC.direction_tip_body;
		direction_tip_tf._x = 0;
		direction_tip_tf._y = 0;
		direction_tip_tf.antiAliasType = 'advanced';
		direction_tip_tf.html = true;
		//direction_tip_tf.multiline = true;
		//direction_tip_tf.wordWrap = true;
		direction_tip_tf.htmlText = 'Press an <b>arrow key</b> to begin.';
		direction_tip_tf.setTextFormat(direction_tip_txtformat);
	}

	public function hide_direction_tip():Void {
		if (this.direction_tip_MC != undefined) {
			this.direction_tip_MC.removeMovieClip();
		}
	}
}
