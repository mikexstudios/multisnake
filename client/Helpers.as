/**
 * Multiplayer Snake Game
 *
 * $Id:$
 */

/**
 * Contains helper functions.
 */
class Helpers {
	//From: http://www.twisty.com/bandwagon/archives/2007/07/23/162642
	// Note that this has been modified to not trim the space character (code_32). - Eugene
	public static function trim(str:String):String
	{
		var stripCharCodes : Object = {
			code_9  : true, // tab
			code_10 : true, // linefeed
			code_13 : true // return
		};
		while (stripCharCodes["code_" + str.charCodeAt(0)]) {
			str = str.substring(1, str.length);
		}
		while (stripCharCodes["code_" + str.charCodeAt(str.length - 1)]) {
			str = str.substring(0, str.length - 1);
		}
		return str;
	}

	public static function mod(a:Number, b:Number) : Number {
		var t:Number = a % b;
		if (t < 0) {
			t += b;
		}
		return t;
	}

	/**
	 * Puts borders around a MovieClip by drawing lines
	 * around it.
	 *
	 * @param   in_MC
	 */
	public static function set_border(in_MC:MovieClip, border_color:Number, border_width:Number,
                                    fill_color:Number):Void {
		if (typeof(fill_color) == 'number') {
			in_MC.beginFill(fill_color, 100);
		}

		if (border_color == undefined) {
			in_MC.lineStyle(0, 0x000000, 100); //0 = hairline thickness, 100% opacity
		} else {
			in_MC.lineStyle(border_width, border_color, 100); //0 = hairline thickness, 100% opacity
		}

		in_MC.moveTo(0,0);
		in_MC.lineTo(in_MC.width,0);
		in_MC.lineTo(in_MC.width, in_MC.height);
		in_MC.lineTo(0, in_MC.height);
		in_MC.lineTo(0,0);
	}

	/**
	 * Takes query strings on the end of the swf like:
	 * snake.swf?somekey=somevalue
	 * and returns an array like: array['somekey'] = somevalue
	 *
	 * @return  Array
	 */
	public static function get_url_parameters():Array {
		var url:String = _root._url;
		//trace(url);
		//Extract everything after ?
		var qpos:Number = url.lastIndexOf('?');
		var all_params:String = url.substr(qpos+1, url.length - 1);
		//Split parameters by &
		var param_pairs:Array = all_params.split('&');
		var param:Array = []; //Holds key, value pairs.
		var param_hash:Array = [];

		for (var i:Number=0; i<param_pairs.length; i++) {
			//trace(param_pairs[i]);
			param = param_pairs[i].split('=');
			param_hash[param[0]] = param[1];
		}

		return param_hash;
	}
}
