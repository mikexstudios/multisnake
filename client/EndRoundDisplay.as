/**
 * Multisnake - EndRoundDisplay
 */

class EndRoundDisplay {
	private var container_mc:MovieClip;

	private var winners_ranked:Array;
	private var my_rank:Number;
	private var wait_time:Number;

	private static var UPDATE_TIME:Number = 1000; //1sec. Could result in more CPU usage...
	private var update_interval:Number;

	public function EndRoundDisplay() {}

	public function set_winners(in_ranked:Array) {
		this.winners_ranked = in_ranked;
	}

	public function set_rank(in_rank:Number) {
		this.my_rank = in_rank;
	}

	public function set_wait_time(in_time:Number) {
		this.wait_time = in_time - 2; //Minus 2 since that seems to be the lag time in displaying the countdown time.
	}

	public function show(is_wait_screen:Boolean):Void {
		this.container_mc = Main.top_mc.createEmptyMovieClip("endround_container_mc", Main.top_mc.getNextHighestDepth());
		this.container_mc._x = 0;
		this.container_mc._y = 0;
		this.container_mc.width = Screen.get_width();
		this.container_mc.height = Screen.get_height();

		if (is_wait_screen) {
			//This is for when the user joins during intermission
			this.show_wait();
			this.show_wait_time(true);
		}	else {
			//Show the normal end round screen.
			this.show_winners();
			this.show_individual_stats();
			this.show_wait_time();
		}
	}

	private function show_wait():Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.font = 'Arial';

