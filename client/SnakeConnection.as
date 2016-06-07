/**
* Multiplayer Snake Game
* SnakeConnection - supports connection to snake server.
*
* $Id:$
*/

import XMLSocket;

class SnakeConnection {
	private static var PORT:Number = 10123;
	private static var PACKET_VALUE_OFFSET:Number = 14; // Don't want any byte to appear to be a newline or EOF.
	private static var ASCII_RADIX:Number = 36;

	private var socket:XMLSocket;
	private var gamehost:String;
	private var connected:Boolean;

	//Doesn't work:
	//public var keep_connecting:Boolean = true; //Used to keep connecting to server until false.

	public function SnakeConnection(host:String) {
		//For older versions of flash (7,8,9 pre r115) that do not automatically
		//connect to the master policy server. Fixes #36:
		System.security.loadPolicyFile('xmlsocket://'+host+':843');

		socket = new XMLSocket();
		socket.onConnect = Delegate.create(this, onConnect);
		socket.onData = Delegate.create(this, onData);
		socket.onClose = Delegate.create(this, onClose);
		gamehost = host;
	}

	public function connect():Void {
		this.socket.connect(gamehost, PORT);
	}

	public function disconnect():Void {
		this.socket.close();
	}

	public function is_connected():Boolean {
		return this.connected;
	}

	/** Dir should be "n", "s", "e", or "w". */
	public function sendDirection(dir : Direction):Void {
	this.socket.send(dir.to_string() + "\n");
	}

	public function sendReset():Void {
		this.socket.send("r\n");
	}

	public function send_userinfo(in_name:String, color_tile:Tile):Void {
		this.socket.send('u' + in_name + ',' + color_tile.to_string());
	}

	/**
	* To speed things up, this function is a modification of parseKnownTypeWindowReport
	* that adds tiles as we parse the report. This saves another n^2 operation (of re-reading
	* the array and adding the files) and also some memory.
	*
	* @param	 top_left_x Number contains the x top left coord.
	* @param	 top_left_y Number contains the y top left coord.
	* @param	 msg Server sent string for food and obstacle positions. Relative positions.
	* @param	 tile_type Number of tile type to add.
	*/
	private function parse_and_add_report(topleft_x:Number, topleft_y:Number, msg:String, tile_type:Tile):Void {
		var len:Number = msg.length;

		for (var i:Number = 0; i < len; i += 2) {
			var x:Number = Helpers.mod(topleft_x + msg.charCodeAt(i) - PACKET_VALUE_OFFSET, Main.app.map.get_width());
			var y:Number = Helpers.mod(topleft_y + msg.charCodeAt(i+1) - PACKET_VALUE_OFFSET, Main.app.map.get_height());
			Main.app.map.add_tile(x, y, tile_type);
		}
	}

	private function parse_and_add_self_report(topleft_x:Number, topleft_y:Number, msg:String):Void {
		var len:Number = msg.length;

		for (var i:Number = 0; i < len; i += 2) {
			var x:Number = Helpers.mod(topleft_x + msg.charCodeAt(i) - PACKET_VALUE_OFFSET, Main.app.map.get_width());
			var y:Number = Helpers.mod(topleft_y + msg.charCodeAt(i+1) - PACKET_VALUE_OFFSET, Main.app.map.get_height());
			Main.app.map.add_tile(x, y, Main.app.snake.get_tile());
		}
	}

	private function parse_and_add_colored_report(topleft_x:Number, topleft_y:Number, msg:String):Void {
		var len:Number = msg.length;

		for (var i:Number = 0; i < len; i += 3) {
			var x:Number = Helpers.mod(topleft_x + msg.charCodeAt(i) - PACKET_VALUE_OFFSET, Main.app.map.get_width());
			var y:Number = Helpers.mod(topleft_y + msg.charCodeAt(i+1) - PACKET_VALUE_OFFSET, Main.app.map.get_height());
			var tileid:Number = msg.charCodeAt(i+2) - PACKET_VALUE_OFFSET;
			var tile:Tile = Tile.from_index(tileid);
			Main.app.map.add_tile(x, y, tile == undefined ? Tile.DARKBLUE : tile);
		}
	}

