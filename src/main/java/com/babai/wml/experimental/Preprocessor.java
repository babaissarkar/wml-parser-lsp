package com.babai.wml.experimental;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import com.babai.wml.core.Definition;
import com.babai.wml.utils.Table;

import static com.babai.wml.experimental.LogUtils.*;
import static com.babai.wml.experimental.ParseUtils.*;
import static com.babai.wml.experimental.Tokenizer.tokenize;

public class Preprocessor {
	private static Table defines = Table.ofWithIndices(
			new Class<?>[]{Integer.class, String.class, String.class, Definition.class},
			new String[]{"Line", "URI", "Name", "Definition"},
			2  // index by Name column
	);
	
	public static Table getDefines() {
		return defines;
	}

	public static void setDefines(Table defines) {
		Preprocessor.defines = defines;
	}

	public static String preprocess(Path inputPath) throws IOException {
		return preprocess(Files.newBufferedReader(inputPath));
	}
	
	public static String preprocess(Reader reader) throws IOException {
		var writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		
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
					handleDirective(t, itor);
				}
				// otherwise ignore
			}
			
			case MACRO -> throw new UnsupportedOperationException("Unimplemented case: " + t.kind());
			default -> throw new IllegalArgumentException("Unexpected value: " + t.kind());
			}
		}
		
		return writer.toString();
	}
	
	private static void handleDirective(Token directiveStart, ListIterator<Token> itor) {
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
			defines.addRow(0, ".", macroName, def);
		}
	}
	
	@SuppressWarnings("unused")
	private static String expandMacroCall(
			Token macroCall,
			List<String> possibleArgs)
	{
		// TODO extract these from macroCall token
		String macroName = "";
		List<String> args = List.of();
		HashMap<String, String> defArgs = new LinkedHashMap<>();
		// ---------------------------------------
		
		List<Table.Row> rows = defines.getRows("Name", macroName);
		Definition def = null;
		if (!rows.isEmpty()) {
			def = (Definition) rows.get(0).getColumn("Definition").getValue();
		}
		
		if (def != null) {
			String argsString = Definition.argsAsString(args, defArgs);
			debugPrint("expanding macro " + def.name()
				+ (!argsString.isEmpty() ? " with " + argsString : ""));
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
			warningPrint(position(macroCall) + " undefined macro " + macroName);
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