		this.container_mc.createTextField('wait_header_tf', this.container_mc.getNextHighestDepth(), 0, 0,
                                      this.container_mc.width, 100);
		var wait_header_tf:TextField = this.container_mc.wait_header_tf;
		//winners_label_tf._x = (this.container_mc.width / 2) - (58 / 2); //can't seem to access the textfield's width directly.
		wait_header_tf._x = 0; //Being centered
		wait_header_tf._y = Screen.get_height()/2 - 50;
		//wait_header_tf.border = true;
		wait_header_tf.antiAliasType = 'advanced';
		wait_header_tf.wordWrap = true;
		wait_header_tf.text = 'You joined the game during our intermission period.';
		txtfmt.align = 'center';
		txtfmt.color = 0x444444; //0x336600 = dark green
		txtfmt.size = 25;
		txtfmt.bold = false;
		wait_header_tf.setTextFormat(txtfmt);
	}

	private function show_winners():Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'left';
		txtfmt.font = 'Arial';

		this.container_mc.createTextField('winners_label_tf', this.container_mc.getNextHighestDepth(), 0, 0,
                                      this.container_mc.width, 40);
		var winners_label_tf:TextField = this.container_mc.winners_label_tf;
		//winners_label_tf._x = (this.container_mc.width / 2) - (58 / 2); //can't seem to access the textfield's width directly.
		winners_label_tf._x = 0; //Being centered
		winners_label_tf._y = 10;
		//winners_label_tf.border = true;
		winners_label_tf.antiAliasType = 'advanced';
		winners_label_tf.text = 'The Winners Are:'; //Empty at first until we get some data
		txtfmt.align = 'center';
		txtfmt.color = 0x000000; //0x336600 = dark green
		txtfmt.size = 35;
		txtfmt.bold = true;
		winners_label_tf.setTextFormat(txtfmt);

		//Create a container for the winners:
		var winners_table:MovieClip = this.container_mc.createEmptyMovieClip('winners_table',
                                                                         this.container_mc.getNextHighestDepth());
		winners_table.width = 350;
		winners_table.height = 130;
		//Position the the MC at the center of the screen.
		winners_table._x = (Screen.get_width()/2) - (winners_table.width / 2);
		winners_table._y = winners_label_tf._y + 50; //Should be same placement as the progress bar
		Helpers.set_border(winners_table, 0x990000, 4, 0xCC0000);

		//Create table headers.
		//NOTE: Technically, this should be attached to the winners_table MC instead of to the
		//      container_mc. However, I wrote this first, and it works, so I don't feel like re-adjusting
		//      all the numbers relative to the winners table again.
		this.container_mc.createTextField('winners_name', this.container_mc.getNextHighestDepth(), 0, 0, 100, 15); //doesn't matter since autosize
		var winner_name_tf:TextField = this.container_mc.winners_name;
		winner_name_tf._x = 60;
		winner_name_tf._y = winners_label_tf._y + 55;
		winner_name_tf.autoSize = true;
		//A really cheap way of aligning, but then again, AS is just all a bunch of cheap hacks.
		winner_name_tf.text = 'Rank      Name                          Score';
		txtfmt.align = 'left';
		//txtfmt.color = 0x000000;
		txtfmt.color = 0xFFFFFF;
		txtfmt.size = 19;
		txtfmt.bold = true;
		winner_name_tf.setTextFormat(txtfmt);

		//Draw line for table header
		winners_table.lineStyle(2, 0xFFFFFF, 100);
		winners_table.moveTo(7, 35); //Remember, the reference is the box.
		winners_table.lineTo(winners_table.width - 7, 35);

		//Create table of winners
		var winners_spacing:Number = 20;
		for (var i:Number=0; i < this.winners_ranked.length; i++) {
			var id_name:String='w_name_'+i; //Some unique id for the winner tf
			var id_score:String='w_score_'+i;

			this.container_mc.createTextField(id_name, this.container_mc.getNextHighestDepth(), 0, 0, 100, 15); //doesn't matter since autosize
			var name_tf:TextField = this.container_mc[id_name];
			name_tf._x = winner_name_tf._x + 20;
			name_tf._y = (i * winners_spacing) + winners_label_tf._y + 92;
			name_tf.autoSize = true;
			name_tf.html = true;
			//We can do this since we know what our rank will only be one digit
			name_tf.htmlText = '<b>'+(i+1)+'</b>          '+this.winners_ranked[i].client_id;
			txtfmt.align = 'left';
			//This changes the color depending on the rank of the player
			/*
			switch(i)
			{
				case 0:
					txtfmt.color = 0xCC0000;
					break;
				case 1:
					txtfmt.color = 0x336600;
					break;
				default: //Rank 3 and below.
					txtfmt.color = 0x444444;
			}
			*/
			txtfmt.color = 0xFFFFFF;

			txtfmt.size = 17;
			txtfmt.bold = null;
			name_tf.setTextFormat(txtfmt);

			this.container_mc.createTextField(id_score, this.container_mc.getNextHighestDepth(), 0, 0, 100, 15); //doesn't matter since autosize
			var score_tf:TextField = this.container_mc[id_score];
			score_tf._x = name_tf._x + 260; //Push to the right
			score_tf._y = name_tf._y;
			score_tf.autoSize = true;
			txtfmt.size = 16;
			score_tf.text = this.winners_ranked[i].score;
			score_tf.setTextFormat(txtfmt);
		}
	}

	private function show_individual_stats():Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.font = 'Arial';

		//Create individual stats label:
		this.container_mc.createTextField('individual_stats_header', this.container_mc.getNextHighestDepth(), 0, 0,
			this.container_mc.width, 25);
		var individual_stats_tf:TextField = this.container_mc.individual_stats_header;
		individual_stats_tf._x = 0; //Being centered
		individual_stats_tf._y = this.container_mc.winners_table._y + this.container_mc.winners_table.height + 5;
		individual_stats_tf.antiAliasType = 'advanced';
		individual_stats_tf.text = 'Your statistics:';
		txtfmt.align = 'center';
		txtfmt.color = 0x000000; //0x336600 = dark green
		txtfmt.size = 20;
		txtfmt.bold = true;
		individual_stats_tf.setTextFormat(txtfmt);

		//Create a container for the individual stats:
		var individual_table:MovieClip = this.container_mc.createEmptyMovieClip('individual_stats_table',
			this.container_mc.getNextHighestDepth());
		individual_table.width = 350;
		individual_table.height = 57;
		//Position the the MC at the center of the screen.
		individual_table._x = (Screen.get_width()/2) - (individual_table.width / 2);
		individual_table._y = individual_stats_tf._y + 30;
		Helpers.set_border(individual_table, 0x336600, 4, 0x339900);

		individual_table.createTextField('individual_header', individual_table.getNextHighestDepth(), 0, 0, 100, 15); //doesn't matter since autosize
		var individual_header_tf:TextField = individual_table.individual_header;
		individual_header_tf._x = 10;
		individual_header_tf._y = 3;
		individual_header_tf.autoSize = true;
		//A really cheap way of aligning, but then again, AS is just all a bunch of cheap hacks.
		individual_header_tf.text = 'Rank      Deaths      Kills       Food          Score';
		txtfmt.align = 'left';
		txtfmt.color = 0xFFFFFF;
		txtfmt.size = 16;
		txtfmt.bold = true;
		individual_header_tf.setTextFormat(txtfmt);

		//Draw line for table header
		individual_table.lineStyle(2, 0xFFFFFF, 100);
		individual_table.moveTo(7, 28); //Remember, the reference is the box.
		individual_table.lineTo(individual_table.width - 7, 28);

		//Create a textfield for each of the stats
		this.create_individual_stats_tf('rank', 20, '<b>'+this.my_rank+'</b>', 17);
		this.create_individual_stats_tf('deaths', 90, Main.app.points_display.getDeaths());
		this.create_individual_stats_tf('kills', 160, Main.app.points_display.getKills());
		this.create_individual_stats_tf('food', 220, Main.app.points_display.getFood());
		this.create_individual_stats_tf('score', 305, Main.app.points_display.getPoints());
	}

	private function create_individual_stats_tf(in_name, in_x, in_text, text_size):Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.font = 'Arial';

		this.container_mc.individual_stats_table.createTextField(in_name, this.container_mc.individual_stats_table.getNextHighestDepth(), 0, 0, 15, 25);
		var my_stats_tf:TextField = this.container_mc.individual_stats_table[in_name];
		my_stats_tf._x = in_x;
		my_stats_tf._y = this.container_mc.individual_stats_table.individual_header._y + 28;
		my_stats_tf.antiAliasType = 'advanced';
		my_stats_tf.autoSize = true;
		my_stats_tf.html = true;
		my_stats_tf.htmlText = in_text; //'<b>'+this.my_rank+'</b>                   '+SnakeGame.points_display.deaths+'                    '+SnakeGame.points_display.kills+'                   '+SnakeGame.points_display.food+'                        '+SnakeGame.points_display.points;
		txtfmt.align = 'left';
		txtfmt.color = 0xFFFFFF; //0x336600 = dark green
		if (typeof(text_size) == 'number') {
			txtfmt.size = text_size;
		} else {
			txtfmt.size = 16;
		}
		txtfmt.bold = null;
		my_stats_tf.setTextFormat(txtfmt);
	}

	private function show_wait_time(is_wait_screen:Boolean):Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.font = 'Arial';

		//Create individual stats label:
		this.container_mc.createTextField('wait_time', this.container_mc.getNextHighestDepth(), 0, 0,
			this.container_mc.width, 25);
		var wait_time_tf:TextField = this.container_mc.wait_time;
		wait_time_tf._x = 0; //Being centered
		wait_time_tf.antiAliasType = 'advanced';
		//wait_time_tf.html = true;
		wait_time_tf.text = ''; //Show nothing at first, update it later.
		txtfmt.align = 'center';
		if (is_wait_screen) {
			//Wait screen
			wait_time_tf._height = 40;
			wait_time_tf.wordWrap = true;
			wait_time_tf._y = Screen.get_height()/2+20;
			txtfmt.color = 0x990000;
			txtfmt.size = 29;
		} else {
			//End Round Screen
			wait_time_tf._y = Screen.get_height() - 30;
			txtfmt.color = 0x444444; //0x336600 = dark green
			txtfmt.size = 18;
		}

		//wait_time_tf.setTextFormat(txtfmt);
		wait_time_tf.setNewTextFormat(txtfmt);

		//IMPORTANT: It turns out that AS is really stupid. If you're using an htmlText type TF,
		//           setNewTextFormat doesn't work on it! Therefore, I'm just going with a regular
		//           TF without the bold formatting. It'll look a bit worse, but it'll be easier
		//           on the client CPU.

		//Set the countdown
		this.update_interval = setInterval(Delegate.create(this, update_wait_time), UPDATE_TIME);
	}

	private function update_wait_time():Void {
		if (this.wait_time < 0) {
			//this.container_mc.wait_time.htmlText = 'Get ready! The next round is <b>starting</b>!';
			this.container_mc.wait_time._height = 200;
			this.container_mc.wait_time.htmlText = 'Get ready! The next round is starting!';
		} else {
			//Convert seconds to minutes:seconds
			var minutes:Number = Math.floor(this.wait_time / 60);
			var seconds:Number = this.wait_time % 60;

			if (minutes > 0) {
				//this.container_mc.wait_time.htmlText = 'The next round starts in <b>'+minutes+'m '+seconds+'s</b>!';
				this.container_mc.wait_time.htmlText = 'The next round starts in '+minutes+'m '+seconds+'s!';
			} else {
				//Assume only seconds
				//this.container_mc.wait_time.htmlText = 'The next round starts in <b>'+seconds+'s</b>!';
				this.container_mc.wait_time.htmlText = 'The next round starts in '+seconds+'s!';
			}

			//Decrement timer.
			this.wait_time -= (1000/UPDATE_TIME);
		}
	}

	private function stop_wait_time():Void {
		clearInterval(this.update_interval);
	}

	public function hide():Void {
		this.container_mc._visible = false;
	}

	public function destroy():Void {
		this.stop_wait_time();
		this.container_mc.removeMovieClip();
	}
}
