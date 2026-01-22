package com.babai.wml;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.*;

import com.babai.wml.core.Config;
import com.babai.wml.core.ConfigAttributeBase;
import com.babai.wml.lsp.WMLLanguageServer;
import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.ArgParser;
import com.babai.wml.utils.Colors;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.logging.*;
import java.nio.file.Files;
import java.nio.file.Path;
import static com.babai.wml.utils.ANSIFormatter.*;

import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

public class Main {
	private static final String[] UNIT_TYPE_COLUMNS = {
			"id",
			"race",
			"gender",
			"hitpoints",
			"movement_type",
			"movement",
			"experience",
			"level",
			"alignment",
			"advances_to",
			"cost",
			"usage",
			"name",
			"image",
			"profile",
			"description"
	};

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
		p.setExtractData(argParse.extractUnitTypeData);
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

		var unitTypes = p.getUnitTypes();
		p.debugPrint("Binary Paths: " + p.getBinaryPaths());
		if (argParse.extractUnitTypeData) {
			HashSet<Config> unitTypeData = p.getUnitTypeData();
			writeUnitTypeData(unitTypeData, argParse.unitTypeOutPath);
			p.debugPrint("Total " + p.getDefines().rowCount() + " macros and " + unitTypeData.size() + " unit types defined.");
		} else {
			p.debugPrint("Unit Types: " + unitTypes);
			p.debugPrint("Total " + p.getDefines().rowCount() + " macros and " + unitTypes.size() + " unit types defined.");
		}
		
	}

	private static void writeUnitTypeData(HashSet<Config> unitTypeData, Path unitTypeOutPath) {
		try (BufferedWriter writer = Files.newBufferedWriter(unitTypeOutPath)) {

			// Header
			writer.write(String.join(",", UNIT_TYPE_COLUMNS));
			writer.newLine();

			for (Config cfg : unitTypeData) {
				StringBuilder row = new StringBuilder();

				for (int i = 0; i < UNIT_TYPE_COLUMNS.length; i++) {
					if (i > 0) row.append(',');

					String key = UNIT_TYPE_COLUMNS[i];
					ConfigAttributeBase attr = cfg.getAttr(key);

					String value = (attr == null) ? "" : attr.stringValue();
					row.append(csvEscape(value));
				}

				writer.write(row.toString());
				writer.newLine();
			}

		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write unit type CSV", e);
		}
	}
	
	private static String csvEscape(String s) {
		if (s == null) return "";

		boolean needsQuotes =
				s.contains(",") ||
				s.contains("\"") ||
				s.contains("\n") ||
				s.contains("\r");

		if (!needsQuotes) return s;

		return "\"" + s.replace("\"", "\"\"") + "\"";
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
		var server = new WMLLanguageServer(
			argParser.predefines,
			argParser.inputPath,
			argParser.dataPath,
			argParser.userDataPath,
			argParser.includes);

		// Initialize a simple JSON-RPC connection over stdin/stdout
		Launcher<LanguageClient> launcher = createServerLauncher(server, System.in, System.out);
		LanguageClient client = launcher.getRemoteProxy();

		server.connect(client);
		launcher.startListening();
	}
}
