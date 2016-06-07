/**
 * Multisnake - IntroScreen
 * Michael Huynh
 */

class IntroScreen {
	private var intro_MC:MovieClip;

	//For intro progress bar:
	private var pb_MC:MovieClip;
	private var pb_length:Number = 0;
	private var pb_length_max:Number = 30; //Specifies the maximum length of the PB
	private var pb_setinterval_id:Number;
	private var pb_max_connect_time:Number = 15000; //15 seconds
	private var pb_interval_time:Number = 300; //in millisecs. Over-written later.
	//End intro progress bar

	public function IntroScreen() {
		this.pb_interval_time = Math.floor(this.pb_max_connect_time / this.pb_length_max);
	}

	public function show_intro():Void {
		this.initialize();

		//Add logos
		this.show_intro_logos();
		this.show_instruction_link();

		//Connecting to...
		this.show_connecting_to();
	}

	public function show_reconnect():Void {
		this.initialize();
		this.show_intro_logos();

		//Hide progress bar stuff
		//this.show_connecting_to(); //Need to run this to create some objects
		this.hide_connecting_to();

		this.intro_set_h1_text('Disconnected from server (timeout).');

		var intro_button_action:Function = function():Void {
			//trace('r'+Main.app.username);
			//Keep username from being destroyed
			var username:String = Main.app.get_username();
			Main.app.destroy();
			Main.start_game();
			Main.app.set_username(username);
		}
		this.show_intro_button('Reconnect to server', Delegate.create(this, intro_button_action));
	}

	/**
	 * Creates the movieclip holding intro screen. Reason why this is a
	 * separate function is that the reconnect intro screen requires this
	 * too.
	 */
	private function initialize():Void {
		Main.app.start_introscreen(); //Used to keep from continually reconnecting.

		//Hide game screen as a precaution (if the server sends anything)
		Main.app.screen.hide();

		//Create MovieClips if they don't already exist
		if (typeof(Main.top_mc.intro) != 'movieclip') {
			Main.top_mc.createEmptyMovieClip('intro', Main.top_mc.getNextHighestDepth());

			this.intro_MC = Main.top_mc.intro;
			//Set MC properties (ie. borders)
			this.intro_MC.width = Screen.get_width();
			this.intro_MC.height = Screen.get_height();
			//Position the MC (top left corner).
			this.intro_MC._x = 0;
			this.intro_MC._y = 0;
		}
	}

	private function show_intro_logos():Void {
		//Check for existing movieclips. If exist, we don't need to recreate:
		if (typeof(this.intro_MC.intro_logo) != 'movieclip') {
			this.intro_MC.attachMovie('intro_logo', 'intro_logo', this.intro_MC.getNextHighestDepth());
		}

		if (typeof(this.intro_MC.product_of) != 'movieclip') {
			this.intro_MC.attachMovie('product_supplelabs', 'product_of', this.intro_MC.getNextHighestDepth());
		}

		//Add graphic to the MC
		var intro_logo:MovieClip = this.intro_MC.intro_logo;
		intro_logo.width = 373;
		intro_logo.height = 100;
		intro_logo._y = 40;
		//Position the center of the MC at the center.
		intro_logo._x = (Screen.get_width()/2) - (intro_logo.width / 2);
		intro_logo.onRelease = function() {
			getURL("http://www.multisnake.com");
		}

		//Bottom right corner of screen
		//TODO: Make this a clickable link that takes the user to supplelabs.com
		var product_of:MovieClip = this.intro_MC.product_of;
		product_of.width = 138;
		product_of.height = 16;
		product_of._x = Screen.get_width() - product_of.width - 10;
		product_of._y = Screen.get_height() - product_of.height - 2;
		product_of.onRelease = function() {
			getURL("http://www.supplelabs.com", "_blank");
		}
	}

	private function show_instruction_link():Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'left';
		txtfmt.font = 'Arial';

