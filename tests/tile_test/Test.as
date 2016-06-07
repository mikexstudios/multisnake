/**
* Multiplayer Snake Game
* 
* $Id:$
*/

import Delegate;

class Test {
	public var map:Array = [ 
		 [3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3],
         [3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,0,3],
         [3,0,2,0,0,0,0,2,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,2,0,0,0,0,1],
         [3,0,0,0,3,3,3,3,3,3,0,0,0,0,3,3,3,3,0,0,2,0,0,0,0,0,3,3,3,2,0,0,0,0,3],
         [3,0,0,3,3,0,2,0,0,3,0,0,2,0,3,3,0,3,3,0,2,0,0,0,2,3,3,0,0,0,3,2,0,0,3],
         [3,0,2,3,0,0,0,0,0,3,0,0,0,0,0,3,0,0,3,0,0,0,0,0,0,3,0,0,0,0,3,3,0,0,3],
         [3,0,0,3,0,0,0,0,0,3,0,0,0,2,0,3,0,0,3,2,0,0,2,0,2,3,0,0,0,0,0,2,2,0,3],
         [3,0,0,3,3,0,0,0,0,2,2,0,0,0,0,3,0,0,3,0,0,0,0,0,0,3,0,0,0,0,3,3,0,0,3],
         [3,0,0,0,3,3,3,3,3,3,0,0,2,0,0,3,3,3,3,2,0,0,0,0,0,3,3,3,3,3,3,2,0,0,3],
         [3,0,0,0,0,2,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,2,2,0,0,0,0,0,3],
         [3,0,2,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,2,0,0,3],
         [3,0,0,0,2,2,3,3,3,2,0,0,0,0,0,0,3,3,3,2,0,0,0,0,0,0,0,3,3,3,3,0,0,0,3],
         [3,0,0,2,2,3,0,3,3,2,0,0,0,2,0,3,3,0,3,2,0,2,0,0,0,0,3,3,0,0,3,2,0,0,3],
         [3,2,0,0,3,0,0,0,3,3,2,0,0,0,0,3,0,0,0,3,3,0,0,0,0,2,3,0,0,0,3,0,0,0,3],
         [3,0,0,2,3,0,0,0,0,2,0,0,0,0,0,3,0,0,0,0,2,2,0,0,0,2,3,0,0,0,3,0,0,0,3],
         [3,0,0,0,3,3,0,0,0,3,2,0,0,0,0,3,0,0,0,0,3,0,0,0,0,0,3,3,3,3,3,2,0,0,3],
         [3,0,0,2,0,3,3,3,2,3,0,0,0,2,2,3,3,3,0,0,3,0,0,0,2,0,0,2,0,0,0,0,0,0,3],
         [3,0,2,0,0,2,0,0,0,0,0,0,0,0,0,0,2,3,3,3,2,0,0,0,2,0,0,0,0,0,0,0,0,0,3],
         [3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3],
         [3,3,3,3,1,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3] ];
	public var location:Object = {x:10, y:10};
	public var screen_MC:MovieClip;
	


	public function Test() {
		this.initialize();
	}
	
	public function initialize():Void {
		trace('here');
		// Add listener for keyboard input.
		var keyListener:Object = new Object();
		keyListener.onKeyDown = Delegate.create(this, onKeyDown_check);
		Key.addListener(keyListener);
		
		//Create screen MC
		this.screen_MC = Main.top_mc.createEmptyMovieClip('screen', Main.top_mc.getNextHighestDepth());
		for(var x:Number=0;x<10;x++)
		{
			for(var y:Number=0;y<10;y++)
			{
				var id:String = 't_'+x+'_'+y;
				this.screen_MC.attachMovie('tile', id, this.screen_MC.getNextHighestDepth());
				this.screen_MC[id]._x = x * 10;
				this.screen_MC[id]._y = y * 10;
				this.screen_MC[id].gotoAndStop(1); //empty
			}
		}
		//this.screen_MC['t_5_5'].gotoAndStop(4);
		//this.show_screen();
		Main.top_mc.onEnterFrame = Delegate.create(this, show_screen);
	}
	
	private function onKeyDown_check():Void {
		var key_code:Number = Key.getCode(); // get key code
		//trace(key_code);
		switch (key_code)
		{
			case Key.LEFT:
				this.location.x--;
				break;
			case Key.RIGHT:
				this.location.x++;
				break;
			case Key.UP:
				this.location.y--;
				break;
			case Key.DOWN:
				this.location.y++;
				break;
			default:
				//nothing
				trace('invalid');
		}
	}	
	
	public function show_screen():Void {
		//Calculate location of screen
		var top_left:Object = {x:0, y:0};
		top_left.x = this.location.x - (10/2); //floor this? no
		top_left.y = this.location.y - (10/2);
		//trace(top_left.x+'_'+top_left.y);
		//trace('show');
		for(var x:Number=0;x<10;x++)
		{
			for(var y:Number=0;y<10;y++)
			{
				var id:String = 't_'+x+'_'+y;
				var tile_type:Number = this.map[top_left.x+x][top_left.y+y];
				//var tile_type:Number = this.map[x][y];
				//trace(tile_type);
				if(tile_type == undefined)
				{
					tile_type = 0;
				}
				/*
				this.screen_MC[id].removeMovieClip();
				this.screen_MC.attachMovie('tile', id, this.screen_MC.getNextHighestDepth());
				this.screen_MC[id]._x = x * 10;
				this.screen_MC[id]._y = y * 10;
				//this.screen_MC[id].gotoAndStop(1); //empty
				*/
				this.screen_MC[id].gotoAndStop(tile_type+1); //empty
			}
		}
	}
	
	public function show_screen2():Void {
		trace('show2');
		for(var x:Number=0;x<10;x++)
		{
			for(var y:Number=0;y<10;y++)
			{
				var id:String = 't_'+x+'_'+y;
				this.screen_MC[id].gotoAndStop(3); //empty
			}
		}
	}
	
	

}
