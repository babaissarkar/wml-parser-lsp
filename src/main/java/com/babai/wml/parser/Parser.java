package com.babai.wml.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import com.babai.wml.query.WMLQuery;
import com.babai.wml.tokenizer.Token;

import static com.babai.wml.utils.Colors.*;
import static com.babai.wml.utils.LogUtils.*;
import static com.babai.wml.cli.ANSIFormatter.colorify;
import static com.babai.wml.tokenizer.Tokenizer.tokenize;
import static com.babai.wml.tokenizer.Token.Kind.*;
import static com.babai.wml.parser.ParseUtils.peek;

public class Parser {
	private List<String> tagStack = new ArrayList<>();
	private HashMap<String, List<Consumer<String>>> queryLambdas = new LinkedHashMap<>();

	public void addQuery(String query, Consumer<String> queryLambda) {
		queryLambdas.computeIfAbsent(query, k -> new ArrayList<>()).add(queryLambda);
	}

	public void parse(String text) throws IOException {
		var line = new StringBuilder();
		var itor = tokenize(text).listIterator();
		while (itor.hasNext()) {
			Token t = itor.next();
			parseToken(itor, t, line);
		}
	}

	private void parseToken(ListIterator<Token> itor, Token t, StringBuilder line) {
		line.setLength(0);
		
		switch (t.kind()) {
		case TEXT -> {
			// Read the entire line
			line.append(t.content());
			while (peek(itor).isKind(TEXT, WHITESPACE, QUOTED, ANGLE_QUOTED)) {
				t = itor.next();
				line.append(t.content());
			}
			
			// Extract key and value once, outside the query loop
			int i = 0;
			for (; i < line.length(); i++) {
				if (line.charAt(i) == '=') break;
			}

			if (i != line.length()) {
				int eqPos = i;

				// Remove trailing whitespace from key
				int keyEnd = eqPos;
				while (keyEnd > 0 && Character.isWhitespace(line.charAt(keyEnd - 1))) keyEnd--;
				String key = line.substring(0, keyEnd);

				// Remove leading whitespace from value
				int valStart = eqPos + 1;
				while (valStart < line.length() && Character.isWhitespace(line.charAt(valStart))) valStart++;
				String val = line.substring(valStart);
				
				// Check against queries
				for (var query : queryLambdas.entrySet()) {
					if (WMLQuery.match(tagStack, query.getKey(), key)) {
						for (var lambda : query.getValue()) {
							lambda.accept(val);
						}
					}
				}
			}
		}
		case TAG -> {
			String tagName = t.content();
			if (tagName.startsWith("+")) {
				// appending tag, like [+units]
				tagName = tagName.substring(1, tagName.length());
				if (isTag(tagName)) {
					tagStack.add(tagName);
				}
			} else if (tagName.startsWith("/")) {
				// end tag
				tagName = tagName.substring(1, tagName.length());
				if (tagStack.isEmpty()) {
					errorPrint(() -> "End tag without matching start tag.");
				} else if (tagStack.getLast().equals(tagName)) {
					tagStack.removeLast();
				} else {
					final String tmpTagName = tagName;
					errorPrint(() -> "Wrong end tag " + colorify(tmpTagName, RED)
					+ " found for tag "
					+ colorify("[" + tagStack.getLast() + "]", tagColor));
				}
				// needs better handling
			} else if (isTag(tagName)) {
				tagStack.add(tagName);
			}
		}
		case WHITESPACE, EOL, COMMENT, QUOTED, ANGLE_QUOTED -> {} //ignore
		case MACRO -> {
			final String tmpContent = t.content();
			warningPrint(() -> "Parser: Unexpanded macro {" + tmpContent + "}, skipping");
		}
		default -> {
			final String tmpContent = t.content();
			warningPrint(() -> "Parser: Unexpected token " + tmpContent + ", skipping");
		}
		}
	}
	
	private boolean isTag(CharSequence tagName) {
		if (tagName.length() == 0) return false;
		int i = 0;
		char c = tagName.charAt(i);
		if (c == '+' || c == '/') {
			i++;
			if (i >= tagName.length()) return false;  // "+" or "/" alone
		}
		if (!Character.isLetter(tagName.charAt(i))) return false;
		i++;  // skip the validated first letter
		for (; i < tagName.length(); i++) {
			c = tagName.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '_')) return false;
		}
		return true;
	}
}
