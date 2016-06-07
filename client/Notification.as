/**
* Multiplayer Snake Game
* SnakeConnection - supports connection to snake server.
*
* $Id:$
*/

class Notification extends MovieClip {
	public static var symbolName:String = "__Packages.Notification";
	public static var symbolOwner:Function = Notification;
	public static var symbolLinked:Boolean = Object.registerClass(symbolName, symbolOwner);

	private static var DISPLAY_TIME:Number = 4000; //ms
	private static var BOX_WIDTH:Number = 120;
	private static var BOX_HEIGHT:Number = 20;
	private static var BOX_XOFFSET:Number = 5;
	private static var BOX_YOFFSET:Number = 5;

	private var notif_mc:MovieClip
	private var clear_interval:Number;

	public function Notification() {
		notif_mc = createEmptyMovieClip("notif_mc", getNextHighestDepth());
		notif_mc.width = BOX_WIDTH;
		notif_mc.height = BOX_HEIGHT;
		notif_mc._x = BOX_XOFFSET;
		notif_mc._y = BOX_YOFFSET;

		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'left';
		txtfmt.font = 'Arial';
		txtfmt.size = 18;
		txtfmt.bold = true;
		txtfmt.color = 0x000000;

		notif_mc.createTextField("notif_text", notif_mc.getNextHighestDepth(), 0, 0, notif_mc.width, notif_mc.height);
		notif_mc.notif_text._x = 0;
		notif_mc.notif_text._y = 0;
		notif_mc.notif_text.antiAliasType = "advanced";
		notif_mc.notif_text.setTextFormat(txtfmt);

		// Draw a box around the text.
		notif_mc.beginFill(0xFFFFFF, 50);
		notif_mc.lineStyle(0, 0x000000, 100); //0 = hairline thickness, 100% opacity
		notif_mc.moveTo(0, 0);
		notif_mc.lineTo(notif_mc.notif_text.textWidth, 0);
		notif_mc.lineTo(notif_mc.notif_text.textWidth, notif_mc.notif_text.textHeight);
		notif_mc.lineTo(0, notif_mc.notif_text.textHeight);
		notif_mc.lineTo(0, 0);

		this.clear_interval = setInterval(Delegate.create(this, clearMC), DISPLAY_TIME);
	}

	private function clearMC():Void {
		clearInterval(this.clear_interval);
		notif_mc.removeMovieClip();
		removeMovieClip();
	}
}
