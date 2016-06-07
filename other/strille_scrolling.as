//---- Scrolling Demo by Strille, 2004, www.strille.net ---\\
//----------------------------------------------------------\\

//--- Attach a welcome message ---\\
attachMovie("welcomeScreen", "welcomeScreen", 1);
trace("If you run this movie in Flash MX 2004 the bitmap image may be shifted 1 pixel.\nThis does not happen in a browser or in the Flash Player.\n");

//--- Function declarations ---\\

//- called when a component changes value
function changeHandler(instance) {
	switch (instance._name) {
		case "showAreasCheckBox":
			showAreas = instance.getValue();
			areaLineHMC._visible = areaLineVMC._visible = showAreas;
			break;
		case "showBoundingBoxCheckBox":
			showBoundingBoxes = instance.getValue();
			for (var n in scr) { //- Remove object MC:s so that they will be redrawn
				removeMovieClip(scr[n]);
			}
			nOfVisObj = 0; //- holds number of visible objects, just for DEMO/DEBUG purposes
			break;
	}
}


//- creates an object movie clip in the screen movie clip
function crObjMC(n) {
	var c = scr.createEmptyMovieClip(n, n); //- create empty object MC
	c._x = oX[n]; //- place object MC in x
	c._y = oY[n]; //- place object MC in y

	var t = oTiles[n]; //- use local variable for speed
	var i = t.length/3; //- calculate number of tiles in the object
	
	while(i--) { //- loop through and create all tiles from the end to the first tile
		tellTarget(c.attachMovie(t[i*3], i, i)) { //- use tellTarget for speed
			_x = t[i*3+1]; //- place tile within object MC
			_y = t[i*3+2]; //- place tile within object MC
		}
	}
	
	//- draw the bounding box - just for DEMO/DEBUG purposes
	if (showBoundingBoxes) {
		var borderMC = c.createEmptyMovieClip(99, 99);
		borderMC.lineStyle(0, boundingBoxColors[n % 6], 75); //- set line color to one of the 6 defined, depending on the object number
		borderMC.moveTo(0, 0);
		borderMC.lineTo(oW[n]-1, 0); borderMC.lineTo(oW[n]-1, oH[n]-1);
		borderMC.lineTo(0, oH[n]-1); borderMC.lineTo(0, 0);
		borderMC.endFill();
	}
	
	nOfVisObj++; //- holds number of visible objects, just for DEMO/DEBUG purposes
}


//- draw visible tiles and place the screen MC
function dr() {
	var sX = int(xPos); //- screen left edge x coord (use local variables for speed)
	var sY = int(yPos); //- screen top edge y coord (use local variables for speed)
	var ax = int(sX/sW); //- x coord of the area in which coord sX, sY is in
	var ay = int(sY/sH); //- y coord of the area in which coord sX, sY is in
	
	if (ax == oldax && ay == olday) { //- if no new area, we use the time to delete old clips no longer visible
		for (var n in scr) { //- Remove object MC:s not visible on screen
			if (!oV[n]) {
				removeMovieClip(scr[n]);
				nOfVisObj--; //- holds number of visible objects, just for DEMO/DEBUG purposes
				break; //- if we removed a mc, break the search. We only delete one old mc per frame
			}
		}
	} else { //- if we have entered a new area, calculate list of objects to check
		oldax = ax; //- set this new area to the current area
		olday = ay; //- set this new area to the current area

		//- create new list of objects which MAY be visible
		ar = []; //- clear the list 
		for (var n in a[ax][ay]) { //- the area in which top left corner is in
			ar[n] = true; //- set the object to visible if it was in the area
		}
		for (var n in a[ax+1][ay]) { //- the area in which top right corner is in
			ar[n] = true; //- set the object to visible if it was in the area
		}
		for (var n in a[ax][ay+1]) { //- the area in which bottom left corner is in
			ar[n] = true; //- set the object to visible if it was in the area
		}
		for (var n in a[ax+1][ay+1]) { //- the area in which bottom right corner is in
			ar[n] = true; //- set the object to visible if it was in the area
		}
	}
	
	scr._x = sOffx - sX; //- scroll screen
	scr._y = sOffy - sY; //- scroll screen

	//- place the lines representing the area divisions (just for DEMO/DEBUG purposes)
	if (showAreas) {
		areaLineVMC._x = sOffx - (sX % sW) + (sX > 0 ? sW : 0);
		areaLineHMC._y = sOffy - (sY % sH) + (sY > 0 ? sH : 0);
	}
	
	var x1 = oX; //- object left edge x coord (use local variables for speed)
	var y1 = oY; //- object top edge y coord (use local variables for speed)
	var x2 = oX_oW; //- object right edge x coord (use local variables for speed)
	var y2 = oY_oH; //- object bottom edge y coord (use local variables for speed)
	var sX_sW = sX + sW; //- screen right edge x coord
	var sY_sH = sY + sH; //- screen bottom edge y coord

	oV = []; //- Reset array which stores if an object is visible or not
	
	for (var n in ar) { //- for every object in the visible areas...
		if (x2[n] > sX) { //...check if it's on screen in the x-dimension...
			if (x1[n] < sX_sW) { // (faster to write each test as a separate if-statemen)
				if (y2[n] > sY) { //...and that it's on screen in the y-dimension
					if (y1[n] < sY_sH) {
						oV[n] = true; //- set current object as VISIBLE
						if (!scr[n]) { //- if the object movie clip doesn't exist...
							crObjMC(n); //- ...create and place it in the scr movie clip
						}
					}
				}
			}
		}
	}
}