		this.intro_MC.createTextField('instruction_link', this.intro_MC.getNextHighestDepth(), 0, 0,
			Screen.get_width(), 22);
		var instruction_link:TextField = this.intro_MC.instruction_link;
		//instruction_link._x = 5;
		instruction_link._y = Screen.get_height() - 43;
		//instruction_link.autoSize = true;
		instruction_link.html = true;
		instruction_link.htmlText = 'Don\'t know how to play? <a href="http://www.multisnake.com/#instructions"><b>Click here for instructions!</b></a>';
		txtfmt.align = 'center';
		txtfmt.color = 0x444444;
		txtfmt.size = 14;
		txtfmt.bold = null;
		instruction_link.setTextFormat(txtfmt);
	}

	private function show_connecting_to():Void {
		//Connecting to server text
		this.intro_set_h1_text('Please wait. Connecting to server...');

		//this.pb_MC._y = 100;
		//this.pb_setinterval_id = setInterval(Delegate.create(this, increment_progress_bar), this.pb_interval_time);
		this.reset_progress_bar(); //Run the progress bar
	}

	private function hide_connecting_to():Void {
		this.intro_MC.intro_connecting_to.removeTextField();
		this.pb_MC.removeMovieClip();
	}

	/**
	 * Since we need to edit this h1 (header) text a lot, creating a function for it.
	 *
	 * @param		in_text
	 */
	private function intro_set_h1_text(in_text:String, in_color:Number):Void {
		//Check for existing textfield. If exist, we don't need to recreate:
		if (typeof(this.intro_MC.intro_connecting_to) != 'object') { //textfields output object
			this.intro_MC.createTextField('intro_connecting_to', this.intro_MC.getNextHighestDepth(), 0, 0,this.intro_MC.width, 30);
		}

		//Some of the below could be moved into the above if()

		var connecting_to:TextField = this.intro_MC.intro_connecting_to;
		connecting_to._x = 0;
		connecting_to._y = this.intro_MC.intro_logo._y + this.intro_MC.intro_logo.height + 25; //5px below the intro logo
		connecting_to.antiAliasType = 'advanced';
		connecting_to.text = in_text;
		//connecting_to.border = true;

		var connecting_to_format:TextFormat = new TextFormat();
		connecting_to_format.align = 'center';
		connecting_to_format.font = 'Arial';
		connecting_to_format.size = 20;
		connecting_to_format.bold = true;
		if (in_color == undefined) {
			connecting_to_format.color = 0x00000;
		} else {
			connecting_to_format.color = in_color;
		}
		connecting_to.setTextFormat(connecting_to_format);
	}

	public function hide_h1_text():Void {
		this.intro_MC.intro_connecting_to.removeTextField();
	}

	/**
	 * Sets the body text and formatting for the intro screen.
	 */
	private function show_intro_body_text(in_text:String, in_color:Number):Void {
		//Check for existing textfield. If exist, we don't need to recreate:
		if (typeof(this.intro_MC.intro_body) != 'object') { //textfields output object
			this.intro_MC.createEmptyMovieClip('intro_body', this.intro_MC.getNextHighestDepth());
		}

		//Create a body (text) box.
		var intro_body:MovieClip = this.intro_MC.intro_body;
		//Hide the progress bar. Show the body text.
		this.pb_MC._visible = false;
		//intro_body._visible = false; //Hide for now
		intro_body.width = 330;
		intro_body.height = 60;
		//Position the the MC at the center of the screen.
		intro_body._x = (Screen.get_width()/2) - (intro_body.width / 2);
		intro_body._y = this.intro_MC.intro_connecting_to._y + 35; //Should be same placement as the progress bar
		//Helpers.set_border(intro_body);

		//Create text box
		intro_body.createTextField('intro_body_tf', intro_body.getNextHighestDepth(), 0, 0, intro_body.width, 60);
		var intro_body_tf:TextField = this.intro_MC.intro_body.intro_body_tf;
		intro_body_tf._x = 0;
		intro_body_tf._y = 0;
		intro_body_tf.html = true;
		intro_body_tf.antiAliasType = 'advanced';
		intro_body_tf.multiline = true;
		intro_body_tf.wordWrap = true;
		intro_body_tf.htmlText = in_text;

		var intro_body_format:TextFormat = new TextFormat();
		intro_body_format.align = 'center';
		intro_body_format.font = 'Arial';
		intro_body_format.size = 15;
		if(in_color == undefined) {
			intro_body_format.color = 0x00000;
		} else {
			intro_body_format.color = in_color;
		}
		intro_body_tf.setTextFormat(intro_body_format);
	}

	private function show_intro_button(in_text:String, in_action:Function):Void {
		//Check for existing button. If exist, we don't need to recreate:
		if (typeof(this.intro_MC.intro_button) != 'movieclip') { //textfields output object
			this.intro_MC.createEmptyMovieClip('intro_button', this.intro_MC.getNextHighestDepth());
		}

		//Create a body (text) box.
		var intro_button:MovieClip = this.intro_MC.intro_button;
		//Hide the progress bar.
		this.pb_MC._visible = false;
		intro_button.width = 250;
		intro_button.height = 40;
		//Position the the MC at the center of the screen.
		intro_button._x = (Screen.get_width()/2) - (intro_button.width / 2);
		intro_button._y = this.intro_MC.intro_connecting_to._y + 60; //Should be same placement as the progress bar
		Helpers.set_border(intro_button, 0x990000, 4, 0xCC0000);

		//Create text box
		intro_button.createTextField('intro_button_tf', intro_button.getNextHighestDepth(), 0, 0, intro_button.width,
			intro_button.height);
		var intro_button_tf:TextField = this.intro_MC.intro_button.intro_button_tf;
		intro_button_tf._x = 0;
		intro_button_tf._y = 6;
		intro_button_tf.antiAliasType = 'advanced';
		intro_button_tf.text = in_text;

		var intro_button_format:TextFormat = new TextFormat();
		intro_button_format.align = 'center';
		intro_button_format.font = 'Arial';
		intro_button_format.size = 19;
		intro_button_format.bold = true;
		intro_button_format.color = 0xFFFFFF;
		intro_button_tf.setTextFormat(intro_button_format);

		//Mouse stuff
		//Make MovieClip clickable
		intro_button.onRelease = in_action;
	}

	private function show_username_text():Void {
		//Create text box
		this.intro_MC.createTextField('intro_username_tf', this.intro_MC.getNextHighestDepth(), 0, 0, 170, 60);
		var username_tf:TextField = this.intro_MC.intro_username_tf;
		username_tf._x = 40;
		username_tf._y = this.intro_MC.intro_logo._y + this.intro_MC.intro_logo.height + 16;
		username_tf.html = true;
		username_tf.antiAliasType = 'advanced';
		username_tf.selectable = false;
		//intro_body_tf.multiline = true;
		//intro_body_tf.wordWrap = true;
		username_tf.htmlText = '<b>Nickname:</b>';

		var username_format:TextFormat = new TextFormat();
		username_format.align = 'right';
		username_format.font = 'Arial';
		username_format.size = 18;
		username_format.color = 0x000000;
		username_tf.setTextFormat(username_format);

		this.intro_MC.createTextField('intro_username_input_tf', this.intro_MC.getNextHighestDepth(), 0, 0, 150, 10);
		var username_input_tf:TextField = this.intro_MC.intro_username_input_tf;
		username_input_tf._x = username_tf._x + 180;
		username_input_tf._y = username_tf._y + 1;
		username_input_tf.type = 'input';
		username_input_tf.border = true;
		username_input_tf._height = 23;
		username_input_tf.restrict = 'A-Za-z0-9_';
		username_input_tf.maxChars = 12;
		username_input_tf.tabEnabled = true;
		username_input_tf.antiAliasType = 'advanced';
		//Check if we already have a stored username
		username_input_tf.text = Main.app.get_username(); // Will be default at first

		var username_input_format:TextFormat = new TextFormat();
		//username_input_format.align = 'center';
		username_input_format.font = 'Arial';
		username_input_format.size = 18;
		username_input_format.color = 0x000000;
		username_input_tf.setTextFormat(username_input_format); //For existing text
		username_input_tf.setNewTextFormat(username_input_format); //For updated text

		this.draw_color_selection(username_format, username_tf._x, this.intro_MC.intro_username_tf._y + 30);

		//Have enter key trigger "click to play". Fixes #120.
		var keyListener:Object = {};
		keyListener.onKeyDown = function() {
			if(Key.isDown(Key.ENTER)) {
				Key.removeListener(keyListener);
				this.intro_click_to_play_action();
			}
		};
		keyListener.onKeyDown = Delegate.create(this, keyListener.onKeyDown);

		//Set the keylistener
		username_input_tf.onSetFocus = function() {
			//trace('added');
			Key.removeListener(keyListener); //if already exist
			Key.addListener(keyListener);
		}

		username_input_tf.onKillFocus = function() {
			//trace('removed');
			Key.removeListener(keyListener);
		}
	}

	private function draw_color_selection(format:TextFormat, x:Number, y:Number) {
		this.intro_MC.createTextField('intro_color_tf', this.intro_MC.getNextHighestDepth(), 0, 0, 170, 60);
		var color_tf:TextField = this.intro_MC.intro_color_tf;
		color_tf._x = x;
		color_tf._y = y;
		color_tf.html = true;
		color_tf.antiAliasType = 'advanced';
		color_tf.selectable = false;
		color_tf.htmlText = '<b>Color:</b>';
		color_tf.setTextFormat(format);

		var tile_y:Number = color_tf._y + Tile.size / 2 + 3;
		var base_tile_x:Number = color_tf._x + color_tf._width + 12;

		this.intro_MC.attachMovie('selection_background', 'selection', this.intro_MC.getNextHighestDepth());
		this.intro_MC['selection']._x = base_tile_x - 2 + 14*2;
		this.intro_MC['selection']._y = tile_y - 2;
		Main.app.snake.set_tile(Tile.COLORS[2]);

		for (var i:Number = 0; i < Tile.COLORS.length; i++) {
			var id:String = 'colorselection_' + Tile.COLORS[i].to_string();
			this.intro_MC.attachMovie(Tile.COLORS[i].library_symbol, id, this.intro_MC.getNextHighestDepth());
			this.intro_MC[id]._x = base_tile_x + i*14;
			this.intro_MC[id]._y = tile_y;
		}

		/* I hate actionscript -- need to figure how how this can be done in a loop. */
		this.intro_MC['colorselection_'+Tile.ORANGE.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.ORANGE); this.intro_MC['selection']._x = base_tile_x-2+14*0});
		this.intro_MC['colorselection_'+Tile.YELLOW.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.YELLOW); this.intro_MC['selection']._x = base_tile_x-2+14*1});
		this.intro_MC['colorselection_'+Tile.GREEN.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.GREEN); this.intro_MC['selection']._x = base_tile_x-2+14*2});
		this.intro_MC['colorselection_'+Tile.DARKGREEN.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.DARKGREEN); this.intro_MC['selection']._x = base_tile_x-2+14*3});
		this.intro_MC['colorselection_'+Tile.BLUE.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.BLUE); this.intro_MC['selection']._x = base_tile_x-2+14*4});
		this.intro_MC['colorselection_'+Tile.DARKBLUE.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.DARKBLUE); this.intro_MC['selection']._x = base_tile_x-2+14*5});
		this.intro_MC['colorselection_'+Tile.PURPLE.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.PURPLE); this.intro_MC['selection']._x = base_tile_x-2+14*6});
		this.intro_MC['colorselection_'+Tile.PINK.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.PINK); this.intro_MC['selection']._x = base_tile_x-2+14*7});
		this.intro_MC['colorselection_'+Tile.BROWN.to_string()].onRelease =
			Delegate.create(this, function() { Main.app.snake.set_tile(Tile.BROWN); this.intro_MC['selection']._x = base_tile_x-2+14*8});
	}

	private function increment_progress_bar():Void {
		var id:String = 'intro_pb_'+this.pb_length;
		this.pb_MC.attachMovie(Tile.GREEN.library_symbol, id, this.pb_MC.getNextHighestDepth());
		this.pb_MC[id]._x = Tile.size * this.pb_length;
		this.pb_MC[id]._y = 0;

		//Increment
		this.pb_length = this.pb_length + 1;

		//Check to see if we should end the progress bar
		if((Main.app.connection.is_connected()) || (this.pb_length > this.pb_length_max)) {
			clearInterval(this.pb_setinterval_id);

			//Now check if we have successfully connected or timed out:
			if (Main.app.connection.is_connected()) {
				//this.intro_set_h1_text('Successfully connected!');
				//this.intro_set_body_text('<b>Click here to begin!</b>', true);

				//Show username stuff
				this.intro_set_h1_text(''); //We technically need to call
				this.show_username_text();

				this.show_intro_button('Click here to play!', Delegate.create(this, this.intro_click_to_play_action));
			} else { //We have timed out:
				//Stop trying to reconnect. Doesn't work:
				//SnakeGame.connection.keep_connecting = false;

				//Reset the length and try again
				this.reset_progress_bar();
			}
		}
	}

	private function reset_progress_bar():Void {
		//If exists, delete
		if (typeof(this.intro_MC.intro_progress_bar) == 'movieclip') {
			this.intro_MC.intro_progress_bar.removeMovieClip();
		}

		//Progress bar. We'll be cheap and use snake tiles
		this.pb_MC = this.intro_MC.createEmptyMovieClip('intro_progress_bar', this.intro_MC.getNextHighestDepth());
		//Set a width of 10*tile_width
		this.pb_MC.width = Tile.size * 10;
		this.pb_MC.height = Tile.size;
		//Move to position
		this.pb_MC._x = 45;
		this.pb_MC._y = this.intro_MC.intro_connecting_to._y + 45; //10px below the connecting to text.

		this.pb_length = 0;
		this.pb_setinterval_id = setInterval(Delegate.create(this, increment_progress_bar), this.pb_interval_time);
	}

	/**
	 * Made this its own function since both increment_progress_bar()
	 * and show_username_text() uses this.
	 */
	private function intro_click_to_play_action():Void {
		//trace(this.intro_MC.intro_username_input_tf.text);
		Main.app.set_username(this.intro_MC.intro_username_input_tf.text);
		Main.app.initialize();
	}

	/**
	 * We make this have its own function so that onConnect = false can
	 * call this.
	 */
	public function show_failed_connect_text():Void {
		//Stop the progress bar
		clearInterval(this.pb_setinterval_id);

		//this.intro_MC.intro_connecting_to.color = 0xCC0000; //Red
		this.intro_set_h1_text('Failed to connect to server.', 0xCC0000);
		//TODO: If we have forums, then include a link there for help.
		//this.intro_set_body_text('There seems to be a problem either your computer or our servers. <b>Please try again later</b>.');
		this.show_intro_body_text('There seems to be a problem with either your computer or our servers. <b>Please refresh this page to try again</b>.');
	}

	public function destroy():Void {
		Main.top_mc['intro'].removeMovieClip();
		Main.app.screen.show();

		Main.app.leave_introscreen();
	}
}
