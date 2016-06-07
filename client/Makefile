# Snake game Makefile

MTASC_FLAGS = -cp ./mx -swf snake.swf -main -infer -version 8 -trace Main.trace #-strict
AS_FILES = Main.as SnakeGame.as SnakeConnection.as Delegate.as Tile.as Map.as Screen.as Snake.as Helpers.as KeyBuffer.as MochiAd.as PresetScreens.as IntroScreen.as NotificationManager.as Notification.as GameStats.as ScrollingNotifications.as TimerDisplay.as EndRoundDisplay.as PointsDisplay.as
RESOURCE_FILES = resources/black_10px.png resources/red_10px.png resources/green_10px.png resources/msnake_logo3_w373.png resources/product_supplelabs.png

SWF_CONFIG_FILE = swf_config.xml

snake.swf: $(AS_FILES) $(RESOURCE_FILES) $(SWF_CONFIG_FILE) Makefile
	swfmill simple $(SWF_CONFIG_FILE) snake.swf
	mtasc $(MTASC_FLAGS) $(AS_FILES)

release:
	#swfmill simple $(SWF_CONFIG_FILE) snake.swf
	#mtasc $(MTASC_FLAGS) -trace no $(AS_FILES)
	cp snake.swf ../web/views/releases/

clean:
	rm -rf snake.swf *~
