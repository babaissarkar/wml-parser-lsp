package com.babai.wml;

import static com.babai.wml.cli.ANSIFormatter.colorify;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import com.babai.wml.cli.ANSIFormatter;
import com.babai.wml.cli.ArgParser;
import com.babai.wml.lsp.WMLLanguageServer;
import com.babai.wml.output.DataExtractor;
import com.babai.wml.parser.Parser;
import com.babai.wml.parser.PathContext;
import com.babai.wml.preprocessor.Definition;
import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.Colors;
import com.babai.wml.utils.LogUtils;
import com.babai.wml.utils.Table;

public class Main {
	private static Table defines;
	private static HashMap<String, String> fileExplanations;
	private static PathContext pathContext;

	public static void main(String[] args) {
		var argParser = new ArgParser();
		argParser.parseArgs(args);
		
		ANSIFormatter.setColorsEnabled(argParser.enableColors);
		
		if (argParser.dataPath == null) {
			LogUtils.errorPrint("Wesnoth Gamedata path not specified.");
		} else {
			LogUtils.infoPrint("Wesnoth Gamedata path: " + argParser.dataPath.toAbsolutePath());
		}
		
		if (argParser.userDataPath == null) {
			LogUtils.errorPrint("Wesnoth Userdata path not specified.");
		} else {
			LogUtils.infoPrint("Wesnoth Userdata path: " + argParser.userDataPath.toAbsolutePath());
		}
		
		pathContext = new PathContext(
			argParser.dataPath,
			argParser.userDataPath,
			new HashSet<Path>());
		
		argParser.predefines.addRow(0, "predefined", "MULTIPLAYER", new Definition("MULTIPLAYER", "true"));
		
		if (argParser.startLSPServer) {
			WMLLanguageServer.initServer(argParser.predefines, pathContext, argParser.includes);
		} else {
			try {
				initParse(argParser);
				if (argParser.generateMacroRef) {
					DataExtractor.generateMacroRef(
						argParser.macroRefPath, defines, fileExplanations, pathContext);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void initParse(ArgParser argParser) throws IOException {
		LogUtils.setLogLevel(argParser.logLevel);
		
		long start = System.nanoTime();
		
		var p = new Preprocessor(pathContext, argParser.predefines);
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
		
		long preprocEnd = System.nanoTime();
		LogUtils.infoPrint("Preprocessing finished in " + (preprocEnd - start) / 1_000_000 + " ms");
		
		HashSet<Path> binaryPaths = new HashSet<>();
		Parser parser = new Parser();
		parser.addQuery("binary_path/path", v -> binaryPaths.add(Path.of(v)));
		for (var q : argParser.queries) {
			parser.addQuery(q, v -> LogUtils.infoPrint("Query " + q + " result: " + v));
		}
		parser.parse(out);
		
		start = preprocEnd;
		long parseEnd = System.nanoTime();
		LogUtils.infoPrint("Parsing finished in " + (parseEnd - start) / 1_000_000 + " ms");

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
}
