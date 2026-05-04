A WML Preprocessor/Parser/LSP Server and multitool.

# Building
Requires Java 21. Run `mvn package`. The final JAR file will be in `jar/wml.jar`.

# Command line options
```bash
java -jar jar/wml.jar -h
Usage: wml [-hsv] [-log-p] [-warn-p] [-color=<'true'|'false'>] [-datadir=<dataPath>] [-eut=<outputPath>] [-gmr=<outputPath>]
           [-i=<inputPath>] [-log-level=<'severe'|'warn'|'info'|'debug'|'off'>] [-o=<outputPath>] [-userdatadir=<userDataPath>]
           [-include=<includes>]... [-q=<queries>]... [-d=NAME BODY]...
  -color, --color=<'true'|'false'>
                            Toggle colored log messages.
  -d, -define, --define=NAME BODY
                            Define macro: -define NAME BODY
      -datadir, --datadir=<dataPath>
                            Absolute path to Wesnoth's data directory. Can also be specified via environment variable WESNOTH_DATA.
      -eut, -extract-unit-type, --extract-unit-type=<outputPath>
                            Extract unit type data to CSV at given path
      -gmr, -generate-macro-ref, --generate-macro-ref=<outputPath>
                            Generate HTML macro reference file
  -h, -?, -help, --help     Print this help
  -i, -input, --input=<inputPath>
                            Preprocess the main input file (default: stdin)
      -include, --include=<includes>
                            Preprocess file/folder and collect macro definitions
      -log-level, --log-level=<'severe'|'warn'|'info'|'debug'|'off'>
                            Set log level to following values severe|warn|info|debug|off
      -log-p, -log-parse, --log-parse
                            Print all parser logs (= -log-level debug)
  -o, -output, --output=<outputPath>
                            Write output to given file (default: stdout)
  -q, -query, --query=<queries>
                            XPath-style WML query. Any tag/key matching this will be printed.
  -s, -server, --server     Run in LSP server mode.
      -userdatadir, --userdatadir=<userDataPath>
                            Absolute path to Wesnoth's userdata directory. Can also be specified via environment variable WESNOTH_USERDATA.
  -v, -version, --version   Print version information
      -warn-p, -warn-parse, --warn-parse
                            Print parser warnings only (= -log-level warn)
```

### Supported LSP features:
* Go To Definition for WML macro calls.
* Hover info for WML macro calls.
* Completion for macro directives and macro calls.
* Hover info for WML paths. Show image preview if path is image.
* Completion for tag names.
* Shows help page link for tag names on hover.
* Preliminary Wesnoth path autocomplete. (Triggered by '/')
* Wesnoth Unit Type ids autocomplete. (Triggered by '=')
* Hints for position macro call arguments.
* Symbol table (only Macro defs and calls ATM, WIP.)

### Supported features
* Detailed colored logging, including files preocessed/macros found and others in debug mode
* Macro reference generation
* Custom WML queries into WML codebases
* Unit Type data extraction (WIP)

### Usage
* **VSCode**: Use the extension from [here](https://github.com/babaissarkar/wml-extension).
* **Kate**: Download the server JAR, install Java runtime, then use this config in **Settings > Configure Kate > LSP Client > User Server settings**.
Adjust paths as needed. Append the `wml` section to your `servers` if you have other stuff there.
```json
{
    "servers": {
        "wml": {
            "command": ["/usr/bin/java","-jar","/path/to/wml.jar","-s","-datadir","/path/to/wesnoth/data","-userdatadir","/path/to/wesnoth/user/data","-include","/path/to/wesnoth/datadir/core/macros","-include","/path/to/wesnoth/datadir/core/units.cfg"],
            "useWorkspace": true,
            "rootIndicationFileNames": ["_main.cfg"],
            "highlightingModeRegex": "Wesnoth"
        }
   }
}
```

Note: this is still very much a prototype. Please be forgiving and report any errors you come across. A log is usually available in Output tab in VSCode under WML LSP Server category, or the Output tab in Kate.
