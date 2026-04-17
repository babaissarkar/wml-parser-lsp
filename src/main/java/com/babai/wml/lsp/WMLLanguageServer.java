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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
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
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.babai.wml.core.Definition;
import com.babai.wml.core.MacroArg;
import com.babai.wml.core.MacroCall;
import com.babai.wml.experimental.LogUtils;
import com.babai.wml.experimental.PathContext;
import com.babai.wml.experimental.Preprocessor;
import com.babai.wml.utils.AIGenerated;
import com.babai.wml.utils.Colors;
import com.babai.wml.utils.FS;
import com.babai.wml.utils.Table;

import static com.babai.wml.utils.ANSIFormatter.colorify;
import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

@AIGenerated
public class WMLLanguageServer implements LanguageServer, LanguageClientAware, TextDocumentService {
	public LanguageClient client;
	
	private PathContext pathContext;
	private Path inputPath;

	private Table baseDefines, defines;
	private HashSet<String> unitTypes = new HashSet<>();
	private List<MacroCall> calls = new ArrayList<>();
	private List<Path> includePaths = new ArrayList<>();
	private List<CompletionItem> macroCompletions = new ArrayList<>();
	private List<CompletionItem> keywords = new ArrayList<>();
	private List<CompletionItem> tags = new ArrayList<>();
	private Properties tagLinks = new Properties();
	
	private Preprocessor p;
// FIXME parsing disabled for now because it hangs LSP client, but it is needed for binaryPath detection...
//	private Parser parser = new Parser();

	private WMLLanguageServer(Table predefines, PathContext context, List<Path> includePaths) {
		this.pathContext = context;
		this.includePaths = includePaths;
		this.defines = predefines;

		initDirectivesList();

		initTagRefLinks();
	}
	
	public static void initServer(Table predefines, PathContext context, List<Path> includes) {
		LogUtils.setLogLevel(Level.OFF);

		var server = new WMLLanguageServer(
			predefines,
			context,
			includes);

		// Initialize a simple JSON-RPC connection over stdin/stdout
		// Most clients support this, unlike connection over TCP
		Launcher<LanguageClient> launcher = createServerLauncher(server, System.in, System.out);
		LanguageClient client = launcher.getRemoteProxy();

		server.connect(client);
		launcher.startListening();
	}
	
