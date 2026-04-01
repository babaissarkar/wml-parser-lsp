package com.babai.wml.experimental;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.babai.wml.core.Definition;
import com.babai.wml.core.MacroArg;
import com.babai.wml.core.MacroCall;
import com.babai.wml.utils.Colors;
import com.babai.wml.utils.Table;

import static com.babai.wml.utils.ANSIFormatter.colorify;
import static com.babai.wml.experimental.LogUtils.*;
import static com.babai.wml.experimental.ParseUtils.*;
import static com.babai.wml.experimental.Tokenizer.tokenize;

public class Preprocessor {
	private Table defines;
	private PathContext context;
	private Path currentPath = Path.of(".");
	private List<String> currentDefineArgs = new ArrayList<>();
	private List<MacroCall> macroCalls;
	private HashMap<String, String> fileExplanations = new LinkedHashMap<>();
	private Writer writer = null;
	private boolean skipElse = true;
	
	// toplevel
	public Preprocessor(PathContext context) {
		this.context = context;
		this.defines =  Table.ofWithIndices(
			new Class<?>[]{Integer.class, String.class, String.class, Definition.class},
			new String[]{"Line", "URI", "Name", "Definition"},
			2  // index by Name column
		);
		this.macroCalls = new ArrayList<>();
	}
	
	// usually for child processes
	public Preprocessor(PathContext context, Table defines) {
		this.context = context;
		this.defines = defines;
		this.macroCalls = new ArrayList<>();
	}
	
	public Table getDefines() {
		return defines;
	}
	
	public void setDefines(Table t) {
		this.defines = t;
	}
	
	public List<MacroCall> getMacroCalls() {
		return macroCalls;
	}
	
	public HashMap<String, String> getFileExplanations() {
		return fileExplanations;
	}

	public void setOutput(Writer writer) {
		this.writer = writer;
	}
	
	// Can handle both file or folder
	// TODO _initial & _final.cfg
	public void preprocess(Path path) {
		String coloredPath = colorify(path.toString(), Colors.filePathColor);
		if (Files.isDirectory(path)) {
			debugPrint("Including directory: " + coloredPath);
			Path main = path.resolve("_main.cfg");
			if (Files.exists(main)) {
				preprocessFile(main);
			} else {
				Predicate<? super Path> filter = entry ->
					Files.isDirectory(entry)
					|| entry.getFileName().toString().endsWith(".cfg");
					
				try (var stream = Files.list(path)) {
					stream
						.filter(filter)
						.sorted()
						.forEach(this::preprocess);
				} catch (IOException e) {
					errorPrint("Cannot find " + path + ", skipping.");
				}
			}
		} else {
			preprocessFile(path);
		}
	}
	
	public void preprocessFile(Path path) {
		int prevMacroCount = this.defines.rowCount();
		String coloredPath = colorify(path.toAbsolutePath().toString(), Colors.filePathColor);
		this.currentPath = path;
		debugPrint("Preprocessing: " + coloredPath);
		try {
			preprocessFile(Files.newBufferedReader(path));
		} catch (IOException e) {
			errorPrint("Cannot find " + path + ", skipping.");
		}
		int newMacroCount = this.defines.rowCount() - prevMacroCount;
		if (newMacroCount > 0) {
			infoPrint(coloredPath + ": " + newMacroCount + " macros");
		}
	}
	
	// Can only deal with a file
	public void preprocessFile(Reader reader) throws IOException {
		var out = new PrintWriter(
			this.writer == null
				? new OutputStreamWriter(System.out)
				: this.writer);
		
		var itor = tokenize(reader).listIterator();
		
		skip(itor, Token.Kind.EOL);

		skip(itor, Token.Kind.WHITESPACE);
		
		String textdomain;
		if (peek(itor).isDirectiveName("textdomain", true)) {
			Token t = itor.next();
			var directiveHeader = DirectiveHeader.parse(t, currentPath.toString());
			textdomain = directiveHeader.args()[0];
			debugPrint("Textdomain: " + textdomain);
		}
		
		fileExplanations.put(currentPath.toUri().toString(), handleDocComment(itor));
		
		while (itor.hasNext()) {
			Token t = itor.next();
			out.print(processToken(itor, t, true));
		}
	}

