package com.babai.wml.experimental;

import java.util.ListIterator;

final class ParseUtils {
	
	private ParseUtils() {};

	static Token peek(ListIterator<Token> it) {
		Token t = it.next();
		it.previous();
		return t;
	}
	
	static void skipEOL(ListIterator<Token> itor) {
		while (itor.hasNext() && ParseUtils.peek(itor).kind() == Token.Kind.EOL) {
			itor.next();
		}
	}
	
	static String consumeUntilEndDirective(String directiveName, ListIterator<Token> itor) {
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

}
