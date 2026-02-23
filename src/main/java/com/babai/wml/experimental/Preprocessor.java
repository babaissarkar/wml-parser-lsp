package com.babai.wml.experimental;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ListIterator;

import static com.babai.wml.experimental.Tokenizer.tokenize;

public class Preprocessor {
	
	public static String preprocess(Path inputPath) throws IOException {
		var writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		
		var itor = tokenize(inputPath).listIterator();
		
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
				if (isDirective(t.getContent())) {
					handleDirective(itor, out);
				}
			}
		}
		
		return writer.toString();
	}

	private static boolean isDirective(String str) {
		String[] directives = {
			"define",
			"arg",
			"undef",
			"ifdef",
			"ifndef",
			"ifhave",
			"ifnhave",
			"ifver",
			"ifnver",
			"error",
			"warning",
			"deprecated"
		};

		if (str == null) return false;

		for (String d : directives) {
			if (str.startsWith(d)) {
				return true;
			}
		}
		return false;
	}
	
	private static void handleDirective(ListIterator<Token> itor, PrintWriter out) {
		// TODO Auto-generated method stub
	}
}
