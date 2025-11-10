package com.babai.wml.lsp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.babai.wml.core.Definition;
import com.babai.wml.preprocessor.Preprocessor;
import com.babai.wml.utils.AIGenerated;
import com.babai.wml.utils.FS;
import com.babai.wml.utils.Table;

@AIGenerated
public class WMLLanguageServer implements LanguageServer, LanguageClientAware, TextDocumentService {
	public LanguageClient client;
	private Path inputPath;
	private Path dataPath;
	private Path userDataPath;
	private Table defines;
	private Vector<Path> includePaths = new Vector<>();
	private Vector<Path> binaryPaths = new Vector<>();
	private HashSet<String> unitTypes = new HashSet<>();
	private List<CompletionItem> macroCompletions = new ArrayList<>();
	private List<CompletionItem> keywords = new ArrayList<>();
	private List<CompletionItem> tags = new ArrayList<>();
	private Properties tagLinks = new Properties();
	private Preprocessor p;

	public WMLLanguageServer(Table predefines, Path inputPath, Path dataPath, Path userDataPath, Vector<Path> includePaths) {
		this.inputPath = inputPath;
		this.dataPath = dataPath;
		this.userDataPath = userDataPath;
		this.includePaths = includePaths;
		this.defines = predefines;

		// Directives, this List never changes so created here once
		BiFunction<String, String, CompletionItem> make = (label, doc) -> {
			CompletionItem item = new CompletionItem(label);
			item.setDocumentation(doc);
			item.setInsertText(item.getLabel() + " ");
			item.setKind(CompletionItemKind.Keyword);
			keywords.add(item);
			return item;
		};

		make.apply("define", "Define a macro");
		make.apply("enddef", "End macro definition");
		make.apply("ifdef", "Do if macro defined");
		make.apply("ifndef", "Do if macro not defined");
		make.apply("ifhave", "Do if file exists");
		make.apply("ifver", "Do if wesnoth version matches condition");
		make.apply("endif", "End if directives block");
		make.apply("arg", "Start optional argument in macro definition");
		make.apply("endarg", "End optional argument in macro definition");
		make.apply("textdomain", "Define Textdomain");
		
		// Reference links for tags
		try {
			tagLinks.load(getClass().getResourceAsStream("/taglinks.properties"));
			for (var tag : tagLinks.entrySet()) {
				CompletionItem item = new CompletionItem(tag.getKey().toString());
				item.setInsertText(item.getLabel() + "]$0[/" + item.getLabel());
				item.setKind(CompletionItemKind.Snippet);
				item.setInsertTextFormat(InsertTextFormat.Snippet);
				tags.add(item);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Returns the word under cursor in the file pointed by URI */
	private static String getWordAtPosition(String uri, Position pos) throws IOException {
		// Convert URI to path
		String pathStr = Path.of(java.net.URI.create(uri)).toString();

		// Read all lines
		String[] lines = Files.readAllLines(Path.of(pathStr)).toArray(new String[0]);

		List<Character> validChars = List.of(':', '+', '-', '/', '~', '.');
		Predicate<Character> isValid = c -> Character.isJavaIdentifierPart(c) || validChars.contains(c);

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
		
		// recognize tags
		if (start > 0 && (line.charAt(start - 1) == '[') && end < line.length() && (line.charAt(end) == ']')) {
			start -= 1;
			end += 1;
		}
		
		return line.substring(start, end);
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

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		var capabilities = new ServerCapabilities();
		capabilities.setDefinitionProvider(true);
		capabilities.setHoverProvider(true);
		capabilities.setCompletionProvider(new CompletionOptions(true, List.of("#", "{", "/", "[", "=")));
		TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
		syncOptions.setOpenClose(true);
		syncOptions.setChange(TextDocumentSyncKind.Full);
		syncOptions.setSave(true);
		capabilities.setTextDocumentSync(syncOptions);
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
				if (word.contains("[")) {
					// Tags
					String searchWord = word.replaceAll("/", "");
					searchWord = searchWord.substring(1, searchWord.length() - 1);
					String link = tagLinks.getProperty(searchWord);
					content.setKind("markdown");
					content.setValue("Tag: **" + word + "**" +  "\n\n"
						+ (link != null ? "Reference: " + link : "")
					);
				} else if (word.contains("/") || word.contains("~")) {
					// Wesnoth Paths
					// if tilde is in front, it's a userdata path, ignore
					// if not, drop, IPF.
					if (!word.startsWith("~") && word.contains("~")) {
						word = word.substring(0, word.indexOf("~"));
					}
					if (word.contains(":")) {
						word = word.substring(0, word.indexOf(":"));
					}
					Path p = FS.resolve(word, binaryPaths, Path.of(
						new URI(params.getTextDocument().getUri())),
						dataPath, userDataPath);
					if (Files.exists(p)) {
						content.setKind("markdown");
						if (FS.getAssetType(word).equals("images")) {
							content.setValue("![Image](" + p.toUri().toString() + ")");
						} else {
							content.setValue("Go To: [" + p.getFileName() + "](" + p.toUri().toString() + ")");
						}
					} else {
						content.setKind("plaintext");
						content.setValue("Non-existant path: " + p);
					}
				} else {
					// Macro calls
					var matches = defines.getRows("Name", word);
					if (!matches.isEmpty()) {
						Definition def = (Definition) matches.get(0).getColumn("Definition").getValue();
						content.setKind("markdown");
						content.setValue("**" + def.name() + "**\n\n" + def.getDocs());
					} else {
						return CompletableFuture.completedFuture(null);
					}
				}
			} catch (IOException e) {
				showLSPMessage("Can't find word under cursor!");
				return CompletableFuture.completedFuture(null);
			} catch (URISyntaxException e) {
				// shouldn't really happen...
				showLSPMessage("Invalid uri for current document!");
				return CompletableFuture.completedFuture(null);
			}
		}
		Hover hover = new Hover(content);
		return CompletableFuture.completedFuture(hover);
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		String triggerChar = params.getContext() != null ? params.getContext().getTriggerCharacter() : null;
		List<CompletionItem> items = new ArrayList<>();

		// Directives
		if (params.getContext().getTriggerKind() == CompletionTriggerKind.Invoked
				|| (triggerChar != null) && triggerChar.equals("#")) {
			items.addAll(keywords);
			return CompletableFuture.completedFuture(Either.forLeft(items));
		}

		if (params.getContext().getTriggerKind() == CompletionTriggerKind.Invoked
				|| (triggerChar != null) && triggerChar.equals("{")) {
			items.addAll(macroCompletions);
			return CompletableFuture.completedFuture(Either.forLeft(items));
		}
		
		if (params.getContext().getTriggerKind() == CompletionTriggerKind.Invoked
				|| (triggerChar != null) && triggerChar.equals("[")) {
			items.addAll(tags);
			return CompletableFuture.completedFuture(Either.forLeft(items));
		}
		
		if (params.getContext().getTriggerKind() == CompletionTriggerKind.Invoked
				|| (triggerChar != null) && triggerChar.equals("="))
		{
			for (var type : unitTypes) {
				CompletionItem item = new CompletionItem(type);
				item.setInsertText(item.getLabel());
				item.setKind(CompletionItemKind.Constant);
				items.add(item);
			}
			return CompletableFuture.completedFuture(Either.forLeft(items));
		}

		if (triggerChar != null && triggerChar.equals("/")) {
			try {
//				String word = getWordAtPosition(params.getTextDocument().getUri(), params.getPosition());
//				showLSPMessage(word);
				items.addAll(listAll(inputPath, "", params.getPosition()));
				return CompletableFuture.completedFuture(Either.forLeft(items));
			} catch (IOException e) {
				e.printStackTrace();
				return CompletableFuture.completedFuture(null);
			}
		}

		return CompletableFuture.completedFuture(null);
	}
	
	private static CompletionItem toCompletionItem(String relPath, Position cursor) {
		CompletionItem item = new CompletionItem(relPath);
		item.setKind(CompletionItemKind.File); // You could detect folder vs file if you want
//		item.setInsertText(relPath);           // Simple snippet = just the path
//		 Replace the trigger "/" with the path text
		Range replaceRange = new Range(
				new Position(cursor.getLine(), cursor.getCharacter() - 1),
				cursor
				);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, relPath)));
		return item;
	}
	
	private static List<CompletionItem> listAll(Path baseDir, String prefix, Position cursor) throws IOException {
		try (Stream<Path> stream = Files.walk(baseDir)) {
			return stream
					.filter(p -> !p.equals(baseDir))
					.map(baseDir::relativize)
					.map(p -> p.toString().replace('\\', '/'))
					.filter(rel -> rel.startsWith(prefix))
					.filter(rel -> !rel.startsWith("."))
					.map(rel -> toCompletionItem(rel, cursor))
					.collect(Collectors.toList());
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
	public void didSave(DidSaveTextDocumentParams saveParams) {
		String uri = saveParams.getTextDocument().getUri();
		inputPath = Path.of(URI.create(uri));
		
		try {
			macroCompletions.clear();
			p.setDefinesMap(defines);
			p.subparse(inputPath);
			binaryPaths = p.getBinaryPaths();
			unitTypes = p.getUnitTypes();
			defines = p.getDefines();
			for (var r : defines.getRows()) {
				CompletionItem item = new CompletionItem();
				Definition def = (Definition) r.getColumn("Definition").getValue();
				item.setLabel(def.name());
				item.setKind(CompletionItemKind.Method);
				String docs = def.getDocs();
				item.setDocumentation(def.name() + (!docs.isEmpty() ? ("\n" + docs) : ""));
				item.setInsertText(item.getLabel());
				item.setInsertTextFormat(InsertTextFormat.Snippet); //
				macroCompletions.add(item);
			}
			showLSPMessage("Parsed, total " + defines.rowCount() + " macros defined.");
		} catch (IOException e) {
			showLSPMessage("Parsing " + inputPath.toString() + " failed.");
		}
	}

	private void initParserForLSP() {
		try {
			p = new Preprocessor(inputPath);
			p.showParseLogs(false);
			p.showWarnLogs(false);
			p.setOutput(null);
			p.setDefinesMap(defines);
			p.token_source.dataPath = dataPath;
			p.token_source.userDataPath = userDataPath;
			p.token_source.showLogs = false;
			if (inputPath != null) {
				for (Path incpath : includePaths) {
					p.subparse(incpath);
					unitTypes.addAll(p.getUnitTypes());
				}
				p.subparse(inputPath);
				unitTypes.addAll(p.getUnitTypes());
				
				binaryPaths = p.getBinaryPaths();
				defines = p.getDefines();
				for (var r : defines.getRows()) {
					CompletionItem item = new CompletionItem();
					Definition def = (Definition) r.getColumn("Definition").getValue();
					item.setLabel(def.name());
					item.setKind(CompletionItemKind.Method);
					String docs = def.getDocs();
					item.setDocumentation(def.name() + (docs != null && !docs.isEmpty() ? ("\n" + docs) : ""));
					item.setInsertText(item.getLabel());
					item.setInsertTextFormat(InsertTextFormat.Snippet); //
					macroCompletions.add(item);
				}

				showLSPMessage(("Parsed, total " + defines.rowCount() + " macros defined."));
			}
		} catch (IOException e) {
			showLSPMessage("Parsing error: " + inputPath.toString() + "not accessible!");
		}
	}
}