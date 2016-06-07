/**
* Multiplayer Snake Game
* SnakeConnection - supports connection to snake server.
*
* $Id:$
*/

class NotificationManager {
	public function add_notification(msg:String):Void {
		var notif_mc:MovieClip = Main.top_mc.attachMovie(Notification.symbolName, "notif",
                                                     Main.top_mc.getNextHighestDepth(), {message: msg});
	}
}
