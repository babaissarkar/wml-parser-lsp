package com.babai.wml;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;

import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.ArgParser;

import java.awt.Color;
import java.io.IOException;
import java.util.logging.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.babai.wml.utils.ANSIFormatter.*;

import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

public class Main {

	public static void main(String[] args) {
		var argParse = new ArgParser();
		argParse.parseArgs(args);
		if (argParse.startLSPServer) {
			initServer();
		} else {
			setLoggingFormat();
			try {
				initParse(argParse);
			} catch (Exception e) {
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
			p.debugPrint("Parsing " + colorify(argParse.inputPath.toString(), p.filePathColor));
		}

		for (Path incpath : argParse.includes) {
			p.subparse(incpath);
		}

		if (argParse.inputPath != null) {
			p.subparse(argParse.inputPath);
		}
		
		p.debugPrint("Total " + p.getDefines().size() + " macros defined.");
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

	public static class WMLLanguageServer implements LanguageServer, LanguageClientAware, TextDocumentService {

		private LanguageClient client;

		public void connect(LanguageClient client) {
			this.client = client;
		}

		@Override
		public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
			ServerCapabilities capabilities = new ServerCapabilities();
			capabilities.setDefinitionProvider(true);
			capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
			InitializeResult result = new InitializeResult(capabilities);

			// Send a "ready" message after startup
			if (client != null) {
				MessageParams msg = new MessageParams();
				msg.setType(MessageType.Info);
				msg.setMessage("WML LSP Server ready!");
				client.showMessage(msg);
			}
			return CompletableFuture.completedFuture(result);
		}

		@Override
		public CompletableFuture<Object> shutdown() {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
				DefinitionParams params) {
			String uri = params.getTextDocument().getUri();

			// Pretend we already know the symbol's definition location
			Location loc = new Location(uri, new Range(new Position(10, 4), // start line/char
					new Position(10, 15) // end line/char
			));

			return CompletableFuture.completedFuture(Either.forLeft(List.of(loc)));
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

		@Override
		public void didChange(DidChangeTextDocumentParams arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void didClose(DidCloseTextDocumentParams arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void didOpen(DidOpenTextDocumentParams arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void didSave(DidSaveTextDocumentParams arg0) {
			// TODO Auto-generated method stub

		}
	}
}
