# Snake game Makefile

MTASC_FLAGS = -swf test.swf -main -infer #-strict
AS_FILES = Main.as Test.as Delegate.as
RESOURCE_FILES = resources/black_10px.png resources/red_10px.png resources/green_10px.png

SWF_CONFIG_FILE = swf_config.xml

test.swf: $(AS_FILES) $(RESOURCE_FILES) $(SWF_CONFIG_FILE) Makefile
	swfmill simple $(SWF_CONFIG_FILE) test.swf
	mtasc $(MTASC_FLAGS) $(AS_FILES)

clean:
	rm -rf test.swf *~