	private void initTagRefLinks() {
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

	//TODO maybe load from a file?
	private void initDirectivesList() {
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
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Override
	@SuppressWarnings("deprecation")
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		var capabilities = new ServerCapabilities();
		capabilities.setDefinitionProvider(true);
		capabilities.setHoverProvider(true);
		capabilities.setInlayHintProvider(true);
		capabilities.setDocumentSymbolProvider(true);
		capabilities.setCompletionProvider(new CompletionOptions(true, List.of("#", "{", "/", "[", "=")));

		var syncOptions = new TextDocumentSyncOptions();
		syncOptions.setOpenClose(true);
		syncOptions.setChange(TextDocumentSyncKind.Full);
		syncOptions.setSave(true);
		capabilities.setTextDocumentSync(syncOptions);

		var wfOptions = new WorkspaceFoldersOptions();
		wfOptions.setSupported(true);
		wfOptions.setChangeNotifications(false);
		var workspaceCaps = new WorkspaceServerCapabilities();
		workspaceCaps.setWorkspaceFolders(wfOptions);
		capabilities.setWorkspace(workspaceCaps);

		var result = new InitializeResult(capabilities);

		if (params.getWorkspaceFolders() != null && !params.getWorkspaceFolders().isEmpty()) {
			// 1. Multi-root workspaces (modern)
			inputPath = Path.of(URI.create(params.getWorkspaceFolders().get(0).getUri()));
		} else if (params.getRootUri() != null) {
			// 2. Single-root URI
			inputPath = Path.of(URI.create(params.getRootUri()));
		} else if (params.getRootPath() != null) {
			// 3. Old deprecated rootPath
			inputPath = Path.of(params.getRootPath());
		}

		// Send a "ready" message after startup
		showLSPMessage("WML LSP Server started at: Path=" + inputPath.toAbsolutePath());

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
				if (!matches.isEmpty()) {
					String targetURI = matches.get(0).getColumn("URI").getValue().toString();
					int targetLine = (int) matches.get(0).getColumn("Line").getValue();
					var range = new Range(new Position(targetLine, 0), new Position(targetLine, 1));
					var loc = new Location(targetURI, range);
					return CompletableFuture.completedFuture(Either.forLeft(List.of(loc)));
				}
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

				if (word == null || word.isEmpty()) return CompletableFuture.completedFuture(null);

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
					// if tilde is in front, it's a userdata path
					// if not, drop, IPF.
					if (!word.startsWith("~") && word.contains("~")) {
						word = word.substring(0, word.indexOf("~"));
					}
					if (word.contains(":")) {
						word = word.substring(0, word.indexOf(":"));
					}
					
					Path p = pathContext.resolve(word, Path.of(new URI(params.getTextDocument().getUri())));
					
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

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {

		String docUri = params.getTextDocument().getUri();
		var emptyRange = new Range(
				new Position(0, 0),
				new Position(0, 0));
		List<Either<SymbolInformation, DocumentSymbol>> symbolList = new ArrayList<>();

		// 1. Macro Definitions
		var matches = defines.getRows("URI", docUri);
		if (!matches.isEmpty()) {
			List<DocumentSymbol> listDef = new ArrayList<>();
			DocumentSymbol mdefRoot = new DocumentSymbol();
			mdefRoot.setName("Macro Definitions");
			mdefRoot.setKind(SymbolKind.Namespace);
			mdefRoot.setRange(emptyRange);
			mdefRoot.setSelectionRange(emptyRange);
			for (var match : matches) {
				String targetName = match.getColumn("Name").getValue().toString();
				int targetLine = (int) match.getColumn("Line").getValue();
				var range = new Range(new Position(targetLine, 0), new Position(targetLine, 1));
				DocumentSymbol sym = new DocumentSymbol();
				sym.setName(targetName);
				sym.setKind(SymbolKind.Function);
				sym.setRange(range);
				sym.setSelectionRange(range);
				listDef.add(sym);
			}
			mdefRoot.setChildren(listDef);
			symbolList.add(Either.forRight(mdefRoot));
		}

		// 2. Macro Calls
		if (!calls.isEmpty()) {
			List<DocumentSymbol> listCall = new ArrayList<>();
			DocumentSymbol mcallRoot = new DocumentSymbol();
			mcallRoot.setName("Macro Calls");
			mcallRoot.setKind(SymbolKind.Namespace);
			mcallRoot.setRange(emptyRange);
			mcallRoot.setSelectionRange(emptyRange);

			for (MacroCall call : calls) {
				if (!call.uri().equals(docUri)) {
					continue;
				}
				DocumentSymbol sym = new DocumentSymbol();
				sym.setName(call.name());
				sym.setKind(SymbolKind.Method);
				sym.setRange(new Range(
						new Position(call.startLine(), call.startChar()),
						new Position(call.endLine(), call.endChar())
						));
				sym.setSelectionRange(new Range(
						new Position(call.startLine(), call.startChar()),
						new Position(call.startLine(), call.endChar())
						));
				listCall.add(sym);
			}

			if (!listCall.isEmpty()) {
				mcallRoot.setChildren(listCall);
				symbolList.add(Either.forRight(mcallRoot));
			}
		}

		return CompletableFuture.completedFuture(symbolList);
	}

	@Override
	public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
		String uri = params.getTextDocument().getUri();
		List<InlayHint> hints = new ArrayList<>();

		for (MacroCall call : calls) {
			// skip calls outside the visible range
//			if (call.startLine() > viewRange.getEnd().getLine()) continue;
//			if (call.endLine() < viewRange.getStart().getLine()) continue;
			if (!call.uri().equals(uri)) continue;

			var rows = defines.getRows("Name", call.name());
			if (rows.isEmpty()) continue;
			var def = (Definition) rows.get(0).getColumn("Definition").getValue();
			String defTargetUri = (String) rows.get(0).getColumn("URI").getValue();
			int targetLine = (int) rows.get(0).getColumn("Line").getValue();
			var range = new Range(new Position(targetLine, 0), new Position(targetLine, 1));
			var loc = new Location(defTargetUri, range);

			List<MacroArg> args = call.args();
			for (int i = 0; i < args.size(); i++) {
				if (i >= def.getArgs().size()) break;

				var arg = args.get(i);
				var paramName = def.getArgs().get(i);

				var part = new InlayHintLabelPart(paramName + "=");
				//	            part.setTooltip(Either.forLeft(def.paramDoc(i))); // optional, null is fine
				part.setLocation(loc); // ctrl+click jumps to #define

				var hint = new InlayHint();
				hint.setPosition(new Position(arg.startLine(), arg.startChar()));
				hint.setLabel(Either.forRight(List.of(part)));
				hint.setKind(InlayHintKind.Parameter);
				hint.setPaddingRight(true);
				hints.add(hint);
			}
		}

		return CompletableFuture.completedFuture(hints);
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
		return this;
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
	public void didChange(DidChangeTextDocumentParams params) {
		inputPath = Path.of(URI.create(params.getTextDocument().getUri()));

		try {
			parseFile(inputPath);
		} catch (IOException e) {
			showLSPMessage("Parsing " + inputPath.toString() + " failed.");
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams arg0) {
		// TODO Auto-generated method stub

	}

	// FIXME still buggy. if you change a file and save, you need to relaunch editor for the diagnostic change to take effect.
	// putting parsing in didSave doesn't work, either
	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		String uri = params.getTextDocument().getUri();
//		var errorsList = p.getErrors().get(uri);
//		if (!(errorsList == null || errorsList.isEmpty())) {
//			client.publishDiagnostics(new PublishDiagnosticsParams(uri, errorsList));
//		}
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		inputPath = Path.of(URI.create(params.getTextDocument().getUri()));
		try {
			parseFile(inputPath);
		} catch (IOException e) {
			showLSPMessage("Parsing " + inputPath.toString() + " failed.");
		}
	}

	private void initParserForLSP() {
		try {
			p = new Preprocessor(pathContext, defines);

//			p.setExtractData(argParse.extractUnitTypeData);

			for (Path incpath : includePaths) {
				// FIXME recheck directory support
				p.preprocess(incpath);
			}

			baseDefines = defines.copy();
			if (inputPath != null) {
				LogUtils.debugPrint("Parsing " + colorify(inputPath.toString(), Colors.filePathColor));
				parseFile(inputPath);
			}

			showLSPMessage("Parsed, " + defines.rowCount() + " macros and " + unitTypes.size() + " unittypes defined.");
		} catch (IOException e) {
			showLSPMessage("Parsing error: " + inputPath.toString() + " not accessible!");
		}
	}

	private void parseFile(Path inputPath) throws IOException {
		p.setDefines(baseDefines.copy());
		//parser.addQuery("binary_path/path", v -> binaryPaths.add(Path.of(v)));
//		parser.parse(p.preprocess(inputPath));
		p.preprocess(inputPath);
		
//		unitTypes.addAll(p.getUnitTypes());

		defines = p.getDefines();
		calls = p.getMacroCalls();

		macroCompletions.clear();
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
	
	private void showLSPMessage(String m) {
		if (client != null) {
			MessageParams msg = new MessageParams();
			msg.setType(MessageType.Info);
			msg.setMessage(m);
			client.showMessage(msg);
		}
	}
}