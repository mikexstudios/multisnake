/**
* Multiplayer Snake Game
*
* $Id:$
*/

class ScrollingNotifications {
	private static var XOFFSET:Number = 5;
	private static var YOFFSET:Number = 5;
	private static var MAX_LINES:Number = 5; //maximum lines to show for scrolling.
	private static var SCROLL_TIME:Number = 3000; //Time to wait before scrolling off old stuff.
	private static var TEXT_HEIGHT_PX:Number = 15; //Used to calculate the line height

	private var container_mc:MovieClip
	private var scroll_interval:Number;
	private var msg_ids:Array;
	private var msg_id_counter:Number;
	//private var intervals:Array;

	public function ScrollingNotifications() {
		this.container_mc = Main.top_mc.createEmptyMovieClip("container_mc", Main.top_mc.getNextHighestDepth());
		this.container_mc._x = XOFFSET;
		this.container_mc._y = YOFFSET;
		//this.container_mc.width = 200;
		//this.container_mc.height = 100;
		//Helpers.set_border(this.container_mc, 0x000000, 0, 0xFFFFFF);
		//this.container_mc._alpha = 50;

		msg_id_counter = 0;

		this.msg_ids = [];
		this.scroll_interval = setInterval(Delegate.create(this, shift_notification), SCROLL_TIME);
	}

	public function add_notification(in_text:String, in_color:Number):Void {
		var id:String = 'sn_'+msg_id_counter;

		//Set to an arbitrary width of 100px. We use text autosizing so the width doesn't really matter.
		this.container_mc.createTextField(id, this.container_mc.getNextHighestDepth(), 0, 0, 100, TEXT_HEIGHT_PX);
		var notify_tf:TextField = this.container_mc[id]; //NOTE: can't access by .id, must use [id]
		notify_tf._x = 0;
		notify_tf._y = this.msg_id_counter * TEXT_HEIGHT_PX;
		notify_tf.autoSize = true;
		notify_tf.html = true;
		notify_tf.htmlText = in_text;
		//DOESN'T WORK:------
		//notify_tf.background = true;
		//notify_tf.backgroundColor = 0xFFFFFF;
		//notify_tf._alpha = 10;
		//-------------------

		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'left';
		txtfmt.font = 'Arial';
		txtfmt.size = 11;
		if (in_color == undefined) {
			in_color = 0x000000;
		}
		txtfmt.color = in_color;
		notify_tf.setTextFormat(txtfmt);

		this.msg_ids.push(id); //Add new id to the bottom of stack
		this.msg_id_counter += 1;

		//Update our private tracking variables
		if (this.msg_ids.length >= MAX_LINES) {
			this.shift_notification();
		}
	}

	public function shift_notification():Void {
		if (this.msg_ids.length > 0) {
			var id:Object = this.msg_ids.shift(); //Remove top element
			this.container_mc[id].removeTextField();
			this.container_mc[id].removeMovieClip();

			// Now move entire movieclip up.
			this.container_mc._y -= TEXT_HEIGHT_PX;
		}
	}

	public function destroy():Void {
		clearInterval(this.scroll_interval);
		this.container_mc.removeMovieClip();
	}
}
