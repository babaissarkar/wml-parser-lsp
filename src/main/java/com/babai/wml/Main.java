package com.babai.wml;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.*;

import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.ArgParser;

import java.awt.Color;
import java.util.logging.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static com.babai.wml.utils.ANSIFormatter.*;

import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

public class Main {

	public static void main(String[] args) {
		var argParse = new ArgParser();
		argParse.parseArgs(args);

		setLoggingFormat();

		if (argParse.startLSPServer) {
			initServer();
		} else {
			try {
				var p = new Preprocessor(System.in);
				if (argParse.inputPath != null) {
					p = new Preprocessor(argParse.inputPath);
				}

				p.showParseLogs(argParse.showParseLogs);
				p.showWarnLogs(argParse.warnParseLogs);
				p.setOutput(argParse.out == null ? System.out : argParse.out);
				p.setDefinesMap(argParse.predefines);
				p.token_source.dataPath = argParse.dataPath;
				p.token_source.userDataPath = argParse.userDataPath;
				p.token_source.showLogs = argParse.showLogs;

				if (argParse.inputPath != null) {
					p.debugPrint("Parsing " + colorify(argParse.inputPath.toString(), p.filePathColor));
				}

				for (Path incpath : argParse.includes) {
					p.subparse(incpath);
				}
				p.subparse(argParse.inputPath);

				p.debugPrint("Total " + p.getDefines().size() + " macros defined.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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

	private static void initServer() {
		var server = new WMLLanguageServer();

		// Initialize a simple JSON-RPC connection over stdin/stdout
		Launcher<LanguageClient> launcher = createServerLauncher(server, System.in, System.out);
		LanguageClient client = launcher.getRemoteProxy();

		server.connect(client);
		launcher.startListening();
	}

	public static class WMLLanguageServer implements LanguageServer, LanguageClientAware {

		// private LanguageClient client;
		//
		public void connect(LanguageClient client) {
			// this.client = client;
		}

		@Override
		public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
			ServerCapabilities capabilities = new ServerCapabilities();
			capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

			InitializeResult result = new InitializeResult(capabilities);
			return CompletableFuture.completedFuture(result);
		}

		@Override
		public CompletableFuture<Object> shutdown() {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void exit() {
			System.exit(0);
		}

		@Override
		public TextDocumentService getTextDocumentService() {
			return new TextDocumentService() {
				@Override
				public void didOpen(DidOpenTextDocumentParams params) {
					System.out.println("File opened: " + params.getTextDocument().getUri());
				}

				@Override
				public void didChange(DidChangeTextDocumentParams params) {
				}

				@Override
				public void didClose(DidCloseTextDocumentParams params) {
				}

				@Override
				public void didSave(DidSaveTextDocumentParams params) {
				}
			};
		}

		@Override
		public WorkspaceService getWorkspaceService() {
			return new WorkspaceService() {
				@Override
				public void didChangeConfiguration(DidChangeConfigurationParams params) {
				}

				@Override
				public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
				}
			};
		}
	}
}
