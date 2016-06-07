﻿/** Copied from http://www.flashdevelop.org/community/viewtopic.php?t=404 */

/**
* Delegate class
* - MTASC friendly
* - allows additional parameters when creating the Delegate
*/
class Delegate
{
	/**
	* Create a delegate function
	* @param target   Objet context
	* @param handler   Method to call
	*/
	public static function create(target:Object, handler:Function):Function
	{
		var func:Function = function() : Object
		{
			var context:Function = arguments.callee;
			var args:Array = arguments.concat(context.initArgs);
			return context.handler.apply(context.target, args);
		}

		// Don't use local references to avoid "Persistent activation object" bug
		// See: http://timotheegroleau.com/Flash/articles/scope_chain.htm
		func.target = target;
		func.handler = handler;
		func.initArgs = arguments.slice(2);
		return func;
	}
}
