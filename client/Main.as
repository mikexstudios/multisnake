/**
* Multiplayer Snake Game
*
* $Id:$
*/

import flash.external.ExternalInterface;

class Main {
	public static var app:SnakeGame;
	public static var top_mc:MovieClip;
	public static var dev_mode:Boolean = false;
	public static var dev_server:String = 'localhost';

  public static function trace(msg:String):Void {
    ExternalInterface.call("console.log", msg);
  }

	public static function start_game():Void {
		app = new SnakeGame();
	}

	private static function main(mc : MovieClip) : Void {
		top_mc = mc;

		//Check if we are in development mode. If so skip ads.
		var url_params:Array = Helpers.get_url_parameters();
		if (url_params['dev'] != undefined) {
			dev_mode = true;
      if (url_params['dev_server'] != undefined) {
        dev_server = url_params['dev_server'];
      }
			trace('In development mode.');
			start_game();
		} else {
			__com_mochibot__("43b27830", mc, 10301, true);
            
            //New MochiAds code for their automatic ads system.
            var _mochiads_game_id:String = "2b13ab508dfddc31";
            
			start_game();
		}
	}

	 //  MochiBot.com -- Version 7
	 //  Tested with with Flash 5-8, ActionScript 1 and 2
	private static function __com_mochibot__(swfid:String, mc:MovieClip, lv:Number, trk:Boolean):Void {
		var x,g,s,fv,sb,u,res,mb,mbc; mb = '__mochibot__'; mbc = "mochibot.com"; g = _global ? _global : _level0._root;
		if (g[mb + swfid]) return g[mb + swfid]; s = System.security; x = mc._root['getSWFVersion'];
		fv = x ? mc.getSWFVersion() : (_global ? 6 : 5); if (!s) s = {}; sb = s['sandboxType'];
		if (sb == "localWithFile") return null; x = s['allowDomain']; if (x) s.allowDomain(mbc);
		x = s['allowInsecureDomain']; if (x) s.allowInsecureDomain(mbc); u = "http://" + mbc + "/my/core.swf?mv=7&fv="
		+ fv + "&v=" + escape(getVersion()) + "&swfid=" + escape(swfid) + "&l=" + lv + "&f=" + mc + (sb ? "&sb=" + sb : "")
		+ (trk ? "&t=1" : ""); lv = (fv > 6) ? mc.getNextHighestDepth() : g[mb + "level"] ? g[mb + "level"] + 1 : lv;
		g[mb + "level"] = lv; if (fv == 5) { res = "_level" + lv; /* if (!eval(res))  loadMovieNum(u, lv);*/ }
		else { res = mc.createEmptyMovieClip(mb + swfid, lv); res.loadMovie(u); } return res;
	}
}
