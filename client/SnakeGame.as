/**
 * Multisnake - SnakeGame
 * Container class for various game elements.
 */

import LoadVars;

/**
 * We want SnakeGame to be a Singleton which sews
 * together all of the other classes.
 */
class SnakeGame {
	public var map:Map;
	public var screen:Screen;
	public var snake:Snake;
	public var preset_screens:PresetScreens;
	public var intro_screen:IntroScreen;
	public var end_round_display:EndRoundDisplay;
	public var notifications:ScrollingNotifications;
	public var timer_display:TimerDisplay;
	public var game_stats:MovieClip;
	public var points_display:MovieClip;
	public var connection:SnakeConnection;

	private var is_gameover:Boolean;
	private var is_introscreen:Boolean;
	private var is_snake_fallback_mode:Boolean; //Determines if we are using the snake_servers (fallback)

	private var username:String = 'Guest';
	private var need_to_send_username:Boolean;

	private static var available_hosts:Array;
	private static var host_index:Number;

	private static var WEB_SERVERS:Array = ['www.multisnake.com']; // TODO configuration file for this stuff.
	private static var SNAKE_SERVERS:Array = ['noserver01.multisnake.com', 'noserver02.multisnake.com',
		'noserver03.multisnake.com', 'noserver04.multisnake.com', 'noserver05.multisnake.com'];
	private var web_servers:Array; // Temporary storage
	private var snake_servers:Array;

	public function SnakeGame() {
		this.web_servers = WEB_SERVERS;
		this.snake_servers = SNAKE_SERVERS;
		this.is_gameover = true;
		this.is_introscreen = true;
		this.is_snake_fallback_mode = false;

		//Have to initialize these here since the intro screen requires some of these.
		//We don't use the this.obj accessing scheme since these are all static objects (not tied to 'this').
		this.screen = new Screen();
		this.map = new Map();
		this.snake = new Snake();
		this.notifications = new ScrollingNotifications();
		this.timer_display = new TimerDisplay();
		this.end_round_display = new EndRoundDisplay(); //Could possibly be moved elsewhere.
		this.preset_screens = new PresetScreens();
		this.intro_screen = new IntroScreen();

		this.snake.set_tile(Tile.random());

		game_stats = Main.top_mc.attachMovie(GameStats.symbolName, "gamestats", Main.top_mc.getNextHighestDepth());
		game_stats._visible = false;

		points_display = Main.top_mc.attachMovie(PointsDisplay.symbolName, "points_display",
			Main.top_mc.getNextHighestDepth());

		// Retrieve optimal snake server hostname. If in development mode,
		// we connect to localhost.
		if (Main.dev_mode) {
			this.onHostsReturned(Main.dev_server);
		} else {
			this.load_server_manager(this.web_servers.shift()); //Take first web server from array.
		}

		intro_screen.show_intro();
	}

	public function start_introscreen() {
		is_introscreen = true;
	}

	public function leave_introscreen() {
		is_introscreen = false;
	}

	private function load_server_manager(in_server:Object):Void {
		if(typeof(in_server) != 'string') {
			trace('not a valid server');
		}
		trace('using server-manager: '+in_server);

		// Load policy file for the "server manager" server/
		// Although flash does this automatically, we assume nothing and force it to load the policy file.
		System.security.loadPolicyFile('http://'+in_server+'/crossdomain.xml');
		var req:LoadVars = new LoadVars();
		req.onData = Delegate.create(this, this.onHostsReturned);
		//req.onLoad = Delegate.create(this, this.did_load_work); //DOESN'T WORK. SO WE CHECK IN ONDATA.
		req.load('http://'+in_server+'/manager/getserver');
	}

	public function set_username(u : String) {
		this.username = u;
	}

	public function get_username() {
		return this.username;
	}

	private function onHostsReturned(hosts_str:String) {
		trace(hosts_str);

		//If connection to the server manager failed.
		if (hosts_str == undefined) {
			if (this.web_servers.length > 0) {
				//Now we try another server.
				this.load_server_manager(this.web_servers.shift());

				return; //Skip rest of function
			} else {
				//We can't connect to any server managers
				hosts_str = 'NULL'; //Use the fallback servers.
			}
		}

		if (hosts_str == 'NULL') {
			trace("No snake servers available");
			// TODO error message or try to reconnect.

			//avaliable_hosts = this.snake_servers;
		}

		host_index = 0;
		available_hosts = hosts_str.split(","); // Returned string should have format server1,server2,server3
		connectToNextHost();
	}

