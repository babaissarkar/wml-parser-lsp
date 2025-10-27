A Work-In-Progress JavaCC based WML Preprocessor/Parser and LSP Server.

# Building
Run `mvn package`. The final JAR file will be in `jar/wml.jar`.

# Command line options
```bash
java -jar jar/wml.jar -h
Usage: Preprocessor [-datadir|-userdatadir|-log|-log-t[oken]|-log-p[arse]|-warn-
p[arse]|-s[erver]|-i[nput] filename|-o[utput] filename|-include file|-h[elp]|-?]
Options:
        -server/-s             Start as WML LSP server
        -datadir [path]        Absolute Path to Wesnoth's data directory
        -userdatadir [path]        Absolute Path to Wesnoth's userdata directory
        -log                   Print all logs (parser and tokenizer)
        -log-parse/-log-p      Print all parser logs
        -log-token/-log-t      Print all tokenizer logs
        -warn-parse/-warn-p    Print parser warnings only
        -include [path]        Preprocess the given file/folder beforehand and c
ollect macro definitions from it.
                               Can be used multiple times to include multiple fi
les before the main input.
        -define/-d [macroname] [body]
                               Define this macro before parsing
        -input/-i [path]       Preprocess the main input file
        -output/-o [path]      Write output to the given file
        -help/-?/-h            Print this help
```