//- called when the map data has been loaded into the map data arrays
function init() {
	noo = oX.length; //- number of objects (just the lenght of one of the arrays containing map data)
	
	oldax = null; //- old area x-coord
	olday = null; // old area y-coord
	xPos = -10; //- screen x-pos
	yPos = -10; //- screen y-pos
	xVel = 0; //- screen x-velocity
	yVel = 0; //- screen y-velocity
	ticks = 0; //- counter (used for fps calc.)
	
	oX_oW = []; //- x coord of the right edge of the object
	oY_oH = []; //- y coord of the bottom edge of the object
	
	nOfVisObj = 0; //- number of visible objects, just for DEMO/DEBUG purposes
	
	//- create variables holding the object position + object size
	for (var n=0;n<noo;n++) {
		oX_oW[n] = oX[n] + oW[n];
		oY_oH[n] = oY[n] + oH[n];
	}
	
	registerObjectsToAreas(); //- register all objects to the area(s) they occupy

	areaLineHMC._x = sOffx; //- place the lines representing area devisions (for DEMO/DEBUG purposes)
	areaLineHMC._width = sW;
	areaLineVMC._y = sOffy; //- place the lines representing area devisions (for DEMO/DEBUG purposes)
	areaLineVMC._height = sH;
	
	welcomeScreen.gotoAndStop(2); //- Show "Click to start mess"
}


//- loads the objects (in this case they are generated randomly)
function loadMap() {
	//- init map variables
	oX = []; //- x coord for the top left corner of the object
	oY = []; //- y coord for the top left corner of the object
	oTiles = []; //- array of the form [tileNum 1, x pos 1, y pos 1, tileNum 2 etc...)
	oW = []; //- the width of the object
	oH = []; //- the height of the object

	//- generate some objects to form a "border"
	for (var n=0;n<50;n++) {
		generate128x16Object(128*n, 0);
		generate128x16Object(128*n, 16+128*50);
	}
	for (var n=0;n<50;n++) {
		generate16x128Object(0, 16+128*n);
		generate16x128Object(128*50-16, 16+128*n);
	}

	//- generate some different objects inside the border
	for (var n=0;n<500;n++) {
		generate16x128Object(16+int(Math.random()*6200), 16+int(Math.random()*6200));
		generate48x32Object(16+int(Math.random()*6200), 16+int(Math.random()*6200));
		generate128x64Object(16+int(Math.random()*6200), 16+int(Math.random()*6200));
		generate128x16Object(16+int(Math.random()*6200), 16+int(Math.random()*6200));
	}
}


//- main loop
function main() {
	if (Key.isDown(kLe)) { //- left
		xVel = -scrollSpeed;
	} else if(Key.isDown(kRi)) { //- right
		xVel = scrollSpeed;
	} else {
		xVel *= 0.9; //- deaccelerate to a halt in x
	}
	
	if (Key.isDown(kUp)) { //- up
		yVel = -scrollSpeed;
	} else if(Key.isDown(kDn)) { //- down
		yVel = scrollSpeed;
	} else {
		yVel *= 0.9; //- deaccelerate to a halt in y
	}

	xPos += xVel; //- add x velocity to x pos
	yPos += yVel; //- add y velocity to y pos
	
	dr(); //- draw tiles
	
	ticks++; //- used to calculate fps
}


//- called every 1000th millisecond
function oncePerSecond() {
	fpsText = (ticks-oldticks) + "/120"; //- display fps
	oldticks = ticks;
}


//- iterates through all objects and calculates in which areas
//- they appear in. If for example object number 3 appears in 
//- area 1,0 (an area equals the screen dimension) then 
//- a[1][0][3] = true, otherwise a[1][0][3] = undefined
function registerObjectsToAreas() { 
	if (!(sW > 0 && sH > 0)) { //- check that screen dimensions are defined
		trace("registerObjectsToAreas() failed:");
		trace("Screen dimensions (sW, sH) not defined!");
		return;
	}

	a = []; //- holds all the lists of objects in all the area squares (where objects exist)
	
	var mMax = Math.max; //- use local variable for speed
	
	for (var n=0;n<noo;n++) {
		var xMin = mMax(int(oX[n]/sW), 0);
		var yMin = mMax(int(oY[n]/sH), 0);
		var xMax = mMax(int(oX_oW[n]/sW), 0);
		var yMax = mMax(int(oY_oH[n]/sH), 0);
	
		for (var x=xMin;x<=xMax;x++) {
			for (var y=yMin;y<=yMax;y++) {
				if (a[x] == undefined) {
					a[x] = [];
				}
				if (a[x][y] == undefined) {
					a[x][y] = [];
				}
				a[x][y][n] = true;
			}
		}
	}
}


function startEngine() {
	this.onEnterFrame = main; //- start main loop
	if (oncePerSecondInterval == undefined) { //- start set interval which calculates fps
		oncePerSecondInterval = setInterval(oncePerSecond, 1000);
	}
}


function generate16x128Object(x, y) {
	oX.push(x);
	oY.push(y);
	oTiles.push([4, 16, 64, 4, 16, 128]);
	oW.push(16);
	oH.push(128);
}


function generate48x32Object(x, y) {
	oX.push(x);
	oY.push(y);
	oTiles.push([1, 16, 16, 1, 32, 16, 1, 16, 32, 1, 32, 32, 1, 48, 16, 1, 48, 32]);
	oW.push(48);
	oH.push(32);
}


function generate128x64Object(x, y) {
	oX.push(x);
	oY.push(y);
	oTiles.push([5, 64, 64, 5, 128, 64]);
	oW.push(128);
	oH.push(64);
}


function generate128x16Object(x, y) {
	oX.push(x);
	oY.push(y);
	oTiles.push([3, 64, 16, 3, 128, 16]);
	oW.push(128);
	oH.push(16);
}
