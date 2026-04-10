package com.babai.wml.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class ParseUtils {
	
	private ParseUtils() {};

	static Token peek(ListIterator<Token> it) {
		Token t = it.next();
		it.previous();
		return t;
	}
	
	static void skip(ListIterator<Token> itor, Token.Kind skipKind) {
		while (itor.hasNext() && ParseUtils.peek(itor).kind() == skipKind) {
			itor.next();
		}
	}
	
	static void skip(ListIterator<Token> itor, Token.Kind skipKind, Token.Kind skipKind2) {
		while (itor.hasNext() && (ParseUtils.peek(itor).kind() == skipKind || ParseUtils.peek(itor).kind() == skipKind2))
		{
			itor.next();
		}
	}
	
	static List<String> splitQuoted(String token) {
		List<String> parts = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		char[] chars = token.toCharArray();

		int i = 0;
		while (i < chars.length) {
			// Token boundary starts at the next non-whitespace character.
			while (i < chars.length && Character.isWhitespace(chars[i])) i++; // skip WS
			if (i >= chars.length) break;

			sb.setLength(0);
			// Read one token. Whitespace ends the token unless we're inside a quoted span.
			while (i < chars.length && !Character.isWhitespace(chars[i])) {
				char c = chars[i];
				if (c == '"') {
					// Keep quote characters in the resulting token to preserve existing call-site behavior.
					// Example: KEY="a b" should stay as one token (not split at inner whitespace).
					sb.append(c);
					i++;
					while (i < chars.length) {
						char q = chars[i];
						sb.append(q);
						i++;
						if (q == '"') break;
					}
				} else if (c == '(' && sb.isEmpty()) {
					// Parenthesized value at token start acts as quoting against whitespace.
					// Keep the body but strip the outer parentheses: (a b) -> a b
					i++;
					while (i < chars.length && chars[i] != ')') {
						sb.append(chars[i]);
						i++;
					}
					// Consume closing ')' if present; tolerate malformed input by leaving as-is.
					if (i < chars.length && chars[i] == ')') i++;
				} else {
					// Regular unquoted token content.
					sb.append(c);
					i++;
				}
			}

			// Commit parsed token fragment.
			parts.add(sb.toString());
		}
		
		return parts;
	}
	
	public static String csvEscape(String s) {
		if (s == null) return "";

		boolean needsQuotes =
				s.contains(",") ||
				s.contains("\"") ||
				s.contains("\n") ||
				s.contains("\r");

		if (!needsQuotes) return s;

		return "\"" + s.replace("\"", "\"\"") + "\"";
	}

}
