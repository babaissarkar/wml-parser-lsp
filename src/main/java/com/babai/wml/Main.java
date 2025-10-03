package com.babai.wml;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;

import com.babai.wml.core.Definition;
import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.AIGenerated;
import com.babai.wml.utils.ArgParser;
import com.babai.wml.utils.Table;

import java.awt.Color;
import java.io.IOException;
import java.util.logging.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;

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
			p.debugPrint("Parsing " + colorify(argParse.inputPath.toString(), p.filePathColor));
		}

		for (Path incpath : argParse.includes) {
			p.subparse(incpath);
		}

		if (argParse.inputPath != null) {
			p.subparse(argParse.inputPath);
		}

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

	@AIGenerated
	public static class WMLLanguageServer implements LanguageServer, LanguageClientAware, TextDocumentService {

		private LanguageClient client;
		private Path inputPath, dataPath, userDataPath;
		private Vector<Path> includePaths;
		private Table defines = null;

		public WMLLanguageServer(Path inputPath, Path dataPath, Path userDataPath, Vector<Path> includePaths) {
			this.inputPath = inputPath;
			this.dataPath = dataPath;
			this.userDataPath = userDataPath;
			this.includePaths = includePaths;
		}

		public void connect(LanguageClient client) {
			this.client = client;
		}

		public void showLSPMessage(String m) {
			if (client != null) {
				MessageParams msg = new MessageParams();
				msg.setType(MessageType.Info);
				msg.setMessage(m);
				client.showMessage(msg);
			}
		}

		private void initParserForLSP() {
			try {
				var p = new Preprocessor(inputPath);
				p.showParseLogs(false);
				p.showWarnLogs(false);
				p.setOutput(null);
				p.token_source.dataPath = dataPath;
				p.token_source.userDataPath = userDataPath;
				p.token_source.showLogs = false;
				if (inputPath != null) {
					for (Path incpath : includePaths) {
						p.subparse(incpath);
					}
					p.subparse(inputPath);
					defines = p.getDefines();
					showLSPMessage(("Parsed, total " + defines.rowCount() + " macros defined."));
				}
			} catch (IOException e) {
				showLSPMessage("Parsing error: " + inputPath.toString() + "not accessible!");
			}
		}

		/** Returns the word under cursor in the file pointed by URI */
		public static String getWordAtPosition(String uri, Position pos) throws IOException {
			// Convert URI to path
			String pathStr = Path.of(java.net.URI.create(uri)).toString();

			// Read all lines
			String[] lines = Files.readAllLines(Path.of(pathStr)).toArray(new String[0]);

			Predicate<Character> isValid = c -> Character.isJavaIdentifierPart(c) || c == ':' || c == '+' || c == '-';

			int lineNum = pos.getLine();
			if (lineNum < 0 || lineNum >= lines.length)
				return null;

			String line = lines[lineNum];
			int charIndex = pos.getCharacter();
			if (charIndex < 0)
				charIndex = 0;
			if (charIndex >= line.length())
				charIndex = line.length() - 1;

			// If cursor is on whitespace, move back one char
			if (!isValid.test(line.charAt(charIndex)) && charIndex > 0) {
				charIndex--;
			}

			int start = charIndex;
			int end = charIndex;

			while (start > 0 && isValid.test(line.charAt(start - 1)))
				start--;
			while (end < line.length() && isValid.test(line.charAt(end)))
				end++;

			if (start >= end)
				return null;
			return line.substring(start, end);
		}

		@Override
		public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
			var capabilities = new ServerCapabilities();
			capabilities.setDefinitionProvider(true);
			capabilities.setHoverProvider(true);
			capabilities.setCompletionProvider(new CompletionOptions(true, // resolveProvider
					List.of("#", "{") // triggerCharacterss
			));
			capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
			var result = new InitializeResult(capabilities);

			// Send a "ready" message after startup
			showLSPMessage("WML LSP Server ready!");
			initParserForLSP();

			return CompletableFuture.completedFuture(result);
		}

		@Override
		public CompletableFuture<Object> shutdown() {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
				DefinitionParams params) {
			if (defines != null) {
				try {
					String word = getWordAtPosition(params.getTextDocument().getUri(), params.getPosition());
					var matches = defines.getRows("Name", word);
					String targetURI = matches.get(0).getColumn("URI").getValue().toString();
					int targetLine = (int) matches.get(0).getColumn("Line").getValue();
					var range = new Range(new Position(targetLine, 0), new Position(targetLine, 1));
					var loc = new Location(targetURI, range);
					return CompletableFuture.completedFuture(Either.forLeft(List.of(loc)));
				} catch (IOException e) {
					showLSPMessage("Can't find word under cursor!");
				}
			}

			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Hover> hover(HoverParams params) {
			var content = new MarkupContent();
			if (defines != null) {
				try {
					String word = getWordAtPosition(params.getTextDocument().getUri(), params.getPosition());
					var matches = defines.getRows("Name", word);
					if (!matches.isEmpty()) {
						Definition def = (Definition) matches.get(0).getColumn("Definition").getValue();
						content.setKind("markdown");
						content.setValue("**" + def.name() + "**\n\n" + def.getDocs());
					} else {
						return CompletableFuture.completedFuture(null);
					}
				} catch (IOException e) {
					showLSPMessage("Can't find word under cursor!");
					return CompletableFuture.completedFuture(null);
				}
			}
			Hover hover = new Hover(content);
			return CompletableFuture.completedFuture(hover);
		}

		@Override
		public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
			String triggerChar = params.getContext() != null ? params.getContext().getTriggerCharacter() : null;
			if (params.getContext().getTriggerKind() == CompletionTriggerKind.Invoked
					|| (triggerChar != null) && triggerChar.equals("#")) {
				// Directives
				BiFunction<String, String, CompletionItem> make = (label, doc) -> {
					CompletionItem item = new CompletionItem(label);
					item.setDocumentation(doc);
					item.setKind(CompletionItemKind.Keyword);
					return item;
				};
				var items = List.of(make.apply("define", "Define a macro"),
						make.apply("enddef", "End macro definition"), make.apply("ifdef", "Do if macro defined"),
						make.apply("ifndef", "Do if macro not defined"), make.apply("ifhave", "Do if file exist"),
						make.apply("ifver", "Do if wesnoth version matches condition"),
						make.apply("endif", "End if directives block"),
						make.apply("arg", "Start optional argument in macro definition"),
						make.apply("endarg", "End optional argument in macro definition"),
						make.apply("textdomain", "Define Textdomain"));
				return CompletableFuture.completedFuture(Either.forLeft(items));
			} else {
				return CompletableFuture.completedFuture(null);
			}
		}

		@Override
		public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem item) {
			// Add more info if needed, or just return the item as-is
			return CompletableFuture.completedFuture(item);
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
