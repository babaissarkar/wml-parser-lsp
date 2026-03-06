package com.babai.wml.experimental;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import com.babai.wml.core.Definition;
import com.babai.wml.utils.Table;

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
			if (t.getKind() == Token.Kind.TEXT) {
				out.print(t.getContent());
			}
			else if (t.getKind() == Token.Kind.WHITESPACE) {
				out.print(" ");
			}
			else if (t.getKind() == Token.Kind.EOL) {
				out.print("\n");
			}
			else if (t.getKind() == Token.Kind.QUOTED) {
				out.print("\"" + t.getContent() + "\"");
			}
			else if (t.getKind() == Token.Kind.ANGLE_QUOTED) {
				out.print("<<" + t.getContent() + ">>");
			}
			else if (t.getKind() == Token.Kind.COMMENT) {
				if (t.isDirective()) {
					handleDirective(t, itor);
				}
				// ignore comment otherwise
			}
		}
		
		return writer.toString();
	}
	
	private static void handleDirective(Token directiveStart, ListIterator<Token> itor) {
		var directiveHeader = processDirectiveNameAndArgs(directiveStart);

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
				String defArgName = processDirectiveNameAndArgs(t).args()[0]; // arg NAME
				
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
	
	private static Token peek(ListIterator<Token> it) {
		Token t = it.next();
		it.previous();
		return t;
	}
	
	private static void skipEOL(ListIterator<Token> itor) {
		while (itor.hasNext() && peek(itor).getKind() == Token.Kind.EOL) {
			itor.next();
		}
	}
	
	private record DirectiveHeader(String name, String[] args) {}
	
	private static DirectiveHeader processDirectiveNameAndArgs(Token token) {
		if (!token.isDirective()) {
			throw new RuntimeException("Not a directive!");
		}
		
		String[] parts = token.getContent().split("\\s+", 2);
		String name = parts[0];
		String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

		return new DirectiveHeader(name, args);
	}
	
	@SuppressWarnings("unused")
	private static void debugPrintTok(Token t) {
		var frame = StackWalker.getInstance()
				.walk(s -> s.skip(1).findFirst().get());
		System.out.println("[tok, " + frame.getMethodName() +":L" + frame.getLineNumber() + "]: " + t);
	}

	@SuppressWarnings("unused")
	private static void debugPrintJ(String str) {
		var frame = StackWalker.getInstance()
				.walk(s -> s.skip(1).findFirst().get());
		System.out.println("[" + frame.getMethodName() +":L" + frame.getLineNumber() + "]: " + str);
	}

	private static String consumeUntilEndDirective(String directiveName, ListIterator<Token> itor) {
		StringBuilder body = new StringBuilder();
		Token t = itor.next();
		while (!t.isDirectiveName(directiveName, false)) {
			if (!itor.hasNext()) {
				// terminated before define completed, error
				throw new RuntimeException("Incomplete macro definition!");
			} else {
				body.append(t.getContent());
				t = itor.next();
			}
		}
		return body.toString();
	}
}