	private String handleDocComment(ListIterator<Token> itor) {
		skip(itor, Token.Kind.EOL);

		skip(itor, Token.Kind.WHITESPACE);

		var docBuff = new StringBuilder();
		while (peek(itor).kind() == Token.Kind.COMMENT && !peek(itor).isDirective()) {
			Token t = itor.next();
			if (t.isDirective()) break;
			docBuff.append(t.content().trim());
			if (peek(itor).kind() == Token.Kind.EOL) {
				t = itor.next();
				docBuff.append(t.content());
			}
			skip(itor, Token.Kind.WHITESPACE);
		}
		return docBuff.toString().trim();
	}

	private String processToken(ListIterator<Token> itor, Token t, boolean expandMacro) {
		return switch (t.kind()) {
		case TEXT, WHITESPACE, EOL -> t.content();
		case QUOTED -> "\"" + t.content() + "\"";
		case ANGLE_QUOTED -> "<<" + t.content() + ">>";
		case MACRO -> {
			if (expandMacro) {
				yield expandMacro(t, currentDefineArgs, this.context);
			} else {
				yield "{" + t.content() + "}";
			}
		}
		case COMMENT -> {
			if (t.isDirective()) {
				handleDirective(t, itor, currentPath.toUri().toString());
			}
			
			yield "";
		}

		default -> throw new IllegalArgumentException("Unexpected value: " + t.kind());
		};
	}
	
	private String consumeUntilEndDirective(String directiveName, ListIterator<Token> itor) {
		StringBuilder body = new StringBuilder();
		if (!itor.hasNext()) return "";
		Token t = itor.next();
		while (!t.isDirectiveName(directiveName, false)) {
			if (!itor.hasNext()) {
				// terminated before define completed, error
				errorPrint("End directive "
						+ colorify(directiveName, Colors.directiveColor)
						+ " not found. Pos: " + position(t, currentPath.toString()));
				break;
			} else {
				// we don't want to expand any macro calls in body when consuming directive body,
				// but rather when that directive is called later on. (ie. lazy not eager behavior)
				body.append(processToken(itor, t, false));
				if (!itor.hasNext()) return body.toString();
				t = itor.next();
			}
		}
		return body.toString();
	}
	
	private void skipUntilEndDirective(String endDir, ListIterator<Token> itor) {
		skipUntilEndDirective2(endDir, endDir, itor);
	}
	
	private void skipUntilEndDirective2(String endDir1, String endDir2, ListIterator<Token> itor) {
		if (!itor.hasNext()) return;
		Token t = itor.next();
		while (!(t.isDirectiveName(endDir1, false) || t.isDirectiveName(endDir2, false))) {
			if (!itor.hasNext()) {
				// terminated before define completed, error
				errorPrint("End directives "
						+ colorify(endDir1, Colors.directiveColor)
						+ " or "
						+ colorify(endDir2, Colors.directiveColor)
						+ " not found. Pos: " + position(t, currentPath.toString()));
				return;
			} else {
				if (!itor.hasNext()) return;
				t = itor.next();
			}
		}
		return;
	}
	
