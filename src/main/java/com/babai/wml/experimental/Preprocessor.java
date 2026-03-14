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
import com.babai.wml.core.Definition;
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
	private Writer writer = null;
	
	// toplevel
	public Preprocessor(PathContext context) {
		this.context = context;
		this.defines =  Table.ofWithIndices(
			new Class<?>[]{Integer.class, String.class, String.class, Definition.class},
			new String[]{"Line", "URI", "Name", "Definition"},
			2  // index by Name column
		);
	}
	
	// usually for child processes
	public Preprocessor(PathContext context, Table defines) {
		this.context = context;
		this.defines = defines;
	}
	
	public Table getDefines() {
		return defines;
	}
	
	public void setOutput(Writer writer) {
		this.writer  = writer;
	}
	
	// Can handle both file or folder
	public void preprocess(Path path) throws IOException {
		String coloredPath = colorify(path.toString(), Colors.filePathColor);
		if (Files.isDirectory(path)) {
			debugPrint("Including directory: " + coloredPath);
			Path main = path.resolve("_main.cfg");
			if (Files.exists(main)) {
				path = main;
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
				throw new RuntimeException("Incomplete macro definition!");
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
	
	private boolean isPath(String str) {
		return str.contains("/");
	}
	
	// TODO This might need to be recursive, like after expansion
	// if macro exists, expand again and so on until no macro calls remain.
	private String expandMacro(Token macroCall, List<String> possibleArgs, PathContext context) {
		if (isPath(macroCall.toString())) {
			// TODO possibleArgs should be zero in this case, otherwise error.
			return handleFileInclusion(macroCall, context);
		} else {
			return expandMacroCall(macroCall, possibleArgs);
		}
	}
	
	private String handleFileInclusion(Token macroCall, PathContext context) {
		Path p = context.resolve(macroCall.content(), currentPath);

		debugPrint("Trying to include: " + colorify(p.toString(), Colors.filePathColor));

		if (!Files.isDirectory(p) && !p.toString().endsWith(".cfg")) return "";

		if (Files.exists(p)) {
			debugPrint("Including: " + colorify(p.toString(), Colors.filePathColor));
			try {
				preprocess(p);
			} catch(IOException ioe) {
				errorPrint("Cannot find file/folder " + macroCall.content());
			}
		} else {
			warningPrint(macroCall.content() + " not found");
		}
		
		return "";
	}
	
	private String expandMacroCall(Token macroCall, List<String> possibleArgs) {
		var parts = ParseUtils.splitParenQuoted(macroCall.content());
		String macroName = parts.get(0);
		List<String> args = new ArrayList<>();
		HashMap<String, String> defArgs = new LinkedHashMap<>();
		for (int i = 1; i < parts.size(); i++) {
			String str = parts.get(i);
			if (str.contains("=")) {
				String[] keyVal = str.split("=", 2);
				defArgs.put(keyVal[0], keyVal[1]);
			} else {
				args.add(str);
			}
		}
		
		// ---------------------------------------
		
		List<Table.Row> rows = defines.getRows("Name", macroName);
		Definition def = null;
		if (!rows.isEmpty()) {
			def = (Definition) rows.get(0).getColumn("Definition").getValue();
		}
		
		if (def != null) {
			String argsString = Definition.argsAsString(args, defArgs);
			debugPrint("expanding macro "
				+ def.coloredName()
				+ (!argsString.isEmpty() ? " with " + colorify(argsString, Colors.macroArgColor) : ""));
			try {
				return def.expand(args, defArgs);
			} catch(IllegalArgumentException e) {
				errorPrint(e.getMessage());
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
				throw new RuntimeException("Not a directive!");
			}
			
			String[] parts = token.content().split("\\s+", 2);
			String name = parts[0];
			String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
	
			return new DirectiveHeader(name, args);
		}
	}
}
