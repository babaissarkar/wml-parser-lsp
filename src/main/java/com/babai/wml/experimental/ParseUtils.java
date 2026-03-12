package com.babai.wml.experimental;

import java.util.ArrayList;
import java.util.List;
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
	
	static List<String> splitParenQuoted(String token) {
		List<String> parts = new ArrayList<>();
		
		StringBuilder sb = new StringBuilder();
		
		char[] chars = token.toCharArray();
		// Macro Name
		int i = 0;
		
		while (i < chars.length) {
			while (Character.isWhitespace(chars[i])) i++; // skip WS
			
			if (i >= chars.length) break;
			
			// parentheses acts as quoting against WS/linebreak
			if (chars[i] == '(') {
				i++;
				if (i >= chars.length) break;
				while (i < chars.length && chars[i] != ')') {
					sb.append(chars[i]);
					i++;
				}
				if (chars[i] == ')') i++;
			} else {
				while (i < chars.length && !Character.isWhitespace(chars[i])) {
					sb.append(chars[i]);
					i++;
				}
			}
			parts.add(sb.toString());
			sb.delete(0, sb.length());
		}
		
		return parts;
	}

}
