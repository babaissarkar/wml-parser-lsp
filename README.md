A Work-In-Progress JavaCC based WML Preprocessor/Parser and LSP Server.

# Building
Run `mvn package`. The final JAR file will be in `jar/wml.jar`.

# Command line options
```bash
java -jar jar/wml.jar -h
Usage: Preprocessor [-datadir|-userdatadir|-log|-log-t[oken]|-log-p[arse]|-warn-p[arse]|-s[erver]|-i[nput] filename|-o[utput] filename|-include file|-h[elp]|-?]
Options:
	-server/-s             Start as WML LSP server
	-datadir               Absolute Path to Wesnoth's data directory
	-userdatadir           Absolute Path to Wesnoth's userdata directory
	-log                   Print all logs (parser and tokenizer)
	-log-parse/-log-p      Print all parser logs
	-log-token/-log-t      Print all tokenizer logs
	-warn-parse/-warn-p    Print parser warnings only
	-include file          Preprocess the given file beforehand and collect macro definitions from it.
	                       Can be used multiple times to include multiple files before the main input.
	-input/-i filename     Preprocess the main input file
	-output/-o filename    Write output to the given file
	-help/-?/-h            Print this help
```