	private function parse_and_remove_absolute_report(msg:String):Void {
		var positions:Array = msg.split(";");
		for (var i:Number = 0; i < positions.length; i++) {
			var coords:Array = positions[i].split(",");
			var x:Number = parseInt(coords[0], ASCII_RADIX);
			var y:Number = parseInt(coords[1], ASCII_RADIX);
			Main.app.map.delete_tile(x, y, undefined);
		}
	}

	private function onData(msg:String) {
		// Remove whitespace (includes newlines)
		// We need to do this since the server sends a \n + command after the first time. Therefore, we need to
		// remove the \n so that charAt works.
		msg = Helpers.trim(msg);
		var msg_type:String = msg.charAt(0);

		switch (msg_type) {
		case "d":
			// Die.
			Main.app.game_over();
			//Main.app.notifications.add_notification('You committed suicide.', 0xCC0000);
			Main.app.points_display.addDeath();
			break;

		case "l": // You were killed by...
			//var killer:String = msg.substr(1);
			//Main.app.notifications.add_notification('You were killed by <b>' + killer + '</b>.', 0xCC0000);
			Main.app.game_over();
			Main.app.points_display.addDeath();
			break;

		case "g": // Ate a food.
			Main.app.snake.grow(1);
			Main.app.points_display.addFood();
			break;

		case "j": // Player joined.
			var id:String = msg.substr(1);
			Main.app.notifications.add_notification('<b>'+id+'</b> joined.', 0x444444);
			break;

		case "i": // Player disconnected.
			var id:String = msg.substr(1);
			Main.app.notifications.add_notification('<b>'+id+'</b> left.', 0x444444);
			break;

		case "a": // Player died.
			Main.app.notifications.add_notification('<b>'+msg.substr(1)+'</b> committed suicide.', 0xCC0000);
			break;

		case "c":
			var num_clients:Number = parseInt(msg.substr(1), ASCII_RADIX);
			Main.app.game_stats.setNumClients(num_clients);
			break;

		case "s": // Add self snake positions
		case "e":
			var semi_index:Number = msg.indexOf(";");
			var topleft:Array = msg.substr(1, semi_index).split(",");
			var topleft_x:Number = parseInt(topleft[0], ASCII_RADIX);
			var topleft_y:Number = parseInt(topleft[1], ASCII_RADIX);

			if (msg_type == "s") {
				this.parse_and_add_self_report(topleft_x, topleft_y, msg.substr(semi_index+1));
			} else {
				this.parse_and_add_colored_report(topleft_x, topleft_y, msg.substr(semi_index+1));
			}
			break;

		case "r": // Remove self snake positions
			this.parse_and_remove_absolute_report(msg.substr(1));
			break;

		case "m":
			this.parse_and_remove_absolute_report(msg.substr(1));
			break;

		case "o": // Obstacle report
		case "f": // Food report
			var tile_type:Tile;
			if (msg_type == "o") {
				tile_type = Tile.WALL;
			} else {
				tile_type = Tile.FOOD;
			}

			var semi_index:Number = msg.indexOf(";");
			var topleft:Array = msg.substr(1, semi_index).split(",");
			var topleft_x:Number = parseInt(topleft[0], ASCII_RADIX);
			var topleft_y:Number = parseInt(topleft[1], ASCII_RADIX);

			Main.app.map.delete_block(topleft_x, topleft_y, tile_type);
			this.parse_and_add_report(topleft_x, topleft_y, msg.substr(semi_index+1), tile_type);
			break;

		case "p":
			// Absolute position report.
			var pos:Array = msg.substr(1).split(",");
			Main.app.snake.setPosition(parseInt(pos[0], ASCII_RADIX), parseInt(pos[1], ASCII_RADIX));
			Main.app.game_iteration();
			break;

		case "t": // A raw tick -- sometimes sent instead of absolute position.
			Main.app.game_iteration();
			break;

		case "b":
			// Board size.
			var lens : Array = msg.substr(1).split(",");
			Main.app.map.set_size(parseInt(lens[0], ASCII_RADIX), parseInt(lens[1], ASCII_RADIX));
			break;

		case "h":
			// Round end. Format is [rank;intermission_duration(seconds);ranked_snake*], ranked_snake=[client_id,score]
			var msg_elements:Array = msg.substr(1).split(";");
			var myrank:Number = parseInt(msg_elements[0], ASCII_RADIX);
			var intermission_duration:Number = parseInt(msg_elements[1], ASCII_RADIX);

			var ranked_snakes:Array = [];
			for (var i:Number = 2; i < msg_elements.length; i++) {
				var pair = msg_elements[i].split(",");
				ranked_snakes.push({client_id : pair[0], score : parseInt(pair[1], ASCII_RADIX)});
			}

			for (var i:Number = 0; i < ranked_snakes.length; i++) {
				trace("client:" + ranked_snakes[i].client_id + " score:" + ranked_snakes[i].score);
			}

			// Set intermission timer.
			Main.app.end_round_display.set_winners(ranked_snakes);
			Main.app.end_round_display.set_rank(myrank);
			Main.app.end_round_display.set_wait_time(intermission_duration);
			//Main.app.destroy();
			Main.app.hide();
			Main.app.end_round_display.show();

			Main.app.map.delete_all(Tile.FOOD); // Clear food.
			Main.app.map.delete_all(Tile.WALL); // Clear walls.
			break;

		case "n": // Round begin.
			var round_duration:Number = parseInt(msg.substr(1), ASCII_RADIX);

			// TODO mikexstudios display "round begin message" and reset the time-remaining timer.	Clear all food.
			trace('Time remaining: '+round_duration);
			Main.app.timer_display.set_time(round_duration);

			//Begin another round. It might be better to completely destroy the
			//app and start again (while somehow retaining connection info), but this will do:
			Main.app.end_round_display.destroy();
			//SnakeGame.end_round_display = null;
			Main.app.restart();
			Main.app.show(); //Putting this after restart lets the tiles draw first.
			//SnakeGame.snake.reset();

			Main.app.points_display.clearPoints();
			break;

		case "u": // Round status.
			var mode:String = msg.charAt(1);
			var time_remaining:Number = parseInt(msg.substr(2), ASCII_RADIX);

			if (mode == "g") {
				// In game mode.
				Main.app.timer_display.set_time(time_remaining);
			} else if (mode == "i") {
				// In intermission mode.
				// TODO mikexstudios display "Waiting for round to begin..." with countdown from time_remaining.
				//SnakeGame.timer_display.set_time(time_remaining);
				Main.app.end_round_display.set_wait_time(time_remaining);
				Main.app.hide();
				Main.app.end_round_display.show(true); //true = show wait screen instead
			} else {
				trace("ERROR: Invalid game mode.");
			}
			break;

		case "k": // You killed ...
			//var victim:String = msg.substr(1);
			//Main.app.notifications.add_notification('You killed <b>' + victim + '</b>!', 0x0000FF);
			Main.app.points_display.addKill();
			break;

		case "q": // Kill notification
			var msg_elements:Array = msg.substr(1).split(";");
			var killer:String = msg_elements[0];
			var victim:String = msg_elements[1];
			Main.app.notifications.add_notification('<b>' + killer + '</b> killed <b>' + victim + '</b>.', 0x444444);
			break;
		}
	}

	private function onConnect(success : Boolean) {
		if (success) {
			trace('Connection established.');
			this.connected = true;
			Main.app.resetHostIndex();
			Main.app.flag_send_username();

			//Stop SnakeGame from connecting
			//clearInterval(Main.app.connect_setinterval_id);
		} else {
			trace('Connection to ' + gamehost + ' failed.');
			Main.app.connectToNextHost();

			//If we fail, try again until the connect progress bar finishes
			//Doesn't work:
			/*
			if(this.keep_connecting) {
				this.connect();
			}
			*/
		}
	}

	private function onClose() {
		trace('Connection closed.');
		this.connected = false;

		Main.app.hide();
		Main.app.destroy();
		Main.app.intro_screen.show_reconnect();

		/*
		if(Main.app.is_introscreen)
		{
			//Show retry intro screen.
			trace('show retry');
			SnakeGame.intro_screen.show_reconnect();
		}
		else
		{
			//Return to intro screen
			Main.app.destroy();
			Main.start_game();
		}
		*/
	}
}