	private void handleDirective(Token directiveStart, ListIterator<Token> itor, String pathUri) {
		var directiveHeader = DirectiveHeader.parse(directiveStart, currentPath.toString());
		var directiveArgs = directiveHeader.args();

		if (directiveHeader.head().equals("define")) {
			// Macro name
			String macroName = directiveArgs[0];
			List<String> macroArgs = Arrays.asList(directiveArgs).subList(1, directiveArgs.length);
			
			skip(itor, Token.Kind.EOL);
			
			// Macro deprecation messages
			boolean isDeprecated = false;
			int depreLevel = 0;
			String removalVersion = "";
			String depreMessage = "";
			while (peek(itor).isDirectiveName("deprecated", true)) {
				debugPrint("Deprecated macro: " + macroName);
				Token t = itor.next();
				isDeprecated = true;
				var deprecationHeader = DirectiveHeader.parse(t, currentPath.toString());
				var depreArgs = deprecationHeader.args();
				depreLevel = Integer.parseInt(depreArgs[0]);
				if (depreLevel == 2 || depreLevel == 3) {
					if (depreArgs.length > 1) {
						removalVersion = depreArgs[1];
					}

					// Rest of args are actually the message in this case that got split
					// join back.
					if (depreArgs.length > 2) {
						depreMessage = String.join(" ", Arrays.copyOfRange(depreArgs, 2, depreArgs.length));
					}
				} else if (depreLevel == 1 || depreLevel == 4) {
					// Rest of args are actually the message in this case that got split
					// join back.
					if (depreArgs.length > 1) {
						depreMessage = String.join(" ", Arrays.copyOfRange(depreArgs, 1, depreArgs.length));
					}
				}
				
				skip(itor, Token.Kind.EOL);
			}

			String doc = handleDocComment(itor);
			
			skip(itor, Token.Kind.EOL);
			
			// defargs processing
			var macroDefaultArgs = new LinkedHashMap<String, String>();
			while (peek(itor).isDirectiveName("arg", true)) {
				Token t = itor.next();
				String defArgName = DirectiveHeader.parse(t, currentPath.toString()).args()[0]; // arg NAME
				
				skip(itor, Token.Kind.EOL);
				
				macroDefaultArgs.put(defArgName, consumeUntilEndDirective("endarg", itor));
				
				skip(itor, Token.Kind.EOL);
			}
			
			// Body
			// Collect args in context, used in processToken macroExpansion
			currentDefineArgs.clear();
			currentDefineArgs.addAll(macroArgs);
			macroDefaultArgs.forEach((k, v) -> currentDefineArgs.add(k));
			
			var def = new Definition(macroName, consumeUntilEndDirective("enddef", itor), macroArgs, macroDefaultArgs);
			
			currentDefineArgs.clear(); // clear arg context
			
			// Extra stuff
			def.setDocs(doc);
			def.setDeprecated(isDeprecated);
			def.setDeprecationLevel(depreLevel);
			def.setDeprecationRemovalVersion(removalVersion);
			def.setDeprecationMessage(depreMessage);
			
			debugPrint("defining macro " + def.coloredName());
			defines.addRow(directiveStart.beginLine(), pathUri, macroName, def);
			
		} else if (directiveHeader.head().equals("ifdef")) {
			// TODO complain if ifdef does not exactly has one arg (macroname)
			boolean hasMacro = !defines.getRows("Name", directiveArgs[0]).isEmpty();
			if (hasMacro) {
				skipElse = true;
			} else {
				// skip upto #else or #endif
				skipUntilEndDirective2("else", "endif", itor);
				skipElse = false;
			}
		} else if (directiveHeader.head().equals("ifndef")) {
			// TODO complain if ifndef does not exactly has one arg (macroname)
			boolean hasMacro = !defines.getRows("Name", directiveArgs[0]).isEmpty();
			if (hasMacro) {
				// skip upto #else or #endif
				skipUntilEndDirective2("else", "endif", itor);
				skipElse = false;
			} else {
				skipElse = true;
			}
		} else if (directiveHeader.head().equals("else")) {
			if (skipElse) {
				skipUntilEndDirective("endif", itor);
				skipElse = false;
			}
		}
		// TODO ifdef/ifndef "else" block handling
	}
	
	private final static Pattern wspattern = Pattern.compile("\\s+");
	private boolean isPath(String str) {
		return str.contains("/") && !wspattern.matcher(str).find();
	}
	
