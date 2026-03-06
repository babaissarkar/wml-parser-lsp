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
			
			Token t = itor.next(); // skip EOL
			t = itor.next();
			
			// TODO macro documentation comments
			
			// defargs processing
			while (t.isDirectiveName("arg", true)) {
				System.out.println("[defarg tok]: " + t);
				String[] defArgToks = t.getContent().split("\\s+", 2); // arg NAME
				itor.next(); // skip EOL
				defArgs.put(defArgToks[1], consumeUntilEndDirective("endarg", itor));
				System.out.println("[defarg key]: " + defArgToks[1]);
				System.out.println("[defarg val]: " + defArgs.get(defArgToks[1]).toString());
				itor.next(); // skip EOL
				t = itor.next();
				System.out.println("[defarg end tok]: " + t);
			}
			
			itor.previous();
			
			// Body
			String body = consumeUntilEndDirective("enddef", itor);
			System.out.println("[define body]: " + body);
			
			var def = new Definition(macroName, body, args, defArgs);
			System.out.println("[Definition]: " + def);
		}
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
				System.out.println("[consumeUntil]: " + body);
				t = itor.next();
			}
		}
		return body.toString();
	}
}
