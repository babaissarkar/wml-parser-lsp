package com.babai.wml.experimental;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import com.babai.wml.core.Definition;

import static com.babai.wml.experimental.Tokenizer.tokenize;

public class Preprocessor {
	
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
		StringBuilder body = new StringBuilder();
		List<String> args;
		var defArgs = new LinkedHashMap<String, String>();
		
		String[] directiveStartTokens = directiveStart.getContent().split("\\s+", 2);
		String directiveName = directiveStartTokens[0]; // first is directive name, rest are args
		System.out.println("[full token]: " + directiveStart);
		System.out.println("[subtokens]: " + Arrays.asList(directiveStartTokens).toString());

		if (directiveName.equals("define")) {
			var directiveArgs = directiveStartTokens[1].split("\\s+");
			
			// Macro name
			String macroName = directiveArgs[0];
			
			// Args
			args = new ArrayList<String>();
			for (int i = 1; i < directiveArgs.length; i++) {
				args.add(directiveArgs[i]);
			}
			System.out.println("[define args]: " + args);
			
			// ignore EOL
			Token t = itor.next();
			t = itor.next();
			
			// TODO macro documentation comments
			
			// TODO defargs processing
			
			// Body
			while (!t.isDirectiveName("enddef")) {
				if (!itor.hasNext()) {
					// terminated before define completed, error
					throw new RuntimeException("Incomplete macro definition!");
				} else {
					body.append(t.getContent());
					t = itor.next();
				}
			}
			System.out.println("[define body]: " + body);
			
			var def = new Definition(macroName, body.toString(), args, defArgs);
			System.out.println("[Definition]: " + def);
		}
	}
}
