package com.babai.wml;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.*;

import com.babai.wml.lsp.WMLLanguageServer;
import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.ArgParser;
import com.babai.wml.utils.Colors;

import java.awt.Color;
import java.io.IOException;
import java.util.logging.*;
import java.nio.file.Path;
import static com.babai.wml.utils.ANSIFormatter.*;

import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

public class Main {

	public static void main(String[] args) {
		var argParse = new ArgParser();
		argParse.parseArgs(args);
		if (argParse.startLSPServer) {
			initServer(argParse);
		} else {
			setLoggingFormat();
			try {
				initParse(argParse);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void initParse(ArgParser argParse) throws IOException {
		var p = new Preprocessor(System.in);
		p.showParseLogs(argParse.showParseLogs);
		p.showWarnLogs(argParse.warnParseLogs);
		p.setOutput(argParse.out == null ? System.out : argParse.out);
		p.setDefinesMap(argParse.predefines);
		p.token_source.dataPath = argParse.dataPath;
		p.token_source.userDataPath = argParse.userDataPath;
		p.token_source.showLogs = argParse.showLogs;

		if (argParse.inputPath != null) {
			p.debugPrint("Parsing " + colorify(argParse.inputPath.toString(), Colors.filePathColor));
		}

		for (Path incpath : argParse.includes) {
			p.subparse(incpath);
		}

		if (argParse.inputPath != null) {
			p.subparse(argParse.inputPath);
		}

		p.debugPrint("Binary Paths: " + p.getBinaryPaths());
		p.debugPrint("Total " + p.getDefines().rowCount() + " macros defined.");
	}

	private static void setLoggingFormat() {
		for (var handler : Logger.getLogger("").getHandlers()) {
			handler.setFormatter(new java.util.logging.Formatter() {
				@Override
				public String format(LogRecord r) {
					// Customize Message for separators between Level and Message
					Level l = r.getLevel();
					String lvlStr = "[" + l + "]";
					if (l == Level.SEVERE) {
						lvlStr = colorify(lvlStr, Color.RED);
					} else if (l == Level.WARNING) {
						lvlStr = colorify(lvlStr, Color.ORANGE);
					} else if (l == Level.INFO) {
						lvlStr = colorify(lvlStr, Color.CYAN);
					}

					return lvlStr + " " + r.getMessage() + "\n";
				}
			});
		}
	}

	private static void initServer(ArgParser argParser) {
		var server = new WMLLanguageServer(argParser.inputPath, argParser.dataPath, argParser.userDataPath,
				argParser.includes);

		// Initialize a simple JSON-RPC connection over stdin/stdout
		Launcher<LanguageClient> launcher = createServerLauncher(server, System.in, System.out);
		LanguageClient client = launcher.getRemoteProxy();

		server.connect(client);
		launcher.startListening();
	}
}
