all: wml/Preprocessor.jj
	cd wml && javacc Preprocessor.jj
	javac -d out wml/*.java
	jar cfe Preprocessor.jar wml.Preprocessor -C out .
clean:
	rm -rf out
	rm -f Preprocessor.jar
