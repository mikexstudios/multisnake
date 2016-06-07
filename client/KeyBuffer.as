/**
* Multiplayer Snake Game
*
* $Id:$
*/

class KeyBuffer {
	private static var DEFAULT_LEN:Number = 2;

	private var buf:Array;
	private var len:Number;

	public function KeyBuffer() {
		this.len = DEFAULT_LEN;
		this.buf = [];
	}

	public function push(c : Direction) : Void {
		if (this.buf.length + 1 > this.len) {
			return;
		}

		this.buf.push(c);
	}

	public function pop() : Object {
		return this.buf.pop();
	}
}