	// TODO This might need to be recursive, like after expansion
	// if macro exists after expansion, expand again and so on until no macro calls remain.
	private String expandMacro(Token macroCall, List<String> possibleArgs, PathContext context) {
		if (isPath(macroCall.content())) {
			// TODO possibleArgs should be zero in this case, otherwise error.
			handleInclusion(macroCall, context);
			return "";
		} else {
			return expandMacroCall(macroCall, possibleArgs);
		}
	}
	
	private void handleInclusion(Token macroCall, PathContext context) {
		Path p = context.resolve(macroCall.content(), currentPath);

		if (!Files.isDirectory(p) && !p.toString().endsWith(".cfg")) return;
		
		String coloredPathString = colorify(p.toString(), Colors.filePathColor);
		
		debugPrint("Trying to include: " + coloredPathString);

		if (!Files.exists(p)) {
			warningPrint(coloredPathString + " does not exist");
			return;
		}
		
		debugPrint("Including: " + coloredPathString);
		
		preprocess(p);
	}
	
	private String expandMacroCall(Token macroCall, List<String> possibleArgs) {
		final String content = macroCall.content();
		final String fallback = "{" + content + "}";
		var parts = ParseUtils.splitQuoted(content);
		String macroName = parts.get(0);
		List<MacroArg> args = new ArrayList<>();
		HashMap<String, String> defArgs = new LinkedHashMap<>();
		
		// ---------------------------------------
		
		List<Table.Row> rows = defines.getRows("Name", macroName);
		Definition def = null;
		if (!rows.isEmpty()) {
			def = (Definition) rows.get(0).getColumn("Definition").getValue();
		}
		
		if (def != null) {
			
			// Process macro call args
			int lastPos = 0;
			for (int i = 1; i < parts.size(); i++) {
				String str = parts.get(i);
				
				// Mandatory positional args
				if (i-1 < def.getArgCount()) {
					//FIXME multiline arguments, also this should be done in splitQuoted
					lastPos = content.indexOf(str, lastPos + 1);
					int argStart = macroCall.beginColumn() + lastPos;
					int argEnd = argStart + str.length();
					int argLine = macroCall.beginLine() - 1; //TODO args may start on a different line. why -1?
					args.add(new MacroArg(str, argLine, argStart, argEnd));
				} else {
					// Optional keyword args
					if (str.contains("=")) {
						String[] keyVal = str.split("=", 2);
						if (def.getDefArgs().containsKey(keyVal[0])) {
							defArgs.put(keyVal[0], keyVal[1]);
						} else {
							//TODO error: invalid defarg passed
						}
					} else {
						//TODO error: more defargs passed than needed
					}
				}
			}
			
			macroCalls.add(new MacroCall(
					macroName,
					macroCall.beginLine(),
					macroCall.endLine(),
					macroCall.beginColumn(),
					macroCall.endColumn(),
					args,
					currentPath.toUri().toString()));
			
			String argsString = Definition.argsAsString2(args, defArgs);
			debugPrint("expanding macro " + def.coloredName()
				+ (!argsString.isEmpty() ? " with " + colorify(argsString, Colors.macroArgColor) : ""));
			
			try {
				return def.expand2(args, defArgs);
			} catch(IllegalArgumentException e) {
				errorPrint("Error expanding macro " + def.coloredName()
					+ " in "
					+ colorify(currentPath.toString(), Colors.filePathColor)
					+ ": " + e.getMessage());
				return fallback;
			}
		} else if (possibleArgs.contains(macroName)) {
			// FIXME: do nothing for now. may need checks later.
			return fallback;
		} else {
			warningPrint(position(macroCall, currentPath.toString()) + " undefined macro " + colorify(macroName, Color.RED));
			return fallback;
		}
	}
	
	private record DirectiveHeader(String head, String[] args) {
		// processDirectiveNameAndArgs
		public static DirectiveHeader parse(Token token, String pathStr) {
			if (!token.isDirective()) {
				errorPrint("Unknown directive found at " + position(token, pathStr));
			}
			
			String[] parts = token.content().split("\\s+", 2);
			String name = parts[0];
			String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
	
			return new DirectiveHeader(name, args);
		}
	}
}
