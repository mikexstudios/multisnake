/**
* Multiplayer Snake Game
*
* $Id:$
*/

class TimerDisplay {
	public var time_remaining:Number;

	private static var UPDATE_TIME:Number = 1000; //1sec. Could result in more CPU usage...
	private var update_interval:Number;
	private var container_mc:MovieClip

	public function TimerDisplay() {
		this.time_remaining = 0; //Initialize

		this.container_mc = Main.top_mc.createEmptyMovieClip("timer_container_mc", Main.top_mc.getNextHighestDepth());
		//Bottom right:
		this.container_mc._x = Screen.get_width() - 65;
		this.container_mc._y = Screen.get_height() - 33;
		this.container_mc._visible = false;
		//this.container_mc.width = 100;
		//this.container_mc.height = 20;
		//Helpers.set_border(this.container_mc, 0x000000, 0, 0xFFFFFF);
		//this.container_mc._alpha = 50;

		this.initialize_textfields();
	}

	private function initialize_textfields():Void {
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'right';
		txtfmt.font = 'Arial';

		this.container_mc.createTextField('time_label_tf', this.container_mc.getNextHighestDepth(), 0, 0, 58, 15);
		var time_label_tf:TextField = this.container_mc.time_label_tf;
		time_label_tf._x = 0;
		time_label_tf._y = 0;
		//time_label_tf.border = true;
		//time_label_tf.width = 100;
		time_label_tf.text = 'Time Left:'; //Empty at first until we get some data
		txtfmt.color = 0x444444;
		txtfmt.size = 10;
		txtfmt.bold = false;
		time_label_tf.setTextFormat(txtfmt);

		this.container_mc.createTextField('time_tf', this.container_mc.getNextHighestDepth(), 0, 0, 58, 15);
		var time_tf:TextField = this.container_mc.time_tf;
		time_tf._x = 0;
		time_tf._y = 13; //Under the Time Left: text
		//time_tf.width = 100;
		//time_tf.border = true;
		//time_tf.height = 30;
		time_tf.text = ''; //Empty at first until we get some data
		txtfmt.color = 0x000000;
		txtfmt.size = 11;
		txtfmt.bold = true;
		time_tf.setNewTextFormat(txtfmt);
	}

	public function show():Void {
		this.container_mc._visible = true;
	}

	public function hide():Void {
		this.container_mc._visible = false;
	}

	public function start():Void {
		this.update_interval = setInterval(Delegate.create(this, update), UPDATE_TIME);
	}

	public function stop():Void {
		clearInterval(this.update_interval);
	}

	public function set_time(in_seconds:Number):Void {
		this.time_remaining = in_seconds;
	}

	public function update():Void {
		//TODO: If time is below a threshold, change color.

		//If timer is negative, do not show it
		if (this.time_remaining < 0) {
			this.container_mc.time_tf.text = ''; //This is faster than hide
		} else {
			//Convert seconds to minutes:seconds
			var minutes:Number = Math.floor(this.time_remaining / 60);
			var seconds:Number = this.time_remaining % 60;

			this.container_mc.time_tf.text = minutes+'m '+seconds+'s';

			//Decrement timer.
			this.time_remaining -= (1000/UPDATE_TIME);
		}
	}

	public function destroy():Void {
		this.stop();
		this.container_mc.removeMovieClip();
	}
}
