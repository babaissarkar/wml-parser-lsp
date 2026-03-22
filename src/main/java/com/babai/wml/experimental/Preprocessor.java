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
	private List<MacroCall> macroCalls;
	private Writer writer = null;
	
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

	public void setOutput(Writer writer) {
		this.writer = writer;
	}
	
	// Can handle both file or folder
	// TODO _initial & _final.cfg
	public void preprocess(Path path) throws IOException {
		String coloredPath = colorify(path.toString(), Colors.filePathColor);
		if (Files.isDirectory(path)) {
			debugPrint("Including directory: " + coloredPath);
			Path main = path.resolve("_main.cfg");
			if (Files.exists(main)) {
				path = main;
				this.currentPath = path;
				debugPrint("Preprocessing: " + coloredPath);
				preprocess(Files.newBufferedReader(path));
			} else {
				try (var stream = Files.list(path)) {
					var files = stream.sorted().toList();
					for (Path f : files) {
						if (Files.isDirectory(f) || !f.toString().endsWith(".cfg"))
							return;
						preprocess(f);
					}
				} catch (IOException e) {
					errorPrint("Cannot find " + path + ", skipping.");
				}
				return;
			}
		} else {
			this.currentPath = path;
			debugPrint("Preprocessing: " + coloredPath);
			preprocess(Files.newBufferedReader(path));
		}
	}
	
	// Can only deal with a file
	public void preprocess(Reader reader) throws IOException {
		var out = new PrintWriter(
			this.writer == null
				? new OutputStreamWriter(System.out)
				: this.writer);
		
		var itor = tokenize(reader).listIterator();
		
		while (itor.hasNext()) {
			Token t = itor.next();

			switch (t.kind()) {
			case TEXT -> out.print(t.content());

			case WHITESPACE -> out.print(" ");

			case EOL -> out.print("\n");

			case QUOTED -> out.print("\"" + t.content() + "\"");

			case ANGLE_QUOTED -> out.print("<<" + t.content() + ">>");

			case COMMENT -> {
				if (t.isDirective()) {
					String path = currentPath.toAbsolutePath().toString();
					handleDirective(t, itor, path);
				}
				// otherwise ignore
			}
			
			case MACRO -> out.print(expandMacro(t, List.of(), this.context));
			default -> throw new IllegalArgumentException("Unexpected value: " + t.kind());
			}
		}
	}
	
	private String consumeUntilEndDirective(String directiveName, ListIterator<Token> itor) {
		StringBuilder body = new StringBuilder();
		Token t = itor.next();
		while (!t.isDirectiveName(directiveName, false)) {
			if (!itor.hasNext()) {
				// terminated before define completed, error
				errorPrint("Incomplete macro definition for "
						+ colorify(directiveName, Colors.directiveColor) + " at " + position(t));
				break;
			} else {
				body.append(t.content());
				t = itor.next();
			}
		}
		return body.toString();
	}
	
	private void handleDirective(Token directiveStart, ListIterator<Token> itor, String pathname) {
		var directiveHeader = DirectiveHeader.parse(directiveStart);

		if (directiveHeader.name().equals("define")) {
			var directiveArgs = directiveHeader.args();
			
			// Macro name
			String macroName = directiveArgs[0];
			List<String> macroArgs = Arrays.asList(directiveArgs).subList(1, directiveArgs.length);

			skipEOL(itor);

			// TODO macro documentation comments
			
			// defargs processing
			var macroDefaultArgs = new LinkedHashMap<String, String>();
			while (peek(itor).isDirectiveName("arg", true)) {
				Token t = itor.next();
				String defArgName = DirectiveHeader.parse(t).args()[0]; // arg NAME
				
				skipEOL(itor);
				
				macroDefaultArgs.put(defArgName, consumeUntilEndDirective("endarg", itor));
				
				skipEOL(itor);
			}
			
			// Body
			var def = new Definition(macroName, consumeUntilEndDirective("enddef", itor), macroArgs, macroDefaultArgs);
			
			// dummy, needs more info
			//defines.addRow(name.beginLine-1, currentPath.toUri().toString(), name.image, def);
			debugPrint("defining macro " + def.coloredName());
			defines.addRow(directiveStart.beginLine(), pathname, macroName, def);
		}
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
			return handleInclusion(macroCall, context);
		} else {
			return expandMacroCall(macroCall, possibleArgs);
		}
	}
	
	private String handleInclusion(Token macroCall, PathContext context) {
		Path p = context.resolve(macroCall.content(), currentPath);

		if (!Files.isDirectory(p) && !p.toString().endsWith(".cfg")) return "";
		
		String coloredPathString = colorify(p.toString(), Colors.filePathColor);
		
		debugPrint("Trying to include: " + coloredPathString);

		if (!Files.exists(p)) {
			warningPrint(coloredPathString + " does not exist");
			return "";
		}
		
		debugPrint("Including: " + coloredPathString);
		try {
			preprocess(p);
		} catch(IOException ioe) {
			errorPrint("Cannot read file/folder " + coloredPathString);
		}
		
		return "";
	}
	
	private String expandMacroCall(Token macroCall, List<String> possibleArgs) {
		final String content = macroCall.content();
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
			debugPrint("expanding macro "
				+ def.coloredName()
				+ (!argsString.isEmpty() ? " with " + colorify(argsString, Colors.macroArgColor) : ""));
			try {
				return def.expand2(args, defArgs);
			} catch(IllegalArgumentException e) {
				errorPrint("Error expanding macro " + def.coloredName() + ": " + e.getMessage());
				return macroCall.toString();
			}
		} else if (possibleArgs.contains(macroName)) {
			// FIXME: do nothing for now. may need checks later why this is happening.
			return macroCall.toString();
		} else {
			warningPrint(position(macroCall) + " undefined macro " + colorify(macroName, Color.RED));
			return macroCall.toString();
		}
	}
	
	private record DirectiveHeader(String name, String[] args) {
		// processDirectiveNameAndArgs
		public static DirectiveHeader parse(Token token) {
			if (!token.isDirective()) {
				errorPrint("Unknown directive found at " + position(token));
			}
			
			String[] parts = token.content().split("\\s+", 2);
			String name = parts[0];
			String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
	
			return new DirectiveHeader(name, args);
		}
	}
}
