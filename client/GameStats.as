/**
 * Multiplayer Snake Game
 *
 * $Id:$
 */

class GameStats extends MovieClip {
	public static var symbolName:String = "__Packages.GameStats";
	public static var symbolOwner:Function = GameStats;
	public static var symbolLinked:Boolean = Object.registerClass(symbolName, symbolOwner);

	private var num_clients_tf:TextField;
	public var num_clients:Number;

	public function GameStats() {
		//Bottom left:
		this._x = 5;
		this._y = Screen.get_height() - 20;

		this.createTextField("num_clients_tf", this.getNextHighestDepth(), 0, 0, 15, 20);
		this.num_clients_tf.autoSize = true;
		this.num_clients_tf.html = true;
	}

	public function setNumClients(clients:Number) {
		//When we are using htmlText, we must set the txtfmt each time.
		var txtfmt:TextFormat = new TextFormat();
		txtfmt.align = 'center';
		txtfmt.font = 'Arial';
		txtfmt.size = 11;
		txtfmt.color = 0x000000;

		num_clients = clients;
		if (num_clients == 1) {
			this.num_clients_tf.htmlText = "<b>1</b> Snake";
		} else {
			this.num_clients_tf.htmlText = '<b>'+num_clients + "</b> snakes";
		}

		this.num_clients_tf.setTextFormat(txtfmt);
	}
}