	public function connectToNextHost():Void {
		if (host_index >= available_hosts.length || available_hosts[0] == 'NULL') {
			//Now we try to connect to the fallback servers
			if (this.is_snake_fallback_mode == false) {
				this.is_snake_fallback_mode = true;
				available_hosts = this.snake_servers;
				host_index = 0;

				this.connectToNextHost();
			} else {
				//We've really failed to connect!
				Main.app.intro_screen.show_failed_connect_text();
			}
		} else {
			var next_host:String = available_hosts[host_index];
			trace('snake-server: '+next_host);
			host_index++;
			connection = new SnakeConnection(next_host);
			connection.connect();
		}
	}

	public function resetHostIndex():Void {
		host_index = 0;
	}

	public function initialize():Void {
		//Everything is initialized! Now we can start the game!
		intro_screen.destroy();

		points_display.clearPoints();

		//Create timer
		timer_display.show();
		timer_display.start();
		points_display.show();

		// Add listener for keyboard input.
		var keyListener:Object = {};
		keyListener.onKeyDown = Delegate.create(this, onKeyDown_check);
		Key.addListener(keyListener);

		//Start the game
		this.restart();
	}

	private function onKeyDown_check():Void {
		var key_code:Number = Key.getCode(); // get key code

		if (!is_introscreen) {
			if (is_gameover) {
				this.restart();
			} else {
				if (preset_screens.direction_tip_MC != undefined) {
					preset_screens.hide_direction_tip();
				}

				switch (key_code) {
				case Key.LEFT:
					snake.push_move(Direction.WEST);
					break;
				case Key.RIGHT:
					snake.push_move(Direction.EAST);
					break;
				case Key.UP:
					snake.push_move(Direction.NORTH);
					break;
				case Key.DOWN:
					snake.push_move(Direction.SOUTH);
					break;
				}
			}
		}
	}

	/* Gets executed on each game iteration. */
	public function game_iteration():Void {
		screen.update_full();
		//snake.move();
	}

	public function game_over():Void {
		this.is_gameover = true;

		//-----------------------------------
		//Clear things that make the game run
		//-----------------------------------
		//Stops CPU sucking setInterval. Note: Technically, clearing the
		//interval will stop the game_iteration loop. However, our socket
		//connection still exists so when the server sends the abs. position
		//of the snake on each iteration, the client still displays it.
		//
		//However, when the snake dies, the server will send d and then cease
		//to keep sending snake positions. So technicaly, clearInterval will
		//work. (Only in testing purposes when we forcefully end the game
		//before the server sends d will we see the snake still moving even when
		//game has ended problem.)
		//   clearInterval(this.setinterval_id);

		//Show Game Over screen
		preset_screens.show_game_over();
	}

	public function flag_send_username() {
		this.need_to_send_username = true;
	}

	public function restart():Void {
		if (this.need_to_send_username) {
			this.connection.send_userinfo(username, snake.get_tile());
			this.need_to_send_username = false;
		}

		this.connection.sendReset();

		preset_screens.hide_game_over();

		snake.reset();
		game_stats._visible = true;
		this.is_gameover = false;

		screen.set_show_direction_tip(true);
	}

	public function show():Void {
		Main.top_mc.white.removeMovieClip();
		points_display.show();
	}

	/* This is really a hackish method of clearing the screen. We just put up a white MC. */
	public function hide():Void {
		if (preset_screens.direction_tip_MC != undefined) {
			preset_screens.hide_direction_tip();
		}

		preset_screens.hide_game_over();

		points_display.hide();

		var white:MovieClip = Main.top_mc.createEmptyMovieClip('white', Main.top_mc.getNextHighestDepth());
		white.width = Screen.get_width();
		white.height = Screen.get_height();
		Helpers.set_border(white, 0xFFFFFF, 0, 0xFFFFFF);
	}

	/* Used when going back to the intro. We want to destroy everything here. */
	public function destroy():Void {
		this.show();
		points_display.removeMovieClip();
		screen.destroy();
		notifications.destroy();
		timer_display.destroy();
		preset_screens.hide_game_over();
		intro_screen.destroy();
		preset_screens.hide_direction_tip(true);
		end_round_display.destroy();
		game_stats.removeMovieClip();
		Main.top_mc.removeMovieClip();
	}
}
