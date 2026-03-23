package com.babai.wml.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
	
	static List<String> splitQuoted(String token) {
		List<String> parts = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		char[] chars = token.toCharArray();

		int i = 0;
		while (i < chars.length) {
			while (i < chars.length && Character.isWhitespace(chars[i])) i++; // skip WS
			
			if (i >= chars.length) break;
			
			// parentheses acts as quoting against WS/linebreak
			var delims = Map.of('(', ')', '\"', '\"');
			Character startDelim = chars[i];
			Character endDelim = delims.get(chars[i]); 
			if (delims.containsKey(startDelim)) {
				// we want the quotes but not parens
				if (startDelim == '\"') sb.append(startDelim);
				i++;
				if (i >= chars.length) break;
				while (i < chars.length && chars[i] != endDelim) {
					sb.append(chars[i]);
					i++;
				}
				if (i >= chars.length) break;
				if (chars[i] == endDelim) {
					if (endDelim == '\"') sb.append(endDelim);
					i++;
				}
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
