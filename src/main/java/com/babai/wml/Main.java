package com.babai.wml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import com.babai.wml.experimental.LogUtils;
import com.babai.wml.experimental.PathContext;
import com.babai.wml.experimental.Preprocessor;
import com.babai.wml.lsp.WMLLanguageServer;
import com.babai.wml.utils.ArgParser;
import com.babai.wml.utils.Colors;

import static com.babai.wml.utils.ANSIFormatter.colorify;

public class Main {

	public static void main(String[] args) {
		var argParse = new ArgParser();
		argParse.parseArgs(args);
		if (argParse.startLSPServer) {
			initServer(argParse);
		} else {
			try {
				initParse(argParse);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void initParse(ArgParser argParse) throws IOException {
		LogUtils.showParseLogs(argParse.showParseLogs);
		LogUtils.showParseWarnings(argParse.warnParseLogs);

		PathContext context = new PathContext(
			argParse.dataPath,
			argParse.userDataPath,
			new HashSet<Path>());

		var p = new Preprocessor(context, argParse.predefines);
		if (argParse.outputPath != null) {
			p.setOutput(Files.newBufferedWriter(argParse.outputPath));
		} else {
			p.setOutput(new BufferedWriter(new OutputStreamWriter(System.out)));
		}
//		p.setExtractData(argParse.extractUnitTypeData);

		if (argParse.inputPath != null) {
			LogUtils.debugPrint("Parsing " + colorify(argParse.inputPath.toString(), Colors.filePathColor));
		}

		for (Path incpath : argParse.includes) {
			// FIXME recheck directory support
			p.preprocess(incpath);
		}

		if (argParse.inputPath != null) {
			p.preprocess(argParse.inputPath);
		} else {
			p.preprocess(new InputStreamReader(System.in));
		}

//		var unitTypes = p.getUnitTypes();
//		p.debugPrint("Binary Paths: " + p.getBinaryPaths());
//		if (argParse.extractUnitTypeData) {
//			HashSet<Config> unitTypeData = p.getUnitTypeData();
//			writeUnitTypeData(unitTypeData, argParse.unitTypeOutPath);
//			LogUtils.debugPrint("Total " + p.getDefines().rowCount() + " macros and " + unitTypeData.size() + " unit types defined.");
//		} else {
//			LogUtils.debugPrint("Unit Types: " + unitTypes);
//			LogUtils.debugPrint("Total " + p.getDefines().rowCount() + " macros and " + unitTypes.size() + " unit types defined.");
//		}

		LogUtils.debugPrint("Total " + p.getDefines().rowCount() + " macros defined.");
	}

	private static void initServer(ArgParser argParser) {
		LogUtils.showParseLogs(argParser.showParseLogs);
		LogUtils.showParseWarnings(argParser.warnParseLogs);
		
		var server = new WMLLanguageServer(
			argParser.predefines,
			argParser.inputPath,
			argParser.dataPath,
			argParser.userDataPath,
			argParser.includes);

		try (var serverSocket = new ServerSocket(9007)) {
			var clientSocket = serverSocket.accept();
			var traceJsonStream = new PrintWriter(System.err, true);
			var launcherBuilder = new LSPLauncher.Builder<LanguageClient>()
					.setLocalService(server)
					.setRemoteInterface(LanguageClient.class)
					.setInput(clientSocket.getInputStream())
					.setOutput(clientSocket.getOutputStream());
			
			if (argParser.showJsonLogs) {
				launcherBuilder = launcherBuilder.traceMessages(traceJsonStream);  // dumps every JSON message
			}
			var launcher = launcherBuilder.create();
			server.connect(launcher.getRemoteProxy());
			launcher.startListening().get(); // blocks; JVM stays alive, exits when client disconnects
		} catch(IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
