all: wml/Preprocessor.jj
	cd wml && javacc Preprocessor.jj && (patch --forward -p0 TokenMgrError.java < better-error-msg.patch||true)
	javac -d out wml/*.java sanitizer/*.java
	jar cfe Preprocessor.jar wml.Preprocessor -C out .
debug: wml/Preprocessor.jj
	cd wml && javacc -debug_parser=true -debug_token_manager -debug_lookahead Preprocessor.jj && (patch --forward -p0 TokenMgrError.java < better-error-msg.patch||true)
	javac -d out wml/*.java sanitizer/*.java
	jar cfe Preprocessor.jar wml.Preprocessor -C out .
clean:
	rm -rf out
	rm -f Preprocessor.jar
