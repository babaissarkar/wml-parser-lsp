package com.babai.wml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

import com.babai.wml.core.Definition;
import com.babai.wml.experimental.LogUtils;
import com.babai.wml.experimental.Parser;
import com.babai.wml.experimental.PathContext;
import com.babai.wml.experimental.Preprocessor;
import com.babai.wml.lsp.WMLLanguageServer;
import com.babai.wml.utils.ANSIFormatter;
import com.babai.wml.utils.ArgParser;
import com.babai.wml.utils.Colors;
import com.babai.wml.utils.Table;

import static com.babai.wml.utils.ANSIFormatter.colorify;
import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

public class Main {
	private static Table defines;
	private static HashMap<String, String> fileExplanations;
	private static PathContext context;

	public static void main(String[] args) {
		var argParse = new ArgParser();
		argParse.parseArgs(args);
		ANSIFormatter.setColorsEnabled(argParse.enableColors);
		if (argParse.startLSPServer) {
			initServer(argParse);
		} else {
			try {
				initParse(argParse);
				if (argParse.generateMacroRef) {
					DataExtractor.generateMacroRef(
						argParse.macroRefPath, defines, fileExplanations, context);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void initParse(ArgParser argParser) throws IOException {
		LogUtils.setLogLevel(argParser.logLevel);

		context = new PathContext(
			argParser.dataPath,
			argParser.userDataPath,
			new HashSet<Path>());

		argParser.predefines.addRow(0, "predefined", "MULTIPLAYER", new Definition("MULTIPLAYER", "true"));
		
		var p = new Preprocessor(context, argParser.predefines);
		BufferedWriter writer = null;
		if (argParser.outputPath != null) {
			writer = Files.newBufferedWriter(argParser.outputPath);
		} else {
			writer = new BufferedWriter(new OutputStreamWriter(System.out));
		}
//		p.setExtractData(argParse.extractUnitTypeData);

		if (argParser.inputPath != null) {
			LogUtils.debugPrint("Parsing " + colorify(argParser.inputPath.toString(), Colors.filePathColor));
		}

		for (Path incpath : argParser.includes) {
			// FIXME recheck directory support
			p.preprocess(incpath);
		}

		String out = "";
		if (argParser.inputPath != null) {
			out = p.preprocess(argParser.inputPath);
		} else {
			out = p.preprocessContent(new InputStreamReader(System.in));
		}
		writer.write(out);
		writer.flush();
		writer.close();
		
		HashSet<Path> binaryPaths = new HashSet<>();
		Parser parser = new Parser();
		parser.addQuery("binary_path/path", v -> binaryPaths.add(Path.of(v)));
		parser.parse(out);

//		var unitTypes = p.getUnitTypes();
//		if (argParse.extractUnitTypeData) {
//			HashSet<Config> unitTypeData = p.getUnitTypeData();
//			writeUnitTypeData(unitTypeData, argParse.unitTypeOutPath);
//			LogUtils.debugPrint("Total " + p.getDefines().rowCount() + " macros and " + unitTypeData.size() + " unit types defined.");
//		} else {
//			LogUtils.debugPrint("Unit Types: " + unitTypes);
//			LogUtils.debugPrint("Total " + p.getDefines().rowCount() + " macros and " + unitTypes.size() + " unit types defined.");
//		}
		
		LogUtils.infoPrint("Binary Paths: " + binaryPaths);
		LogUtils.infoPrint("Total " + p.getDefines().rowCount() + " macros defined.");
		defines = p.getDefines();
		fileExplanations = p.getFileExplanations();
	}

	private static void initServer(ArgParser argParser) {
		LogUtils.setLogLevel(Level.OFF);
		
		argParser.predefines.addRow(0, "predefined", "MULTIPLAYER", new Definition("MULTIPLAYER", "true"));

		var server = new WMLLanguageServer(
			argParser.predefines,
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
